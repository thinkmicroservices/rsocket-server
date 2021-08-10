
package com.thinkmicroservices.rsocket.message;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 *
 * @author cwoodward
 */
@Data
@SuperBuilder
@Jacksonized
public class Response extends AbstractMessage {
     @Builder.Default
    protected MessageType type=MessageType.RESPONSE;
    protected String requestUuid;
    protected String requestMessage;
    protected String responseMessage;
}
