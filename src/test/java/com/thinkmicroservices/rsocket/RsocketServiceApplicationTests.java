package com.thinkmicroservices.rsocket;

import com.thinkmicroservices.rsocket.controller.RSocketController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.thinkmicroservices.rsocket.message.Event;
import com.thinkmicroservices.rsocket.message.MessageType;

import com.thinkmicroservices.rsocket.message.Request;
import com.thinkmicroservices.rsocket.message.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
class RsocketServiceApplicationTests {

    private static final String TEST_DESTINATION = "TEST-DESTINATION";
    private static final String TEST_SOURCE = "TEST-SOURCE";
    private static final String REQUEST_CONTENT = "This is a request!";
    private static final String CHANNEL_CONTENT = "This is a channel request!";
    private static final String FIRE_AND_FORGET_CONTENT = "FIRE!";
    private static RSocketRequester rSocketRequester;

    @BeforeAll
    public static void setupOnce(@Autowired RSocketRequester.Builder builder, @Value("${spring.rsocket.server.port}") Integer port) {
        rSocketRequester = builder.tcp("localhost", port);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void testRequestResponse() {
        /* create a request */
        Mono<Response> responseMono = rSocketRequester
                .route(RSocketController.RSOCKET_ROUTE_REQUEST_RESPONSE)
                .data(Request.builder()
                        .message(REQUEST_CONTENT)
                        .source(TEST_SOURCE)
                        .destination(TEST_DESTINATION).build())
                .retrieveMono(Response.class);

        /* Verify that the response message contains the expected data */
        StepVerifier
                .create(responseMono)
                .consumeNextWith(response -> {
                    System.out.println("Response:" + response);
                    assertThat(response.getType()).isEqualTo(MessageType.RESPONSE);
                    assertThat(response.getSource()).isEqualTo(RSocketController.SOURCE);
                    assertThat(response.getDestination()).isEqualTo(TEST_SOURCE);
                    assertThat(response.getCreatedAt()).isGreaterThan(Long.MIN_VALUE);
                    assertNotNull(response.getUuid());
                    assertThat(response.getResponseMessage()).isEqualTo(
                            String.format(RSocketController.REQUEST_RESPONSE_FORMAT, RSocketController.RESPONSE_PREFIX, REQUEST_CONTENT)
                    );
                })
                .verifyComplete();
    }

    @Test
    void testRequestStream() {

        Flux<Event> result = rSocketRequester
                .route(RSocketController.RSOCKET_ROUTE_REQUEST_STREAM)
                .data(Request.builder()
                        .source(TEST_SOURCE)
                        .destination(TEST_DESTINATION)
                        .build())
                .retrieveFlux(Event.class);

        // Verify that the response messages contain the expected data
        StepVerifier
                .create(result)
                .consumeNextWith(event -> {
                    assertThat(event.getType()).isEqualTo(MessageType.EVENT);
                    assertThat(event.getSource()).isEqualTo(RSocketController.SOURCE);
                    assertThat(event.getDestination()).isEqualTo(TEST_SOURCE);
                    assertThat(event.getCreatedAt()).isGreaterThan(Long.MIN_VALUE);
                    assertNotNull(event.getUuid());

                }
                )
                .thenCancel()
                .verify();
    }

    // @Test
    public void testChannelGetsStream() {

        Flux<Request> requestFlux = Flux.just(
                Request.builder()
                        .message(CHANNEL_CONTENT)
                        .source(TEST_SOURCE)
                        .destination(TEST_DESTINATION).build()
        );

        /* create the initial request*/
        Flux<Event> events = rSocketRequester
                .route(RSocketController.RSOCKET_ROUTE_CHANNEL)
                .data(requestFlux)  
                .retrieveFlux(Event.class);

        /* add a fire and forget request */
       
        rSocketRequester
                .route(RSocketController.RSOCKET_ROUTE_FIRE_AND_FORGET)
                .data(Request.builder().message(FIRE_AND_FORGET_CONTENT))
                .retrieveMono(Void.class);

        StepVerifier
                .create(events)
                .consumeNextWith(event1 -> {
                    assertThat(event1.getType()).isEqualTo(MessageType.EVENT);
                    assertThat(event1.getSource()).isEqualTo(RSocketController.SOURCE);
                    assertThat(event1.getDestination()).isEqualTo(TEST_SOURCE);
                    assertThat(event1.getCreatedAt()).isGreaterThan(Long.MIN_VALUE);
                    assertNotNull(event1.getUuid());
                    assertThat(event1.getMessage()).isEqualTo(String.format("%s: '%s'", RSocketController.RESPONSE_PREFIX, FIRE_AND_FORGET_CONTENT));
                })
                .consumeNextWith(event2 -> {
                    assertThat(event2.getType()).isEqualTo(MessageType.EVENT);
                    assertThat(event2.getSource()).isEqualTo(RSocketController.SOURCE);
                    assertThat(event2.getDestination()).isEqualTo(TEST_SOURCE);
                    assertThat(event2.getCreatedAt()).isGreaterThan(Long.MIN_VALUE);
                    assertNotNull(event2.getUuid());
                    assertThat(event2.getMessage()).isEqualTo(FIRE_AND_FORGET_CONTENT);

                })
                .thenCancel()
                .verify();

    }

}
