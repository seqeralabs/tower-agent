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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class AgentErrorResultTest {

    @Test
    void usesExceptionMessage() {
        Assertions.assertEquals("boom", new String(Agent.errorResult(new IOException("boom"))));
    }

    @Test
    void fallsBackToExceptionTypeWithoutMessage() {
        // ProcessBuilder.start() throws a message-less NullPointerException for a null command
        NullPointerException e = Assertions.assertThrows(NullPointerException.class,
                () -> new ProcessBuilder().command("sh", "-c", null).start());

        Assertions.assertEquals("java.lang.NullPointerException", new String(Agent.errorResult(e)));
    }

}
