package com.thinkmicroservices.rsocket.message;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 *
 * @author cwoodward
 */
@Data
@SuperBuilder
@Jacksonized
public class Request  extends AbstractMessage {
     @Builder.Default
    protected MessageType type=MessageType.REQUEST;
    public String message; 
}

