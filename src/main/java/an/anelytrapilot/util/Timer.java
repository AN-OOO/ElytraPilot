package an.anelytrapilot.util;

public class Timer {
    private long time;

    public Timer() {
        reset();
    }

    public boolean passedMs(long ms) {
        return getMs(System.nanoTime() - time) >= ms;
    }

    public boolean every(long ms) {
        boolean passed = getMs(System.nanoTime() - time) >= ms;
        if (passed)
            reset();
        return passed;
    }

    public void reset() {
        this.time = System.nanoTime();
    }

    public long getMs(long time) {
        return time / 1000000L;
    }
}