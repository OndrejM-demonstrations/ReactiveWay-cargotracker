package net.java.cargotracker.infrastructure.routing;

import fish.payara.cdi.jsr107.impl.NamedCache;
import fish.payara.micro.cdi.Inbound;
import fish.payara.micro.cdi.Outbound;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.schedulers.Schedulers;
import net.java.pathfinder.api.TransitPath;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.cache.Cache;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import net.java.pathfinder.api.reactive.GraphTraversalRequest;
import net.java.pathfinder.api.reactive.GraphTraversalResponse;

@ApplicationScoped
class GraphTraversalResource {

    @Inject
    @Outbound
    private Event<GraphTraversalRequest> requestEvent;

    @Resource
    private ManagedExecutorService executor;

    @Inject
    @NamedCache(cacheName = "GraphTraversalRequest")
    Cache<Long, String> atMostOnceDeliveryCache;

    private ConcurrentHashMap<Long, FlowableEmitter<TransitPath>> resultEmitterMap = new ConcurrentHashMap<>();

    public Flowable<TransitPath> get(String origin, String destination) {
        GraphTraversalRequest request = new GraphTraversalRequest(origin, destination);
        return Flowable.create(
                (FlowableEmitter<TransitPath> emitter) -> onSubscribe(emitter, request),
                BackpressureStrategy.BUFFER)
                .timeout(1, TimeUnit.MINUTES, Schedulers.from(executor));
    }

    private void onSubscribe(FlowableEmitter<TransitPath> emitter, GraphTraversalRequest request) {
        resultEmitterMap.put(request.getId(), emitter);
        atMostOnceDeliveryCache.put(request.getId(), "");
        requestEvent.fire(request);
    }

    public void handleResponse(@Observes @Inbound GraphTraversalResponse response) {
        FlowableEmitter<TransitPath> resultEmitter;
        if (response.isCompleted()) {
            resultEmitter = resultEmitterMap.remove(response.getId());
        } else {
            resultEmitter = resultEmitterMap.get(response.getId());
        }
        if (resultEmitter != null) {
            try {
                if (response.getTransitPath() != null) {
                    resultEmitter.onNext(response.getTransitPath());
                }
                if (response.isCompleted()) {
                    resultEmitter.onComplete();
                }
            } catch (Exception e) {
                resultEmitterMap.remove(response.getId());
                resultEmitter.onError(e);
            }
        }
    }

}
