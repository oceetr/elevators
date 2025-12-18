package elevator;

import util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static elevator.ElevatorState.Direction;
import static elevator.ElevatorState.Status;

public class Elevator implements Runnable {
    private final int id;
    private final int maxFloor;
    private int currentFloor = 1;
    private Direction dir = Direction.NONE;
    private Status status = Status.IDLE;

    private final List<Target> targets = new ArrayList<>();
    private final int capacity;
    private int load = 0;

    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean running = true;

    public Elevator(int id, int maxFloor, int capacity) {
        this.id = id;
        this.maxFloor = maxFloor;
        this.capacity = Math.max(1, capacity);
    }

    public int getId() { return id; }

    public static class Snapshot {
        public final int currentFloor;
        public final Direction dir;
        public final Status status;
        public final int load;
        public final int capacity;
        public final int reserved;

        public Snapshot(int currentFloor, Direction dir, Status status, int load, int capacity, int reserved) {
            this.currentFloor = currentFloor;
            this.dir = dir;
            this.status = status;
            this.load = load;
            this.capacity = capacity;
            this.reserved = reserved;
        }

        public int freeCapacity() {
            return capacity - load - reserved;
        }
    }

    public Snapshot snapshot() {
        lock.lock();
        try {
            int reserved = 0;
            for (Target t : targets) {
                if (!t.internal) reserved += t.waitingPax;
            }
            return new Snapshot(currentFloor, dir, status, load, capacity, reserved);
        } finally {
            lock.unlock();
        }
    }

