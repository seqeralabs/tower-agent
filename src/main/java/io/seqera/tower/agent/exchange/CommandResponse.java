package io.seqera.tower.agent.exchange;

import io.micronaut.core.annotation.ReflectiveAccess;

/**
 *
 * @author Jordi Deu-Pons <jordi@seqera.io>
 */
@ReflectiveAccess
public class CommandResponse {

    private String id;
    private String result;
    private int exitStatus;

    private boolean heartbeat;

    public CommandResponse() {}

    public CommandResponse(String id, String result, int exitStatus) {
        this.id = id;
        this.result = result;
        this.exitStatus = exitStatus;
    }

    public CommandResponse(boolean heartbeat) {
        this.heartbeat = heartbeat;
    }

    public String getId() {
        return id;
    }

    public String getResult() {
        return result;
    }

    public int getExitStatus() {
        return exitStatus;
    }

    public boolean isHeartbeat() {
        return heartbeat;
    }

    @Override
    public String toString() {
        return "CommandResponse[id="+id+"; result="+result+"; exitStatus="+exitStatus+"]";
    }

}
