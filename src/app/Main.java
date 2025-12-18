package app;

import dispatcher.Dispatcher;
import elevator.Elevator;
import sim.PassengerGenerator;
import util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        final int FLOORS = 15;
        final int ELEVATORS = 3;
        final int CAPACITY = 6;
        final int SIM_MS = 30000;

        List<Elevator> lifts = new ArrayList<>();
        for (int i = 0; i < ELEVATORS; i++) {
            lifts.add(new Elevator(i + 1, FLOORS, CAPACITY));
        }

        Dispatcher dispatcher = new Dispatcher(lifts);

        Thread dispThread = new Thread(dispatcher, "dispatcher");
        dispThread.start();

        List<Thread> liftThreads = new ArrayList<>();
        for (Elevator e : lifts) {
            Thread t = new Thread(e, "elevator-" + e.getId());
            t.start();
            liftThreads.add(t);
        }

        ScheduledExecutorService genSvc = Executors.newSingleThreadScheduledExecutor();
        PassengerGenerator.schedule(genSvc, dispatcher, FLOORS, 800);

        Log.info("Симуляция стартовала на " + SIM_MS + " ms");
        Thread.sleep(SIM_MS);

        Log.info("Остановка симуляции...");
        genSvc.shutdownNow();
        dispatcher.shutdown();
        for (Elevator e : lifts) ;
        dispThread.interrupt();
        for (Thread t : liftThreads) t.interrupt();
        dispThread.join();
        for (Thread t : liftThreads) t.join();

        Log.info("Симуляция завершена.");
    }
}


