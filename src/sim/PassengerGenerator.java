package sim;

import dispatcher.Dispatcher;
import request.ExternalRequest;
import util.Log;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PassengerGenerator implements Runnable {
    private final Dispatcher dispatcher;
    private final int floors;
    private final Random rnd = new Random();

    public PassengerGenerator(Dispatcher dispatcher, int floors) {
        this.dispatcher = dispatcher;
        this.floors = floors;
    }

    @Override
    public void run() {
        int f = rnd.nextInt(floors) + 1;
        ExternalRequest.Direction dir;
        if (f == 1) dir = ExternalRequest.Direction.UP;
        else if (f == floors) dir = ExternalRequest.Direction.DOWN;
        else dir = rnd.nextBoolean() ? ExternalRequest.Direction.UP : ExternalRequest.Direction.DOWN;

        boolean pr = rnd.nextDouble() < 0.12;
        int pax = rnd.nextInt(4) + 1;
        var r = new ExternalRequest(f, dir, pr, pax);
        dispatcher.submitRequest(r);
        Log.info("Сгенерирован пассажирский запрос: " + r);
    }

    public static void schedule(ScheduledExecutorService svc, Dispatcher d, int floors, long periodMs) {
        PassengerGenerator gen = new PassengerGenerator(d, floors);
        svc.scheduleAtFixedRate(gen, 0, periodMs, TimeUnit.MILLISECONDS);
    }
}
