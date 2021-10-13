package io.seqera.tower.agent;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.rxjava2.http.client.websockets.RxWebSocketClient;
import io.micronaut.scheduling.TaskScheduler;
import io.seqera.tower.agent.exchange.CommandResponse;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public class Agent {

    private ApplicationContext ctx;
    private Map<String,String> env;
    private String hostName;
    private String accessToken;
    private String towerUrl;

    private AgentClientSocket agentClient;

    Agent() {
        env =  System.getenv();
        ctx = ApplicationContext.run();
    }

    public void run() throws Exception {
        hostName = env.get("TOWER_AGENT_HOSTNAME");
        if (hostName == null) throw new IllegalStateException("TOWER_AGENT_HOSTNAME not set");

        accessToken = env.get("TOWER_ACCESS_TOKEN");
        if (accessToken == null) throw new IllegalStateException("TOWER_ACCESS_TOKEN not set");

        towerUrl = env.get("TOWER_API_ENDPOINT");
        if (towerUrl == null) throw new IllegalStateException("TOWER_API_ENDPOINT not set");

        final URI uri = new URI(towerUrl + "/agent/" + hostName + "/connect");
        final MutableHttpRequest<?> req = HttpRequest.GET(uri).bearerAuth(accessToken);
        final RxWebSocketClient webSocketClient = ctx.getBean(RxWebSocketClient.class);
        agentClient = webSocketClient.connect(AgentClientSocket.class, req).blockingFirst();

        System.out.println("Connected");
        sendPeriodicHeartbeat();
    }

    /**
     * Send a heartbeat every minute in order to avoid closing the connection due to idleness.
     */
    private void sendPeriodicHeartbeat() {
        TaskScheduler scheduler = ctx.getBean(TaskScheduler.class);

        scheduler.scheduleAtFixedRate(null, Duration.ofMinutes(1), () -> {
            System.out.println("Sending heartbeat");
            agentClient.send(new CommandResponse(true));
        });
    }

    public static void main(String[] args) throws Exception {
        new Agent().run();
    }
}
