package io.seqera.tower.agent.exchange;

import io.micronaut.core.annotation.ReflectiveAccess;

/**
 * @author Jordi Deu-Pons <jordi@seqera.io>
 */
@ReflectiveAccess
public class HeartbeatMessage extends AgentMessage {

    public HeartbeatMessage() {
    }

    @Override
    public String toString() {
        return "HeartbeatMessage[]";
    }

}
