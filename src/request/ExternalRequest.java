package request;

public class ExternalRequest implements Comparable<ExternalRequest> {
    public enum Direction { UP, DOWN }

    private final int floor;
    private final Direction dir;
    private final boolean priority;
    private final int passengers;
    private final long ts;

    public ExternalRequest(int floor, Direction dir, boolean priority, int passengers) {
        this.floor = floor;
        this.dir = dir;
        this.priority = priority;
        this.passengers = Math.max(1, passengers);
        this.ts = System.nanoTime();
    }

    public int getFloor() { return floor; }
    public Direction getDirection() { return dir; }
    public boolean isPriority() { return priority; }
    public int getPassengers() { return passengers; }

    @Override
    public int compareTo(ExternalRequest o) {
        if (this.priority && !o.priority) return -1;
        if (!this.priority && o.priority) return 1;
        return Long.compare(this.ts, o.ts);
    }

    @Override
    public String toString() {
        return "ExternalRequest{floor=" + floor + ", dir=" + dir + ", pr=" + priority + ", pax=" + passengers + '}';
    }
}
