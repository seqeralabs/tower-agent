package io.seqera.tower.agent.exchange;

import io.micronaut.core.annotation.ReflectiveAccess;

import java.nio.charset.StandardCharsets;

/**
 * @author Jordi Deu-Pons <jordi@sequera.io>
 */
@ReflectiveAccess
public class CommandResponse extends AgentMessage {
    private String id;
    private byte[] result;
    private int exitStatus;

    public CommandResponse() {
    }

    public CommandResponse(String id, byte[] result, int exitStatus) {
        this.id = id;
        this.result = result;
        this.exitStatus = exitStatus;
    }

    public String getId() {
        return id;
    }

    public byte[] getResult() {
        return result;
    }

    public String getResultAsString() {
        if (result == null) {
            return null;
        }

        return new String(result, StandardCharsets.UTF_8);
    }

    public int getExitStatus() {
        return exitStatus;
    }

    @Override
    public String toString() {
        return "CommandResponse[id=" + id + "; result=" + new String(result, StandardCharsets.UTF_8) + "; exitStatus=" + exitStatus + "]";
    }

}
