package io.seqera.tower.agent.exchange;


import io.micronaut.core.annotation.Introspected;

/**
 *
 * @author Jordi Deu-Pons <jordi@seqera.io>
 */
@Introspected
public class CommandRequest {

    private String id;
    private String command;

    private boolean heartbeat;

    public CommandRequest() {}

    public CommandRequest(String id, String command) {
        this.id = id;
        this.command = command;
    }

    public CommandRequest(boolean heartbeat) {
        this.heartbeat = heartbeat;
    }

    public String getId() {
        return id;
    }

    public String getCommand() {
        return command;
    }

    public boolean isHeartbeat() {
        return heartbeat;
    }

    @Override
    public String toString() {
        return "CommandRequest[id="+id+"; command="+command+"]";
    }

}
