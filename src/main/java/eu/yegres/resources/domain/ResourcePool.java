package eu.yegres.resources.domain;

import eu.yegres.resources.domain.event.AvailableResourceEvent;
import eu.yegres.resources.domain.event.BookResourceEvent;
import eu.yegres.resources.domain.event.InitializePoolEvent;
import eu.yegres.resources.domain.event.ReleaseResourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import static eu.yegres.resources.domain.Resource.stub;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.generate;

@Service
@Transactional
public class ResourcePool {

    private static final Logger LOG = LoggerFactory.getLogger(ResourcePool.class);

    private Queue<Resource> resources = new ConcurrentLinkedDeque<>();

    @Autowired
    private ResourceRepository repository;

    @EventListener(ContextRefreshedEvent.class)
    public AvailableResourceEvent initializeQueue() {
        List<Resource> availableResources = repository.findAll().stream()
                .filter(Resource::isAvailable).collect(toList());
        resources.clear();
        resources.addAll(availableResources);

        LOG.info("Available resource pool size {}", resources.size());
        return new AvailableResourceEvent(!resources.isEmpty());
    }

    @EventListener
    public AvailableResourceEvent createPool(InitializePoolEvent event) {
        repository.deleteAll();
        repository.save(generate(Resource::new)
                .limit(event.getPoolSize()).collect(toList()));
        return initializeQueue();
    }

    @EventListener
    public AvailableResourceEvent bookResource(BookResourceEvent event) {
        if (!resources.isEmpty() && !isAlreadyBookedBy(event.getHolderId())) {
            LOG.info("Booking resource for {}", event.getHolderId());
            event.setBookedResourceId(bookResource(event.getHolderId()));
        } else {
            LOG.warn("Booking wasn't successful");
            event.setBookedResourceId(null);
        }
        return new AvailableResourceEvent(!resources.isEmpty());
    }

    private boolean isAlreadyBookedBy(String holderId) {
        //TODO will be better to write count query instead of read whole entity
        return repository.findByHolderId(holderId).isPresent();
    }

    private Long bookResource(String holderId) {
        Resource resource = resources.poll();
        if (!resource.isAvailable()) {
            throw new IllegalStateException("Queue contains unavailable resource");
        }
        LOG.info("Resource id:{} successfully booked by holder:{}", resource.getId(), holderId);
        return repository.save(resource.bookFor(holderId)).getId();
    }

    @EventListener
    public AvailableResourceEvent releaseResource(ReleaseResourceEvent event) {
        Resource resource = findResource(event.getHolderId());

        if (resource.isBookedBy(event.getHolderId())) {
            resources.add(resource.release());
            repository.save(resource);
            LOG.info("Resource id:{} released", resource.getId());
        }
        return new AvailableResourceEvent(!resources.isEmpty());
    }

    private Resource findResource(String holderId) {
        return repository.findByHolderId(holderId).orElse(stub());
    }
}
