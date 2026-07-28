package net.dodian.uber.game.model.entity.player;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

/** Encapsulated RSMod-style queue of absolute route waypoints. */
public final class RouteDestination {
    public static final int MAX_WAYPOINTS = 80;

    private final Deque<MovementPoint> waypoints = new ArrayDeque<>();
    private boolean running;

    public void replace(Collection<MovementPoint> route, boolean running) {
        if (route.size() > MAX_WAYPOINTS) {
            throw new IllegalArgumentException(
                    "Route contains " + route.size() + " waypoints; maximum is " + MAX_WAYPOINTS
            );
        }
        waypoints.clear();
        waypoints.addAll(route);
        this.running = running;
    }

    public void clear() {
        waypoints.clear();
        running = false;
    }

    public void add(MovementPoint point) {
        if (waypoints.size() >= MAX_WAYPOINTS) {
            throw new IllegalStateException("Route waypoint capacity exceeded");
        }
        waypoints.addLast(point);
    }

    public MovementPoint peekFirst() {
        return waypoints.peekFirst();
    }

    public MovementPoint pollFirst() {
        MovementPoint point = waypoints.pollFirst();
        if (waypoints.isEmpty()) {
            running = false;
        }
        return point;
    }

    public MovementPoint destination() {
        return waypoints.peekLast();
    }

    public int size() {
        return waypoints.size();
    }

    public boolean isEmpty() {
        return waypoints.isEmpty();
    }

    public boolean isNotEmpty() {
        return !waypoints.isEmpty();
    }

    public boolean isRunning() {
        return running;
    }

    public List<MovementPoint> snapshot() {
        return List.copyOf(waypoints);
    }
}
