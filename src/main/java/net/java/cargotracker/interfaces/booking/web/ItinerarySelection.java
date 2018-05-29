package net.java.cargotracker.interfaces.booking.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import net.java.cargotracker.infrastructure.routing.ExternalRoutingService;
import net.java.cargotracker.interfaces.booking.facade.BookingServiceFacade;
import net.java.cargotracker.interfaces.booking.facade.dto.CargoRoute;
import net.java.cargotracker.interfaces.booking.facade.dto.RouteCandidate;
import org.omnifaces.cdi.*;

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
    @Push(channel = "routeCandidates")
    private PushContext push;

    private CompletableFuture<Void> websocketTriggered;

    @PostConstruct
    public void init() {
        routeCandidates = new ArrayList<>();
        websocketTriggered = new CompletableFuture<>();
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

    public void pageLoaded() {
        websocketTriggered.complete(null);
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
                    websocketTriggered.thenRun(() -> {
                        push.send("refresh");
                    });
                }).doOnError(e -> {
                    log.log(Level.WARNING, e, () -> "Error: " + e.getMessage());
                    websocketTriggered.thenRun(() -> {
                        push.send("error: " + e.getMessage());
                    });
                })
                .doOnComplete(() -> {
                    loadingFinished = true;
                    websocketTriggered.thenRun(() -> {
                        push.send("finished");
                    });
                })
                .subscribe();
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
}
