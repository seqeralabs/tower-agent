package io.seqera.tower.agent;

import io.micronaut.http.HttpRequest;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.ClientWebSocket;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.seqera.tower.agent.exchange.AgentMessage;
import io.seqera.tower.agent.exchange.CommandRequest;
import io.seqera.tower.agent.exchange.CommandResponse;
import io.seqera.tower.agent.exchange.HeartbeatMessage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;

/**
 * @author Jordi Deu-Pons <jordi@seqera.io>
 */
@ClientWebSocket
abstract class AgentClientSocket implements AutoCloseable {

    private WebSocketSession session;

    private Instant openingTime;

    @OnOpen
    void onOpen(WebSocketSession session, HttpRequest request) {
        this.session = session;
        this.openingTime = Instant.now();
        System.out.println("Client opened connection");
    }

    @OnMessage
    void onMessage(AgentMessage message) {
        if (message instanceof HeartbeatMessage) {
            System.out.println("Received heartbeat");
            return;
        }

        if (message instanceof CommandRequest) {
            execCommand((CommandRequest) message);
            return;
        }

        throw new RuntimeException(String.format("Unknown agent message '%s'", message.getClass().getSimpleName()));
    }

    @OnClose
    void onClose() {
        if (openingTime != null) {
            System.out.println("Closed after " + Duration.between(openingTime, Instant.now()));
        }
    }

    private void execCommand(CommandRequest message) {
        try {
            System.out.println("Execute command: " + message.getCommand());
            Process process = new ProcessBuilder().command("sh","-c", message.getCommand()).start();
            int exitStatus = process.waitFor();
            // read the stdout
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line=null;
            while ( (line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
            String result = builder.toString();

            // send result
            CommandResponse response = new CommandResponse(message.getId(), result.getBytes(), exitStatus);
            System.out.println("Sending response --> " + response);
            session.sendSync(response);
            System.out.println("Response sent");
        }
        catch ( Throwable e ){
            // send result
            CommandResponse response = new CommandResponse(message.getId(), e.getMessage().getBytes(), -1);
            session.sendSync(response);
        }
    }

    abstract void send(AgentMessage message);
}
