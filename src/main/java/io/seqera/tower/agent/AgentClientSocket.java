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

import io.micronaut.scheduling.TaskScheduler;
import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.ClientWebSocket;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.seqera.tower.agent.exchange.AgentMessage;
import io.seqera.tower.agent.exchange.CommandRequest;
import io.seqera.tower.agent.exchange.HeartbeatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * @author Jordi Deu-Pons <jordi@seqera.io>
 */
@ClientWebSocket
abstract class AgentClientSocket implements AutoCloseable {
    private static Logger logger = LoggerFactory.getLogger(AgentClientSocket.class);

    private WebSocketSession session;
    private Instant openingTime;

    // Callback to manage a command request
    private Consumer<CommandRequest> commandRequestCallback;

    @OnOpen
    void onOpen(WebSocketSession session) {
        this.session = session;
        this.openingTime = Instant.now();
        logger.info("Connection to Tower established");
        logger.debug("Websocket session URL: {}", session.getRequestURI());
    }

    @OnMessage(maxPayloadLength=Agent.MAX_WEBSOCKET_PAYLOAD_SIZE)
    void onMessage(AgentMessage message) {
        if (message instanceof HeartbeatMessage) {
            logger.info("Received heartbeat");
            return;
        }

        if (message instanceof CommandRequest && commandRequestCallback != null) {
            commandRequestCallback.accept((CommandRequest) message);
            return;
        }

        throw new RuntimeException(String.format("Unknown agent message '%s'", message.getClass().getSimpleName()));
    }

    @OnClose
    void onClose(CloseReason reason) {

        // Duplicated agent
        if (reason.getCode() == 4000) {
            logger.error("There is an active agent for this user and connection ID. Please close it before starting a new one.");
            System.exit(-1);
        }

        if (reason.getCode() == 4001) {
            logger.info("Closing to reauthenticate the session");
        } else {
            logger.info("Closed for unknown reason after");
            if (openingTime != null) {
                Duration d = Duration.between(openingTime, Instant.now());
                String duration = String.format("%sd %sh %sm %ss", d.toDaysPart(), d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart());
                logger.info("Session duration {}", duration);
            }
        }
    }

    abstract void send(AgentMessage message);

    public abstract Future<String> sendAsync(AgentMessage message);

    public boolean isOpen() {
        return session.isOpen();
    }

    public void setCommandRequestCallback(Consumer<CommandRequest> callback) {
        this.commandRequestCallback = callback;
    }

    public String getId() {
        return session.getId();
    }


}
