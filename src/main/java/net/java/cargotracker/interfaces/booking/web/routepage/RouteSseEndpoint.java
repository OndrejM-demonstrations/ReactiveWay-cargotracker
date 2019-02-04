package net.java.cargotracker.interfaces.booking.web.routepage;

import java.io.Serializable;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.*;
import net.java.cargotracker.interfaces.booking.web.routepage.RoutePageToSseBroker;

@Path("route-data")
@RequestScoped
public class RouteSseEndpoint implements Serializable {

    @Inject
    RoutePageToSseBroker broker;

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void connectToReceiveEvents(
            @QueryParam("key") long key,
            @Context SseEventSink eventSink, 
            @Context Sse sse) {
        broker.connectSseEndpoint(key, eventSink, sse);
    }

}
