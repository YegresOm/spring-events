package eu.yegres.resources.domain.event;

public class AvailableResourceEvent {

    private final boolean resourceAvailable;

    public AvailableResourceEvent(boolean resourceAvailable) {
        this.resourceAvailable = resourceAvailable;
    }


    public boolean isResourceAvailable() {
        return resourceAvailable;
    }
}
