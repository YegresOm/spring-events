package eu.yegres.resources.domain;


import javax.persistence.Entity;

@Entity
public class Resource extends PersistentObject {

    public static final String DEFAULT_HOLDER = "FREE";

    private String holderId;

    public Resource() {
        holderId = DEFAULT_HOLDER;
    }

    public Resource(String holderId) {
        this.holderId = holderId;
    }

    public boolean isAvailable() {
        return DEFAULT_HOLDER.equals(this.holderId);
    }

    public boolean isBookedBy(String holderId) {
        return this.holderId.equals(holderId);
    }

    public Resource bookFor(String holderId) {
        this.holderId = holderId;
        return this;
    }

    public Resource release() {
        this.holderId = DEFAULT_HOLDER;
        return this;
    }

    public static Resource stub() {
        return new Resource("INACTIVE_RESOURCE");
    }

    @Override
    public String toString() {
        return "Resource{" +
                "id='" + this.getId() + '\'' +
                "updated='" + this.getUpdated() + '\'' +
                "holderId='" + holderId + '\'' +
                '}';
    }
}
