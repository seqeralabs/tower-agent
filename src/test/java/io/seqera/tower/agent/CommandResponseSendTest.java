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
import io.seqera.tower.agent.exchange.AgentMessage;
import io.seqera.tower.agent.exchange.CommandResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Sends a command response larger than Netty's 1 MiB pooling threshold through the
 * real agent WebSocket client. Under {@code nativeTest} this covers the whole send
 * path (JSON encoding, frame writing, buffer release) inside a native image.
 */
@MicronautTest
class CommandResponseSendTest {

    static final BlockingQueue<AgentMessage> received = new LinkedBlockingQueue<>();

    @ServerWebSocket("/test/agent")
    static class ReceivingServer {

        @OnMessage(maxPayloadLength = 2 * Agent.MAX_WEBSOCKET_PAYLOAD_SIZE)
        void onMessage(AgentMessage message) {
            received.add(message);
        }
    }

    @Inject
    EmbeddedServer server;

    @Inject
    RxWebSocketClient webSocketClient;

    @Test
    void sendsResponseAbovePoolingThreshold() throws Exception {
        byte[] result = new byte[2 * 1024 * 1024];
        Arrays.fill(result, (byte) 'x');

        AgentClientSocket socket = webSocketClient
                .connect(AgentClientSocket.class, HttpRequest.GET(server.getURI().resolve("/test/agent")))
                .timeout(10, TimeUnit.SECONDS)
                .blockingFirst();
        try {
            socket.sendAsync(new CommandResponse("large", result, 0)).get(30, TimeUnit.SECONDS);

            AgentMessage message = received.poll(30, TimeUnit.SECONDS);
            CommandResponse response = Assertions.assertInstanceOf(CommandResponse.class, message);
            Assertions.assertEquals("large", response.getId());
            Assertions.assertArrayEquals(result, response.getResult());
        } finally {
            socket.close();
        }
    }

}
