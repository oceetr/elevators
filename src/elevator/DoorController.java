package elevator;

import util.Log;

public class DoorController {
    private static final int OPEN_MS = 200;
    private static final int CLOSE_MS = 200;

    public static void open(int elevatorId, int floor) throws InterruptedException {
        Log.info("Лифт " + elevatorId + " двери открываются на этаже " + floor);
        Thread.sleep(OPEN_MS);
        Log.info("Лифт " + elevatorId + " двери открыты на этаже " + floor);
    }

    public static void close(int elevatorId, int floor) throws InterruptedException {
        Log.info("Лифт " + elevatorId + " двери закрываются на этаже " + floor);
        Thread.sleep(CLOSE_MS);
        Log.info("Лифт " + elevatorId + " двери закрыты на этаже " + floor);
    }
}


