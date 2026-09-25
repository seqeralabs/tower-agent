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

import io.micronaut.http.HttpRequest;
import io.micronaut.rxjava2.http.client.websockets.RxWebSocketClient;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.ServerWebSocket;
import io.micronaut.websocket.exceptions.WebSocketClientException;
import io.seqera.tower.agent.exchange.AgentMessage;
import io.seqera.tower.agent.exchange.CommandResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * The agent logs failed responses from the future returned by
 * {@link Agent#sendResponse}, so every way a send can fail must complete that
 * future exceptionally rather than being thrown or dropped.
 */
@MicronautTest
class AgentClientSocketTest {

    @ServerWebSocket("/test/socket")
    static class DiscardingServer {

        @OnMessage
        void onMessage(String message) {
        }
    }

    static class UnencodableMessage extends AgentMessage {

        public String getValue() {
            throw new IllegalStateException("cannot encode");
        }
    }

    @Inject
    EmbeddedServer server;

    @Inject
    RxWebSocketClient webSocketClient;

    @Test
    void sendAsyncReportsEncodingFailureThroughFuture() throws Exception {
        AgentClientSocket socket = connect();
        try {
            CompletableFuture<String> result = socket.sendAsync(new UnencodableMessage());

            ExecutionException failure = Assertions.assertThrows(ExecutionException.class, () -> result.get(10, TimeUnit.SECONDS));
            Assertions.assertTrue(hasCause(failure, IllegalStateException.class, "cannot encode"), () -> "unexpected failure: " + failure);
        } finally {
            socket.close();
        }
    }

    @Test
    void sendResponseReportsClosedSessionThroughFuture() throws Exception {
        AgentClientSocket socket = connect();
        socket.close();
        for (int i = 0; i < 100 && socket.isOpen(); i++) {
            Thread.sleep(50);
        }
        Assertions.assertFalse(socket.isOpen());
        CommandResponse response = new CommandResponse("closed", new byte[0], 0);

        // sendAsync itself throws on a closed session, which is why sendResponse catches it
        Assertions.assertThrows(WebSocketClientException.class, () -> socket.sendAsync(response));

        CompletableFuture<String> result = Agent.sendResponse(socket, response);
        ExecutionException failure = Assertions.assertThrows(ExecutionException.class, () -> result.get(10, TimeUnit.SECONDS));
        Assertions.assertInstanceOf(WebSocketClientException.class, failure.getCause());
    }

    private AgentClientSocket connect() {
        return webSocketClient
                .connect(AgentClientSocket.class, HttpRequest.GET(server.getURI().resolve("/test/socket")))
                .timeout(10, TimeUnit.SECONDS)
                .blockingFirst();
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type, String message) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause) && message.equals(cause.getMessage())) {
                return true;
            }
        }
        return false;
    }

}
