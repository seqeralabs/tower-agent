package io.seqera.tower.agent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;

import io.micronaut.http.HttpRequest;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.ClientWebSocket;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.seqera.tower.agent.exchange.CommandRequest;
import io.seqera.tower.agent.exchange.CommandResponse;

/**
 * @author Jordi Deu-Pons <jordi@seqera.io>
 */
@ClientWebSocket
abstract class AgentClientSocket implements AutoCloseable {

    private WebSocketSession session;
    private HttpRequest request;

    private Instant openingTime;

    @OnOpen
    void onOpen(WebSocketSession session, HttpRequest request) {
        this.session = session;
        this.request = request;
        this.openingTime = Instant.now();
        System.out.println("Client opened connection");
    }

    @OnMessage
    void onMessage(CommandRequest message) {
        if (message.isHeartbeat()) {
            System.out.println("Received heartbeat");
            return;
        }

        System.out.println("Execute command: " + message.getCommand());
        try {

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
            CommandResponse response = new CommandResponse(message.getId(), result, exitStatus);
            System.out.println("Sending response --> " + response);
            session.sendSync(response);
            System.out.println("Response sent");
        }
        catch ( Throwable e ){
            // send result
            CommandResponse response = new CommandResponse(message.getId(), e.getMessage(), -1);
            session.sendSync(response);
        }
    }

    @OnClose
    void onClose() {
        System.out.println("Closed after " + Duration.between(openingTime, Instant.now()));
    }

    abstract void send(CommandResponse response);
}
