/*
 * Copyright (c) 2021, Seqera Labs.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as
 * defined by the Mozilla Public License, v. 2.0.
 */

package io.seqera.tower.agent;

import io.micronaut.configuration.picocli.PicocliRunner;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.rxjava2.http.client.RxHttpClient;
import io.micronaut.rxjava2.http.client.websockets.RxWebSocketClient;
import io.micronaut.scheduling.TaskScheduler;
import io.micronaut.websocket.exceptions.WebSocketClientException;
import io.seqera.tower.agent.exchange.CommandRequest;
import io.seqera.tower.agent.exchange.CommandResponse;
import io.seqera.tower.agent.exchange.HeartbeatMessage;
import io.seqera.tower.agent.model.ServiceInfoResponse;
import io.seqera.tower.agent.utils.VersionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Command(
        name = "tw-agent",
        description = "Nextflow Tower Agent",
        headerHeading = "%n",
        versionProvider = VersionProvider.class,
        mixinStandardHelpOptions = true,
        sortOptions = false,
        abbreviateSynopsis = true,
        descriptionHeading = "%n",
        commandListHeading = "%nCommands:%n",
        requiredOptionMarker = '*',
        usageHelpWidth = 160,
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n"
)
public class Agent implements Runnable {
    public static final int HEARTBEAT_MINUTES_INTERVAL = 1;
    private static Logger logger = LoggerFactory.getLogger(Agent.class);

    @Parameters(index = "0", paramLabel = "AGENT_CONNECTION_ID", description = "Agent connection ID to identify this agent", arity = "1")
    String agentKey;

    @Option(names = {"-t", "--access-token"}, description = "Tower personal access token (TOWER_ACCESS_TOKEN)", defaultValue = "${TOWER_ACCESS_TOKEN}", required = true)
    String token;

    @Option(names = {"-u", "--url"}, description = "Tower server API endpoint URL. Defaults to tower.nf (TOWER_API_ENDPOINT)", defaultValue = "${TOWER_API_ENDPOINT:-https://api.tower.nf}", required = true)
    String url;

    @Option(names = {"--no-secure"}, description = "Explicitly allow to connect to a non-SSL secured Tower server (this is not recommended)")
    boolean noSecure;

    private ApplicationContext ctx;
    private AgentClientSocket agentClient;

    Agent() {
        ctx = ApplicationContext.run();
    }

    public static void main(String[] args) throws Exception {
        PicocliRunner.run(Agent.class, args);
    }

    @Override
    public void run() {
        try {
            checkTower();
            connectTower();
            sendPeriodicHeartbeat();
        } catch (Exception e) {
            logger.error(e.getMessage());
            System.exit(-1);
        }
    }

    /**
     * Connect the agent to Tower using websockets
     */
    private void connectTower() {
        try {
            final URI uri = new URI(url + "/agent/" + agentKey + "/connect");
            if (!uri.getScheme().equals("https") && !noSecure) {
                logger.error("You are trying to connect to an insecure server: {}", url);
                logger.error("if you want to force the connection use '--no-secure' option. NOT RECOMMENDED!");
                System.exit(-1);
            }
            if (!uri.getScheme().equals("https") && noSecure) {
                logger.warn("You are connecting using an INSECURE CONNECTION: {}", url);
            }

            final MutableHttpRequest<?> req = HttpRequest.GET(uri).bearerAuth(token);
            final RxWebSocketClient webSocketClient = ctx.getBean(RxWebSocketClient.class);
            agentClient = webSocketClient.connect(AgentClientSocket.class, req)
                    .timeout(5, TimeUnit.SECONDS)
                    .blockingFirst();
            agentClient.setConnectCallback(this::connectTower);
            agentClient.setCommandRequestCallback(this::execCommand);
        } catch (URISyntaxException e) {
            logger.error("Invalid URI: {}/agent/{}/connect - {}", url, agentKey, e.getMessage());
        } catch (WebSocketClientException e) {
            logger.error("Connection error - {}", e.getMessage());
        } catch (Exception e) {
            if (e.getCause() instanceof TimeoutException) {
                logger.error("Connection timeout [trying to reconnect in {} minutes]", HEARTBEAT_MINUTES_INTERVAL);
            } else {
                logger.error("Unknown problem");
                e.printStackTrace();
            }
        }
    }

