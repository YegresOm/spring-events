package eu.yegres.resources.domain.event;

public class ReleaseResourceEvent {

    private final String holderId;

    public ReleaseResourceEvent(String holderId) {
        this.holderId = holderId;
    }

    public String getHolderId() {
        return holderId;
    }
}
