package io.seqera.tower.agent.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CommandRequest.class, name = "command-request"),
        @JsonSubTypes.Type(value = CommandResponse.class, name = "command-response"),
        @JsonSubTypes.Type(value = HeartbeatMessage.class, name = "heartbeat")
})
public abstract class AgentMessage {}
