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
import java.util.function.Consumer;

/**
 * @author Jordi Deu-Pons <jordi@seqera.io>
 */
@ClientWebSocket
abstract class AgentClientSocket implements AutoCloseable {
    private static Logger logger = LoggerFactory.getLogger(AgentClientSocket.class);

    private WebSocketSession session;
    private Instant openingTime;

    // Callback to reconnect the agent
    private Runnable connectCallback;

    // Callback to manage a command request
    private Consumer<CommandRequest> commandRequestCallback;

    @OnOpen
    void onOpen(WebSocketSession session) {
        this.session = session;
        this.openingTime = Instant.now();
        logger.info("Connection to Tower established");
        logger.debug("Websocket session URL: {}", session.getRequestURI());
    }

    @OnMessage
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
            if (connectCallback != null) {
                connectCallback.run();
            }
            return;
        }

        if (openingTime != null) {
            Duration d = Duration.between(openingTime, Instant.now());
            String duration = String.format("%sd %sh %sm %ss", d.toDaysPart(), d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart());
            logger.info("Closed after {}. [trying to reconnect in {} minutes]", duration, Agent.HEARTBEAT_MINUTES_INTERVAL);
        }
    }

    abstract void send(AgentMessage message);

    public boolean isOpen() {
        return session.isOpen();
    }

    public void setConnectCallback(Runnable connectCallback) {
        this.connectCallback = connectCallback;
    }

    public void setCommandRequestCallback(Consumer<CommandRequest> callback) {
        this.commandRequestCallback = callback;
    }


}
