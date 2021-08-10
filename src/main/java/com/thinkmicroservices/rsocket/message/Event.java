package com.thinkmicroservices.rsocket.message;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 *
 * @author cwoodward
 */
@SuperBuilder
@Data
@Jacksonized
public class Event extends AbstractMessage {

     @Builder.Default
    protected MessageType type=MessageType.EVENT;
    private String message;

}
