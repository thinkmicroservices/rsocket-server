package com.thinkmicroservices.rsocket.controller;

import com.thinkmicroservices.rsocket.message.Request;
import com.thinkmicroservices.rsocket.message.Event;
import com.thinkmicroservices.rsocket.message.Response;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 *
 * @author cwoodward
 */
@Controller
@Slf4j

public class RSocketController {

    public static final String SOURCE = "RSOCKET Server";

    public static final String RESPONSE_PREFIX = "RECEIVED ";

    public static final String RSOCKET_ROUTE_FIRE_AND_FORGET = "fire-and-forget";
    public static final String RSOCKET_ROUTE_REQUEST_RESPONSE = "request-response";
    public static final String RSOCKET_ROUTE_REQUEST_STREAM = "request-stream";
    public static final String RSOCKET_ROUTE_CHANNEL = "channel";

    public static final String FIRE_AND_FORGET_FORMAT = "FIRE_AND_FORGET: '%s'";
    public static final String REQUEST_RESPONSE_FORMAT = "%s: '%s'";

    private final List<ConnectionLogEntry> clientConnectionLog = new ArrayList<>();
    private final Set<Sinks.Many<Event>> clientOutboundChannels = new HashSet<>();

    @MessageMapping(RSOCKET_ROUTE_FIRE_AND_FORGET)
    public void fireAndForget(RSocketRequester requester, Request request) {
        registerRequester(requester, ConnectionType.FIRE_AND_FORGET);
        log.info("Received 'fire and forget' request: " + request);
        clientOutboundChannels.stream().forEach(currentSink -> {

            Event event = Event.builder()
                    .source(SOURCE)
                    .destination("BROADCAST")
                    .message(String.format(FIRE_AND_FORGET_FORMAT, request.getMessage()))
                    .build();
            log.info("send fire-and-forget :" + currentSink.name() + ",event: " + event);
            currentSink.tryEmitNext(event);
        });
    }

    @MessageMapping(RSOCKET_ROUTE_REQUEST_RESPONSE)

    public Response requestResponse(RSocketRequester requester, Request request) {
        registerRequester(requester, ConnectionType.REQUEST_RESPONSE);
        log.info("RSocketRequester:" + requester);

        log.info("Received 'request/response' request: " + request);

        return Response.builder()
                .requestUuid(request.getUuid())
                .requestMessage(request.getMessage())
                .source(SOURCE)
                .destination(request.getSource())
                .responseMessage(String.format(REQUEST_RESPONSE_FORMAT, RESPONSE_PREFIX, request.getMessage()))
                .build();
    }

    @MessageMapping(RSOCKET_ROUTE_REQUEST_STREAM)
    Flux<Event> requestStream(RSocketRequester requester, Request request) {
        registerRequester(requester, ConnectionType.REQUEST_STREAM);
        log.info("Received 'request/stream' request: " + request);
        // create an empty flux
        Flux<Event> responseFlux = Flux.empty();

        log.info("client connection count:" + clientConnectionLog.size());

        for (ConnectionLogEntry entry : clientConnectionLog) {

            Event newResponse = Event.builder()
                    .source(SOURCE)
                    .destination(request.getSource())
                    .message(entry.toString())
                    .build();

            responseFlux = Flux.concat(responseFlux, Flux.just(newResponse));
        }

        return responseFlux;

    }

    @MessageMapping(RSOCKET_ROUTE_CHANNEL)
    /**
     *
     * @param requester
     * @param requestFlux
     * @return
     */
    public Flux<Event> channel(RSocketRequester requester, Flux<Request> requestFlux) {

        // add the requester
        registerRequester(requester, ConnectionType.CHANNEL);
        log.info("Received 'channel' request" + clientConnectionLog.size());

        // create a sink where we can send event messages
        Sinks.Many<Event> sink = Sinks.many().multicast().onBackpressureBuffer();
        //add the outbound stream to the set

        // add the sink to the list of outbound channels
        clientOutboundChannels.add(sink);

        requestFlux.subscribe(request -> {

            log.info("channel processing request:" + request);

            // create an event to send out all connected outbound connections
            Event event = Event.builder().message(request.getMessage()).build();
            // iterate over the  outbound channels and send each an event
            clientOutboundChannels.stream().forEach(currentSink -> {

                log.info("send channel event :" + currentSink.name() + ",event: " + event);
                currentSink.tryEmitNext(event);
            });

        });

        return sink.asFlux();
    }

    /**
     *
     * @param requester
     * @param type
     */
    private void registerRequester(RSocketRequester requester, ConnectionType type) {
        log.info("reqisterRequester:" + type);
        requester.rsocket()
                .onClose()
                .doFirst(() -> {
                    // Add all new clients to a client list
                    log.info("RSocket connected.", requester.rsocketClient().source());
                    clientConnectionLog.add(ConnectionLogEntry.builder()
                            .state(ConnectionState.CONNECTED)
                            .type(type)
                            .build());
                })
                .doOnError(error -> {
                    // Warn when channels are closed by clients
                    log.warn("RSocket error.", requester.rsocketClient().source());
                    clientConnectionLog.add(ConnectionLogEntry.builder()
                            .state(ConnectionState.ERROR)
                            .type(type)
                            .build());
                })
                .doOnCancel(() -> {
                    log.warn("RSocket cancel.", requester.rsocketClient().source());
                    clientConnectionLog.add(ConnectionLogEntry.builder()
                            .state(ConnectionState.CANCEL)
                            .type(type)
                            .build());
                })
                .doFinally(consumer -> {

                    log.info("RSocket finally.", requester.rsocketClient().source());
                    clientConnectionLog.add(ConnectionLogEntry.builder()
                            .state(ConnectionState.DISCONNECTED)
                            .type(type)
                            .build());
                })
                .subscribe();
    }

}
