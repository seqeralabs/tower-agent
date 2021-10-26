package io.seqera.tower.agent.exchange;

import io.micronaut.core.annotation.ReflectiveAccess;

/**
 * @author Jordi Deu-Pons <jordi@seqera.io>
 */
@ReflectiveAccess
public class CommandRequest extends AgentMessage {
    private String id;
    private String command;

    public CommandRequest() {
    }

    public CommandRequest(String id, String command) {
        this.id = id;
        this.command = command;
    }

    public String getId() {
        return id;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "CommandRequest[id=" + id + "; command=" + command + "]";
    }

}
