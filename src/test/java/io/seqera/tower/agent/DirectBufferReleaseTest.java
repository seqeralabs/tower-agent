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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Guards the native image build configuration rather than agent logic, so it only
 * means something under {@code nativeTest}; on the JVM it always passes.
 * <p>
 * On Java 25, Netty frees any direct buffer above its 1 MiB pooling threshold with
 * {@code Arena.ofShared().close()}, which a native image supports only when built with
 * {@code -H:+SharedArenaSupport}. Without it, every command response whose encoded
 * frame crosses that threshold fails to send.
 */
class DirectBufferReleaseTest {

    @Test
    void releasesDirectBufferAbovePoolingThreshold() {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer(2 * 1024 * 1024);
        Assertions.assertTrue(buffer.release());
    }

}
