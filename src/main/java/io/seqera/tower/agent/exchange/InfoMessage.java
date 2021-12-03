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

package io.seqera.tower.agent.exchange;

import io.micronaut.core.annotation.ReflectiveAccess;

/**
 * @author Jordi Deu-Pons <jordi@seqera.io>
 */
@ReflectiveAccess
public class InfoMessage extends AgentMessage {

    private String userName;
    private String workDir;
    private String agentVersion;

    public InfoMessage() {
    }

    public InfoMessage(String userName, String workDir, String agentVersion) {
        this.userName = userName;
        this.workDir = workDir;
        this.agentVersion = agentVersion;
    }

    public String getUserName() {
        return userName;
    }

    public String getWorkDir() {
        return workDir;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    @Override
    public String toString() {
        return "InfoMessage[" +
                "userName='" + userName + '\'' +
                "; workDir='" + workDir + '\'' +
                "; agentVersion='" + agentVersion + '\'' +
                ']';
    }
}
