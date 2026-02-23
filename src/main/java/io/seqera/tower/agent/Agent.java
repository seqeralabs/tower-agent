/*
 * Copyright 2021-2026, Seqera.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import io.seqera.tower.agent.exceptions.RecoverableException;
import io.seqera.tower.agent.exceptions.UnrecoverableException;
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
import java.net.UnknownHostException;
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
    public static final int MAX_WEBSOCKET_PAYLOAD_SIZE = 10485760;
    private static final Logger logger = LoggerFactory.getLogger(Agent.class);

    @Value("${tower.agent.heartbeat:`45s`}")
    Duration heartbeatDelay;

    @Parameters(index = "0", paramLabel = "AGENT_CONNECTION_ID", description = "Agent connection ID to identify this agent.", arity = "1")
    String agentKey;

    @Option(names = {"-t", "--access-token"}, description = "Tower personal access token. If not provided TOWER_ACCESS_TOKEN variable will be used.", defaultValue = "${TOWER_ACCESS_TOKEN}", required = true)
    String token;

    @Option(names = {"-u", "--url"}, description = "Tower server API endpoint URL. If not provided TOWER_API_ENDPOINT variable will be used [default: https://api.cloud.seqera.io].", defaultValue = "${TOWER_API_ENDPOINT:-https://api.cloud.seqera.io}", required = true)
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
            sendPeriodicHeartbeat();
            infiniteLoop();
        } catch (UnrecoverableException e) {
            logger.error(e.getMessage());
            System.exit(1);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    private void infiniteLoop() throws InterruptedException, IOException {
        while (true) {
            try {
                if (agentClient == null || !agentClient.isOpen()) {
                    checkTower();
                    connectTower();
                }
            } catch (RecoverableException e) {
                logger.error(e.getMessage());
            }

            Thread.sleep(2000);
        }
    }

    /**
     * Connect the agent to Tower using websockets
     */
    private void connectTower() {
        logger.info("Connecting to Tower");
        try {
            final URI uri = new URI(url + "/agent/" + agentKey + "/connect");
            if (!uri.getScheme().equals("https")) {
                throw new UnrecoverableException(String.format("You are trying to connect to an insecure server: %s", url));
            }

            final MutableHttpRequest<?> req = HttpRequest.GET(uri).bearerAuth(token);
            final RxWebSocketClient webSocketClient = ctx.getBean(RxWebSocketClient.class);
            agentClient = webSocketClient.connect(AgentClientSocket.class, req)
                    .timeout(5, TimeUnit.SECONDS)
                    .blockingFirst();
            agentClient.setCommandRequestCallback(this::execCommand);
            sendInfoMessage();
        } catch (URISyntaxException e) {
            throw new UnrecoverableException(String.format("Invalid URI: %s/agent/%s/connect - %s", url, agentKey, e.getMessage()));
        } catch (WebSocketClientException e) {
            throw new RecoverableException(String.format("Connection error - %s", e.getMessage()));
        } catch (UnknownHostException e) {
            throw new RecoverableException("Unknown host exception - Check that it's a valid DNS domain.");
        } catch (Exception e) {
            if (e.getCause() instanceof TimeoutException) {
                throw new RecoverableException(String.format("Connection timeout  -- %s", e.getCause().getMessage()));
            }

            throw new RecoverableException(String.format("Unknown problem - %s", e.getMessage()), e);
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
            logger.trace("REQUEST: {}", message.getCommand());
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
            if (agentClient != null && agentClient.isOpen()) {
                logger.info("Sending heartbeat");
                logger.trace("websocket session '{}'", agentClient.getId());
                agentClient.send(new HeartbeatMessage());
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
            throw new UnrecoverableException("Impossible to detect current Unix username. Try setting USER environment variable.");
        }

        // Set default workDir
        if (workDir == null) {
            logger.debug("No work directory provided. Using default ~/work.");
            String defaultPath = System.getProperty("user.home") + "/work";
            try {
                workDir = Paths.get(defaultPath);
            } catch (InvalidPathException e) {
                throw new UnrecoverableException("Impossible to define a default work directory. Please provide one using '--work-dir'.");
            }
        }

        // Validate workDir exists
        if (!Files.exists(workDir)) {
            throw new UnrecoverableException(String.format("The work directory '%s' do not exists. Create it or provide a different one using '--work-dir'.", workDir));
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
    private void checkTower() throws IOException {
        final RxHttpClient httpClient = ctx.getBean(RxHttpClient.class);
        ServiceInfoResponse infoResponse = null;
        try {
            final URI uri = new URI(url + "/service-info");
            final MutableHttpRequest<?> req = HttpRequest.GET(uri).bearerAuth(token);
            infoResponse = httpClient.retrieve(req, ServiceInfoResponse.class).blockingFirst();
        } catch (Exception e) {
            if (url.contains("/api")) {
                throw new RecoverableException(String.format("Tower API endpoint '%s' it is not available", url));
            }
            throw new RecoverableException(String.format("Tower API endpoint '%s' it is not available (did you mean '%s/api'?)", url, url));
        }

        if (infoResponse != null && infoResponse.getServiceInfo() != null && infoResponse.getServiceInfo().getApiVersion() != null) {
            final ModuleDescriptor.Version systemApiVersion = ModuleDescriptor.Version.parse(infoResponse.getServiceInfo().getApiVersion());
            final ModuleDescriptor.Version requiredApiVersion = ModuleDescriptor.Version.parse(getVersionApi());

            if (systemApiVersion.compareTo(requiredApiVersion) < 0) {
                throw new UnrecoverableException(String.format("Tower at '%s' is running API version %s and the agent needs a minimum of %s", url, systemApiVersion, requiredApiVersion));
            }
        }

        try {
            final URI uri = new URI(url + "/user");
            final MutableHttpRequest<?> req = HttpRequest.GET(uri).bearerAuth(token);
            httpClient.retrieve(req).blockingFirst();
        } catch (Exception e) {
            throw new UnrecoverableException(String.format("Invalid TOWER_ACCESS_TOKEN, check that the given token has access at '%s'.", url));
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
