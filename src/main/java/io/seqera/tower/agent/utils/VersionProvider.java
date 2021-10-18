package io.seqera.tower.agent.utils;

import picocli.CommandLine;

import java.util.Properties;

public class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        Properties properties = new Properties();
        properties.load(this.getClass().getResourceAsStream("/META-INF/build-info.properties"));
        return new String[]{String.format("@|yellow Tower Agent version %s (build %s)|@", properties.get("version"), properties.get("commitId"))};
    }
}
