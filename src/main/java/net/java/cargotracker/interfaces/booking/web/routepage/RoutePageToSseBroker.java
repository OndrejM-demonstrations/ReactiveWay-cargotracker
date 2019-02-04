package net.java.cargotracker.interfaces.booking.web.routepage;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.sse.*;

@ApplicationScoped
public class RoutePageToSseBroker {
    
    private Map<Long, CompletableFuture<SseEndpointData>> store;
    
    @PostConstruct
    public void init() {
        store = new HashMap<>();
    }
    
    public long initAndGetUniqueKey() {
        long id = Math.abs(new Random().nextLong());
        store.put(id, new CompletableFuture<>());
        return id;
    }
    
    public void connectSseEndpoint(long key, SseEventSink eventSink, Sse sse) {
        CompletableFuture<SseEndpointData> future = store.get(key);
        if (future != null) {
            future.complete(new SseEndpointData(eventSink, sse));
        }
    }
    
    public CompletionStage<SseEndpointData> afterSseEndpointConnected(long key) {
        return store.get(key);
    }
    
    public void shutdownForKey(long key) {
        store.remove(key);
    }
    
    public static class SseEndpointData {
        SseEventSink eventSink;
        Sse sse;

        public SseEndpointData(SseEventSink eventSink, Sse sse) {
            this.eventSink = eventSink;
            this.sse = sse;
        }

        public SseEventSink getEventSink() {
            return eventSink;
        }

        public Sse getSse() {
            return sse;
        }
        
    }
}
