package dispatcher;

import elevator.Elevator;
import elevator.ElevatorState;
import request.ExternalRequest;
import util.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class Dispatcher implements Runnable {
    private final List<Elevator> elevators;
    private final BlockingQueue<ExternalRequest> queue = new PriorityBlockingQueue<>();
    private volatile boolean running = true;

    public Dispatcher(List<Elevator> elevators) {
        this.elevators = elevators;
    }

    public void submitRequest(ExternalRequest req) {
        try {
            Log.info("Поступил запрос (генератор): " + req);
            queue.put(req);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() { running = false; }

    private int costFor(Elevator.Snapshot snap, ExternalRequest r) {
        int dist = Math.abs(snap.currentFloor - r.getFloor());

        if (snap.dir == ElevatorState.Direction.UP
                && r.getDirection() == ExternalRequest.Direction.UP
                && r.getFloor() >= snap.currentFloor) {
            dist -= 2;
        } else if (snap.dir == ElevatorState.Direction.DOWN
                && r.getDirection() == ExternalRequest.Direction.DOWN
                && r.getFloor() <= snap.currentFloor) {
            dist -= 2;
        }

        if (r.isPriority()) dist -= 3;

        if (snap.freeCapacity() <= 0) dist += 1000;

        return Math.max(0, dist);
    }

    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                ExternalRequest req = queue.take();
                List<Elevator> candidates = new ArrayList<>(elevators);
                candidates.sort(Comparator.comparingInt(e -> costFor(e.snapshot(), req)));

                boolean assigned = false;
                for (Elevator e : candidates) {
                    if (e.tryReserve(req.getFloor(), req.getPassengers())) {
                        Log.info("Диспетчер зарезервировал места и назначил лифт " + e.getId() + " на " + req);
                        assigned = true;
                        break;
                    } else {
                    }
                }

                if (!assigned) {
                    Log.info("Не удалось зарезервировать места для " + req + " — возвращаю в очередь");
                    Thread.sleep(200);
                    queue.put(req);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Log.info("Диспетчер завершает работу.");
    }
}


