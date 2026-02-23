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
