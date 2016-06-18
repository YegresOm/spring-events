package eu.yegres.resources.domain.event;

public class InitializePoolEvent {

    private final int poolSize;

    public InitializePoolEvent(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getPoolSize() {
        return poolSize;
    }
}
