package eu.yegres.resources.domain.event;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class BookResourceEvent {

    private final String holderId;

    private final CompletableFuture<Long> bookedResourceId = new CompletableFuture<>();

    public BookResourceEvent(String holderId) {
        this.holderId = holderId;
    }

    public String getHolderId() {
        return holderId;
    }

    public Future<Long> getBookedResourceId() {
        return bookedResourceId;
    }

    public void setBookedResourceId(Long bookedResourceId) {
        this.bookedResourceId.complete(bookedResourceId);
    }
}