    /**
     * Executes a command request and sends the response back to Tower
     *
     * @param message Command request message
     */
    private void execCommand(CommandRequest message) {
        try {
            logger.info("Execute: {}", message.getCommand());
            Process process = new ProcessBuilder().command("sh", "-c", message.getCommand()).start();
            int exitStatus = process.waitFor();
            // read the stdout
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
            String result = builder.toString();

            // send result
            CommandResponse response = new CommandResponse(message.getId(), result.getBytes(), exitStatus);
            logger.info("Sending response --> {}", response);
            agentClient.send(response);
            logger.info("Response sent");
        } catch (Exception e) {
            // send result
            CommandResponse response = new CommandResponse(message.getId(), e.getMessage().getBytes(), -1);
            agentClient.send(response);
        }
    }

    /**
     * Send a heartbeat every minute in order to avoid closing the connection due to idleness.
     */
    private void sendPeriodicHeartbeat() {
        TaskScheduler scheduler = ctx.getBean(TaskScheduler.class);
        Duration interval = Duration.ofMinutes(HEARTBEAT_MINUTES_INTERVAL);
        scheduler.scheduleAtFixedRate(interval, interval, () -> {
            if (agentClient.isOpen()) {
                logger.info("Sending heartbeat");
                agentClient.send(new HeartbeatMessage());
            } else {
                logger.info("Trying to reconnect");
                connectTower();
            }
        });
    }

    /**
     * Do some health checks to the Tower API endpoint to verify that it is available and
     * compatible with this Agent.
     */
    private void checkTower() {
        final RxHttpClient httpClient = ctx.getBean(RxHttpClient.class);
        try {
            final URI uri = new URI(url + "/service-info");
            final MutableHttpRequest<?> req = HttpRequest.GET(uri).bearerAuth(token);

            ServiceInfoResponse infoResponse = httpClient.retrieve(req, ServiceInfoResponse.class).blockingFirst();
            if (infoResponse.getServiceInfo() != null && infoResponse.getServiceInfo().getApiVersion() != null) {
                final ModuleDescriptor.Version systemApiVersion = ModuleDescriptor.Version.parse(infoResponse.getServiceInfo().getApiVersion());
                final ModuleDescriptor.Version requiredApiVersion = ModuleDescriptor.Version.parse(getVersionApi());

                if (systemApiVersion.compareTo(requiredApiVersion) < 0) {
                    logger.error("Tower at '{}' is running API version {} and the agent needs a minimum of {}", url, systemApiVersion, requiredApiVersion);
                    System.exit(-1);
                }
            }
        } catch (Exception e) {
            if (url.contains("/api")) {
                logger.error("Tower API endpoint '{}' it is not available", url);
            } else {
                logger.error("Tower API endpoint '{}' it is not available (did you mean '{}/api'?)", url, url);
            }
            System.exit(-1);
        }

        try {
            final URI uri = new URI(url + "/user");
            final MutableHttpRequest<?> req = HttpRequest.GET(uri).bearerAuth(token);
            httpClient.retrieve(req).blockingFirst();
        } catch (Exception e) {
            logger.error("Invalid TOWER_ACCESS_TOKEN, check that the given token has access at '{}'.", url);
            System.exit(-1);
        }
    }

    /**
     * Minimum API required version
     *
     * @return Required API version
     * @throws IOException On reading properties file
     */
    private String getVersionApi() throws IOException {
        Properties properties = new Properties();
        properties.load(this.getClass().getResourceAsStream("/META-INF/build-info.properties"));
        return properties.get("versionApi").toString();
    }
}