    public boolean tryReserve(int floor, int pax) {
        lock.lock();
        try {
            int reserved = load;
            for (Target t : targets) {
                reserved += t.waitingPax;
            }
            if (reserved + pax > capacity) return false;
            for (Target t : targets) {
                if (t.floor == floor && !t.internal) {
                    t.waitingPax += pax;
                    return true;
                }
            }
            targets.add(new Target(floor, pax, false));
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void addTarget(int floor, int expectedPassengers) {
        lock.lock();
        try {
            boolean found = false;
            for (Target t : targets) {
                if (t.floor == floor && !t.internal) {
                    t.waitingPax += expectedPassengers;
                    found = true;
                    break;
                }
            }
            if (!found) targets.add(new Target(floor, expectedPassengers, false));
            Log.info("Назначение цели: Лифт " + id + " <- этаж " + floor + " (ожидаемо pax=" + expectedPassengers + ")");
        } finally {
            lock.unlock();
        }
    }

    public void addInternalTarget(int floor) {
        lock.lock();
        try {
            for (Target t : targets) {
                if (t.floor == floor && t.internal) return;
            }
            targets.add(new Target(floor, 0, true));
            Log.info("Внутренняя цель: Лифт " + id + " -> этаж " + floor);
        } finally {
            lock.unlock();
        }
    }

    private Integer chooseNextTargetLocked() {
        if (targets.isEmpty()) return null;
        if (dir == Direction.UP) {
            int best = Integer.MAX_VALUE; Integer res = null;
            for (Target t : targets) if (t.floor >= currentFloor) {
                int d = t.floor - currentFloor;
                if (d < best) { best = d; res = t.floor; }
            }
            if (res != null) return res;
        } else if (dir == Direction.DOWN) {
            int best = Integer.MAX_VALUE; Integer res = null;
            for (Target t : targets) if (t.floor <= currentFloor) {
                int d = currentFloor - t.floor;
                if (d < best) { best = d; res = t.floor; }
            }
            if (res != null) return res;
        }
        int best = Integer.MAX_VALUE; Integer res = null;
        for (Target t : targets) {
            int d = Math.abs(t.floor - currentFloor);
            if (d < best) { best = d; res = t.floor; }
        }
        return res;
    }

    private Target findTargetByFloorLocked(int floor) {
        for (Target t : targets) if (t.floor == floor) return t;
        return null;
    }

    private void removeTargetIfClearedLocked(int floor) {
        Iterator<Target> it = targets.iterator();
        while (it.hasNext()) {
            Target t = it.next();
            if (t.floor == floor) {
                if (t.internal || t.waitingPax <= 0) it.remove();
            }
        }
    }

    private void simulateBoardingAndAlightingLocked(Target t) throws InterruptedException {
        int alight = (int)(Math.random() * (load + 1));
        if (alight > 0) {
            load -= alight;
            Log.info("Лифт " + id + " высадил " + alight + " пассажиров. Загрузка: " + load + "/" + capacity);
        }
        int free = capacity - load;
        int boarding = Math.min(free, t.waitingPax);
        if (boarding > 0) {
            load += boarding;
            t.waitingPax -= boarding;
            Log.info("Лифт " + id + " принял " + boarding + " пассажиров на этаже " + t.floor + ". Загрузка: " + load + "/" + capacity);
            for (int i = 0; i < boarding; i++) {
                int dest;
                do {
                    dest = 1 + (int)(Math.random() * maxFloor);
                } while (dest == t.floor);
                boolean found = false;
                for (Target tt : targets) {
                    if (tt.floor == dest && tt.internal) { found = true; break; }
                }
                if (!found) targets.add(new Target(dest, 0, true));
                Log.info("Внутренняя цель: Лифт " + id + " -> этаж " + dest);
            }
        }
        if (t.waitingPax > 0) {
            Log.info("Осталось ожидать на этаже " + t.floor + " пассажиров: " + t.waitingPax);
        }
    }

    @Override
    public void run() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                Integer next;
                lock.lock();
                try {
                    next = chooseNextTargetLocked();
                    if (next == null) {
                        dir = Direction.NONE;
                        status = Status.IDLE;
                    } else {
                        if (next > currentFloor) dir = Direction.UP;
                        else if (next < currentFloor) dir = Direction.DOWN;
                        else dir = Direction.NONE;
                        status = Status.MOVING;
                    }
                } finally {
                    lock.unlock();
                }

                if (next == null) {
                    Thread.sleep(200);
                    continue;
                }

                boolean reached = false;
                while (!reached && running && !Thread.currentThread().isInterrupted()) {
                    lock.lock();
                    try {
                        if (dir == Direction.UP) currentFloor++;
                        else if (dir == Direction.DOWN) currentFloor--;
                        Log.info("Лифт " + id + " подъехал к этажу " + currentFloor);
                        Target t = findTargetByFloorLocked(currentFloor);
                        if (t != null) {
                            status = Status.DOORS_OPEN;
                            Log.info("Лифт " + id + " остановился на этаже " + currentFloor);
                            lock.unlock();
                            try {
                                DoorController.open(id, currentFloor);
                                lock.lock();
                                try {
                                    simulateBoardingAndAlightingLocked(t);
                                } finally {
                                    lock.unlock();
                                }
                                DoorController.close(id, currentFloor);
                            } finally {
                                if (lock.isHeldByCurrentThread()) {
                                } else {
                                }
                            }
                            lock.lock();
                            try {
                                removeTargetIfClearedLocked(currentFloor);
                                status = Status.MOVING;
                            } finally {
                                lock.unlock();
                            }
                        }
                        if (currentFloor == next) reached = true;
                        lock.lock();
                        try {
                            if (currentFloor <= 1) dir = Direction.UP;
                            else if (currentFloor >= maxFloor) dir = Direction.DOWN;
                        } finally {
                            lock.unlock();
                        }
                    } finally {
                        if (lock.isHeldByCurrentThread()) lock.unlock();
                    }
                    Thread.sleep(250);
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            Log.info("Лифт " + id + " завершает работу.");
        }
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            List<Target> copy = new ArrayList<>(targets);
            return "Elevator{" +
                    "id=" + id +
                    ", floor=" + currentFloor +
                    ", dir=" + dir +
                    ", status=" + status +
                    ", load=" + load + "/" + capacity +
                    ", targets=" + copy +
                    '}';
        } finally {
            lock.unlock();
        }
    }
}




