package net.java.cargotracker.interfaces.booking.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.*;
import net.java.cargotracker.interfaces.booking.facade.BookingServiceFacade;
import net.java.cargotracker.interfaces.booking.facade.dto.CargoRoute;
import net.java.cargotracker.interfaces.booking.facade.dto.RouteCandidate;
import net.java.cargotracker.interfaces.booking.web.routepage.RoutePageToSseBroker;

/**
 * Handles itinerary selection. Operates against a dedicated service facade, and
 * could easily be rewritten as a thick Swing client. Completely separated from
 * the domain layer, unlike the tracking user interface.
 * <p/>
 * In order to successfully keep the domain model shielded from user interface
 * considerations, this approach is generally preferred to the one taken in the
 * tracking controller. However, there is never any one perfect solution for all
 * situations, so we've chosen to demonstrate two polarized ways to build user
 * interfaces.
 *
 * @see net.java.cargotracker.interfaces.tracking.CargoTrackingController
 */
@Named
@ViewScoped
public class ItinerarySelection implements Serializable {

    private static final long serialVersionUID = 1L;
    private boolean loadingStarted;
    private boolean loadingFinished;
    private String trackingId;
    private CargoRoute cargo;
    List<RouteCandidate> routeCandidates;
    @Inject
    private BookingServiceFacade bookingServiceFacade;
    private static final Logger log = Logger.getLogger(
            ItinerarySelection.class.getName());

    @Inject
    private RoutePageToSseBroker broker;
    private long sseKey;

    @PostConstruct
    public void init() {
        routeCandidates = new ArrayList<>();
        sseKey = broker.initAndGetUniqueKey();
    }
    
    @PreDestroy
    public void shutdown() {
        broker.shutdownForKey(sseKey);
    }

    public List<RouteCandidate> getRouteCandidates() {
        return routeCandidates;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public CargoRoute getCargo() {
        return cargo;
    }

    public List<RouteCandidate> getRouteCanditates() {
        return routeCandidates;
    }

    public void load() {
        if (hasLoadingStarted()) {
            return;
        }
        loadingStarted = true;
        cargo = bookingServiceFacade.loadCargoForRouting(trackingId);
        bookingServiceFacade
                .requestPossibleRoutesForCargo(trackingId)
                .doOnNext(routeCandidate -> {
                    log.info(() -> "Accepted " + routeCandidate);
                    routeCandidates.add(routeCandidate);
                    
                    broker.afterSseEndpointConnected(sseKey)
                            .thenAccept(sseData -> sendSseMessge("refresh", sseData));
                }).doOnError(e -> {
                    log.log(Level.WARNING, e, () -> "Error: " + e.getMessage());
                    broker.afterSseEndpointConnected(sseKey)
                            .thenAccept(sseData -> sendSseMessge("error: " + e.getMessage(), sseData));
                })
                .doOnComplete(() -> {
                    loadingFinished = true;
                    broker.afterSseEndpointConnected(sseKey)
                            .thenAccept(sseData -> sendSseMessge("finished", sseData));
                })
                .subscribe();
    }

    private void sendSseMessge(String message, RoutePageToSseBroker.SseEndpointData sseData) {
        OutboundSseEvent.Builder sseDataEventBuilder = sseData.getSse().newEventBuilder()
                .mediaType(MediaType.TEXT_PLAIN_TYPE);
        sseData.getEventSink().send(
                sseDataEventBuilder
                        .data(message)
                        .build()
        );
    }

    private boolean hasLoadingStarted() {
        return loadingStarted;
    }

    public boolean isLoadingFinished() {
        return loadingFinished;
    }

    public String assignItinerary(int routeIndex) {
        RouteCandidate route = routeCandidates.get(routeIndex);
        bookingServiceFacade.assignCargoToRoute(trackingId, route);

        return "show.html?faces-redirect=true&trackingId=" + trackingId;
    }

    public long getSseKey() {
        return sseKey;
    }
    
}
