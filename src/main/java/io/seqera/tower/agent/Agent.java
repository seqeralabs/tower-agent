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
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.rxjava2.http.client.RxHttpClient;
import io.micronaut.rxjava2.http.client.websockets.RxWebSocketClient;
import io.micronaut.scheduling.TaskScheduler;
import io.micronaut.websocket.exceptions.WebSocketClientException;
import io.seqera.tower.agent.exchange.CommandRequest;
import io.seqera.tower.agent.exchange.CommandResponse;
import io.seqera.tower.agent.exchange.HeartbeatMessage;
import io.seqera.tower.agent.exchange.InfoMessage;
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
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public static final int MAX_WEBSOCKET_PAYLOAD_SIZE = 5242880;
    private static final Logger logger = LoggerFactory.getLogger(Agent.class);

    @Value("${tower.agent.heartbeat:`45s`}")
    Duration heartbeatDelay;

    @Parameters(index = "0", paramLabel = "AGENT_CONNECTION_ID", description = "Agent connection ID to identify this agent.", arity = "1")
    String agentKey;

    @Option(names = {"-t", "--access-token"}, description = "Tower personal access token. If not provided TOWER_ACCESS_TOKEN variable will be used.", defaultValue = "${TOWER_ACCESS_TOKEN}", required = true)
    String token;

    @Option(names = {"-u", "--url"}, description = "Tower server API endpoint URL. If not provided TOWER_API_ENDPOINT variable will be used [default: https://api.tower.nf].", defaultValue = "${TOWER_API_ENDPOINT:-https://api.tower.nf}", required = true)
    String url;

    @Option(names = {"-w", "--work-dir"}, description = "Default path where the pipeline scratch data is stored. It can be changed when launching a pipeline from Tower [default: ~/work].")
    Path workDir;

    private String validatedWorkDir;
    private String validatedUserName;
    private final ApplicationContext ctx;
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
            validateParameters();
            checkTower();
            connectTower();
            sendPeriodicHeartbeat();
        } catch (Exception e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
    }

    private void connectTowerDelay() {
        TaskScheduler scheduler = ctx.getBean(TaskScheduler.class);
        Duration delay = Duration.ofSeconds(2);
        scheduler.schedule(delay, this::connectTower);
    }

    /**
     * Connect the agent to Tower using websockets
     */
    private void connectTower() {
        logger.info("Connecting to Tower");
        try {
            final URI uri = new URI(url + "/agent/" + agentKey + "/connect");
            if (!uri.getScheme().equals("https")) {
                logger.error("You are trying to connect to an insecure server: {}", url);
                System.exit(1);
            }

            final MutableHttpRequest<?> req = HttpRequest.GET(uri).bearerAuth(token);
            final RxWebSocketClient webSocketClient = ctx.getBean(RxWebSocketClient.class);
            agentClient = webSocketClient.connect(AgentClientSocket.class, req)
                    .timeout(5, TimeUnit.SECONDS)
                    .blockingFirst();
            agentClient.setConnectCallback(this::connectTowerDelay);
            agentClient.setCommandRequestCallback(this::execCommand);
            sendInfoMessage();
        } catch (URISyntaxException e) {
            logger.error("Invalid URI: {}/agent/{}/connect - {}", url, agentKey, e.getMessage());
            System.exit(1);
        } catch (WebSocketClientException e) {
            logger.error("Connection error - {}", e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            if (e.getCause() instanceof TimeoutException) {
                logger.error("Connection timeout [trying to reconnect in {} seconds]", heartbeatDelay);
            } else {
                logger.error("Unknown problem");
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    /**
     * Executes a command request and sends the response back to Tower
     *
     * @param message Command request message
     */
    private void execCommand(CommandRequest message) {
        CommandResponse response;

        try {
            Process process = new ProcessBuilder()
                    .command("sh", "-c", message.getCommand())
                    .redirectErrorStream(true)
                    .start();

            // read the stdout
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }

            // truncate response to fit the maximum websocket size
            if (builder.length() > (MAX_WEBSOCKET_PAYLOAD_SIZE - 100)) {
                logger.warn("Response to [{}] '{}' was truncated", message.getId(), message.getCommand());
                builder.setLength(MAX_WEBSOCKET_PAYLOAD_SIZE - 100);
            }

            String result = builder.toString();
            process.waitFor(10, TimeUnit.SECONDS);
            int exitStatus = process.exitValue();
            process.destroy();
            response = new CommandResponse(message.getId(), result.getBytes(), exitStatus);
        } catch (Throwable e) {
            response = new CommandResponse(message.getId(), e.getMessage().getBytes(), 1);
        }
        // send result
        logger.info("Sending response {}'", response.getId());
        logger.trace("RESPONSE: {}", response);
        agentClient.sendAsync(response);
    }

    /**
     * Send a heartbeat every minute in order to avoid closing the connection due to idleness.
     */
    private void sendPeriodicHeartbeat() {
        TaskScheduler scheduler = ctx.getBean(TaskScheduler.class);
        scheduler.scheduleWithFixedDelay(heartbeatDelay, heartbeatDelay, () -> {
            if (agentClient.isOpen()) {
                logger.info("Sending heartbeat");
                agentClient.send(new HeartbeatMessage());
            } else {
                logger.info("Trying to reconnect");
                connectTower();
            }
        });
    }

    private void sendInfoMessage() throws IOException {
        agentClient.send(new InfoMessage(
                validatedUserName,
                validatedWorkDir,
                getVersion()
        ));
    }

    /**
     * Validate and set default values of all Agent configurable parameters
     *
     * @throws IOException
     */
    private void validateParameters() throws IOException {
        // Fetch username
        validatedUserName = System.getenv().getOrDefault("USER", System.getProperty("user.name"));
        if (validatedUserName == null || validatedUserName.isEmpty() || validatedUserName.isBlank() || validatedUserName.equals("?")) {
            logger.error("Impossible to detect current Unix username. Try setting USER environment variable.");
            System.exit(1);
        }

        // Set default workDir
        if (workDir == null) {
            logger.debug("No work directory provided. Using default ~/work.");
            String defaultPath = System.getProperty("user.home") + "/work";
            try {
                workDir = Paths.get(defaultPath);
            } catch (InvalidPathException e) {
                logger.error("Impossible to define a default work directory. Please provide one using '--work-dir'.");
                System.exit(1);
            }
        }

        // Validate workDir exists
        if (!Files.exists(workDir)) {
            logger.error("The work directory '{}' do not exists. Create it or provide a different one using '--work-dir'.", workDir);
            System.exit(1);
        }
        validatedWorkDir = workDir.toAbsolutePath().normalize().toString();

        String agentVersion = getVersion();
        String requiredApiVersion = getVersionApi();

        logger.info("TOWER AGENT v{}", agentVersion);
        logger.info("Compatible with TOWER API v{}", requiredApiVersion);
        logger.info("Connecting as user '{}' with default work directory '{}'", validatedUserName, validatedWorkDir);
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
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            if (url.contains("/api")) {
                logger.error("Tower API endpoint '{}' it is not available", url);
            } else {
                logger.error("Tower API endpoint '{}' it is not available (did you mean '{}/api'?)", url, url);
            }
            System.exit(1);
        }

        try {
            final URI uri = new URI(url + "/user");
            final MutableHttpRequest<?> req = HttpRequest.GET(uri).bearerAuth(token);
            httpClient.retrieve(req).blockingFirst();
        } catch (Exception e) {
            logger.error("Invalid TOWER_ACCESS_TOKEN, check that the given token has access at '{}'.", url);
            System.exit(1);
        }
    }

    /**
     * Minimum API required version
     *
     * @return Required API version
     * @throws IOException On reading properties file
     */
    private String getVersionApi() throws IOException {
        return getProperties().get("versionApi").toString();
    }

    /**
     * Current Agent version
     *
     * @return Agent version
     * @throws IOException On reading properties file
     */
    private String getVersion() throws IOException {
        return getProperties().get("version").toString();
    }

    /**
     * Load 'build-info.properties'
     *
     * @return Build properties
     * @throws IOException On reading properties file
     */
    private Properties getProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(this.getClass().getResourceAsStream("/META-INF/build-info.properties"));
        return properties;
    }
}
