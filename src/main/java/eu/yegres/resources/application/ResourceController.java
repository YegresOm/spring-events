package eu.yegres.resources.application;

import eu.yegres.resources.domain.event.AvailableResourceEvent;
import eu.yegres.resources.domain.event.BookResourceEvent;
import eu.yegres.resources.domain.event.InitializePoolEvent;
import eu.yegres.resources.domain.event.ReleaseResourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class ResourceController {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceController.class);

    @Autowired
    private ApplicationEventPublisher publisher;

    private AtomicBoolean resourceAvailable = new AtomicBoolean();

    @RequestMapping(path = "/pool/{size}", method = GET)
    public ResponseEntity createPool(@PathVariable int size) {
        publisher.publishEvent(new InitializePoolEvent(size));
        return ok("POOL CREATED\n");
    }

    @RequestMapping(path = "/book", method = GET)
    public ResponseEntity bookResource(HttpServletRequest request) {
        if (!resourceAvailable.get()) {
            return ok("NOT AVAILABLE\n");
        }
        BookResourceEvent event = new BookResourceEvent(request.getRemoteHost());
        publisher.publishEvent(event);
        Long resourceId = extractResourceId(event);
        return resourceId != null ? ok(format("BOOKED RESOURCE ID: %s \n", resourceId))
                : ok("BOOKING WASN'T SUCCESSFUL\n");
    }

    private Long extractResourceId(BookResourceEvent event) {
        try {
            //TODO timeout value should not be hardcoded
            return event.getBookedResourceId().get(1, SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Exception during resource booking", e);
            publisher.publishEvent(new ReleaseResourceEvent(event.getHolderId()));
            return null;
        }
    }

    @RequestMapping(path = "/release", method = GET)
    public ResponseEntity releaseResource(HttpServletRequest request) {
        publisher.publishEvent(new ReleaseResourceEvent(request.getRemoteHost()));
        return ok("RELEASED\n");
    }

    @EventListener
    public void onAvailableResourceEvent(AvailableResourceEvent event) {
        resourceAvailable.compareAndSet(!event.isResourceAvailable(), event.isResourceAvailable());
    }
}
