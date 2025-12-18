package util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Log {
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    public static void info(String s) {
        synchronized (Log.class) {
            System.out.println("[" + LocalTime.now().format(F) + "] " + s);
        }
    }
}

