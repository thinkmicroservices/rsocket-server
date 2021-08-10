package com.thinkmicroservices.rsocket.message;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author cwoodward
 */

@SuperBuilder
@Data
public abstract class AbstractMessage {

    protected String source;
    protected String destination;
    @Builder.Default
    protected String uuid = UUID.randomUUID().toString();
    @Builder.Default
    protected long createdAt = System.currentTimeMillis();

}
