package goldminer;

import util.FP;

import java.util.Calendar;

public class Main {
    public static void main(String[] argv) {
        int playerID = -1;
        switch (argv.length) {
            case 2:
                playerID = 0;
                break;
            case 3:
                playerID = 1;
                break;
            default:
                System.err.println("Wrong number of arguments!");
                System.exit(0);
                return;
        }
        int FPS = Integer.valueOf(argv[argv.length - 1]);
        GUI gui = new GUI(playerID);
        gui.startTimerTask(FPS);
        gui.beginWelcomeScreen();
        FP.liftExp(() -> Thread.sleep(5000)).run();
        gui.beginWaitingConnectionScreen();
        if (playerID == 0) {
            gui.connection = new Connections.TCPServer(
                    Integer.valueOf(argv[0]),
                    State::move,
                    () -> {
                        State.start();
                        gui.beginGameScreen();
                    },
                    () -> {
                        State.init();
                        State.randomInit();
                        return State.getSnapshot().dumpEntities();
                    },
                    null,
                    State::pause,
                    State::resume,
                    () -> {
                        gui.beginCountDownScreen();
                        FP.liftExp(() -> Thread.sleep(3000)).run();
                    },
                    () -> {
                        long realTime = Calendar.getInstance().getTimeInMillis();
                        long time = State.getSnapshot().getTime();
                        synchronized (gui) {
                            if (gui.lastSpaceDownTime + 400 < realTime) {
                                gui.lastSpaceDownTime = realTime;
                                if (gui.isPaused) {
                                    gui.connection.sendResume();
                                    State.resume();
                                } else {
                                    gui.connection.sendPause(time + 300);
                                    State.pause(time + 300);
                                }
                                gui.isPaused = !gui.isPaused;
                            }
                        }
                    }
            );
        } else {
            gui.connection = new Connections.TCPClient(
                    argv[0],
                    Integer.valueOf(argv[1]),
                    State::move,
                    () -> {
                        State.start();
                        gui.beginGameScreen();
                    },
                    null,
                    s -> {
                        State.init();
                        State.loadEntities(s.split(","));
                    },
                    State::pause,
                    State::resume,
                    () -> {
                        gui.beginCountDownScreen();
                        FP.liftExp(() -> Thread.sleep(3000)).run();
                    },
                    null
            );
        }
    }
}
