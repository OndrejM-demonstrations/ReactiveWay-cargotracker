package net.java.pathfinder.api.reactive;

import java.io.Serializable;
import java.util.Random;

public class GraphTraversalRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String origin;
    private final String destination;
    private final long id = new Random().nextLong();
    
    public GraphTraversalRequest(String origin, String destination) {
        this.origin = origin;
        this.destination = destination;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public long getId() {
        return id;
    }

}
