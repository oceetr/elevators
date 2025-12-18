package elevator;

public class Target {
    public final int floor;
    public int waitingPax;
    public final boolean internal;

    public Target(int floor, int pax, boolean internal) {
        this.floor = floor;
        this.waitingPax = pax;
        this.internal = internal;
    }

    @Override
    public String toString() {
        if (internal) return "(" + floor + ":internal)";
        return "(" + floor + ":ext=" + waitingPax + ")";
    }
}
