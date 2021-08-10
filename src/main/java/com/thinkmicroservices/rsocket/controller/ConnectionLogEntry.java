package com.thinkmicroservices.rsocket.controller;

import java.sql.Time;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @author cwoodward
 */
@Data
@Builder
public class ConnectionLogEntry {

    private long timestamp = Instant.now().getEpochSecond();
    private ConnectionType type;
    private ConnectionState state;
}

enum ConnectionType {
    REQUEST_RESPONSE("request-response"), FIRE_AND_FORGET("fire-and-forget"),
    REQUEST_STREAM("request-stream"), CHANNEL("Channel");

    private String type;

    ConnectionType(String type) {
        this.type = type;

    }

    public String getType() {
        return type;
    }

  

}

  enum ConnectionState {
        CONNECTED("Connected"), DISCONNECTED("Disconnected"), ERROR("Error"),CANCEL("Cancel");

        private String state;

        ConnectionState(String state) {
            this.state=state;
        }
        
        public String getState(){
            return this.state;
        }
        
        
    }