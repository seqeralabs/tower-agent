/*
 * Copyright 2021-2024, Seqera.
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
 *
 */

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
