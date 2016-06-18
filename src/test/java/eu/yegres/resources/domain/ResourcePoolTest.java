package eu.yegres.resources.domain;

import eu.yegres.resources.domain.event.AvailableResourceEvent;
import eu.yegres.resources.domain.event.BookResourceEvent;
import eu.yegres.resources.domain.event.InitializePoolEvent;
import eu.yegres.resources.domain.event.ReleaseResourceEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.generate;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ResourcePoolTest {

    @InjectMocks
    private ResourcePool pool;

    @Mock
    private ResourceRepository repository;

    @Mock
    private BookResourceEvent bookResourceEvent;

    @Mock
    private Resource resource;

    @Captor
    private ArgumentCaptor<Resource> resourceCaptor;

    @Captor
    ArgumentCaptor<List<Resource>> resourceListCaptor;

    @Test
    public void shouldInitializeQueueAndReturnResourceAvailableEven() {
        //given
        given(repository.findAll()).willReturn(singletonList(new Resource()));

        //when
        AvailableResourceEvent result = pool.initializeQueue();

        //then
        assertThat(result.isResourceAvailable()).isTrue();
    }

    @Test
    public void shouldReturnResourceIsNotAvailable() {
        //given
        given(repository.findAll()).willReturn(singletonList(Resource.stub()));

        //when
        AvailableResourceEvent result = pool.initializeQueue();

        //then
        assertThat(result.isResourceAvailable()).isFalse();
    }

    @Test
    public void shouldReinitializeQueueAndReturnResourceIsNotAvailable() {
        //given
        given(repository.findAll())
                .willReturn(singletonList(Resource.stub()))
                .willReturn(singletonList(new Resource()));

        pool.initializeQueue();

        //when
        AvailableResourceEvent result = pool.initializeQueue();

        //then
        assertThat(result.isResourceAvailable()).isTrue();
    }

    @Test
    public void shouldCreatePoolAndReturnResourceAvailableEvent() {
        //given
        given(repository.findAll()).willReturn(singletonList(new Resource()));
        int poolSize = 10;

        //when
        AvailableResourceEvent result = this.pool.createPool(new InitializePoolEvent(poolSize));

        //then
        verify(repository).deleteAll();
        verify(repository).save(resourceListCaptor.capture());
        assertThat(resourceListCaptor.getValue()).hasSize(poolSize);
        assertThat(result.isResourceAvailable()).isTrue();
    }

    @Test
    public void shouldBookResourceSetResourceIdAndReturnResourceIsNotAvailable() {
        //given
        String holderId = "holder";
        Long resourceId = 123L;

        given(resource.getId()).willReturn(resourceId);
        given(bookResourceEvent.getHolderId()).willReturn(holderId);
        given(repository.findByHolderId(holderId)).willReturn(empty());
        given(repository.save(any(Resource.class))).willReturn(resource);

        //when
        initializeQueueWithSize(1);
        AvailableResourceEvent result = pool.bookResource(bookResourceEvent);

        //then
        assertThat(result.isResourceAvailable()).isFalse();
        verify(repository).save(resourceCaptor.capture());
        assertThat(resourceCaptor.getValue().isBookedBy(holderId)).isTrue();
        verify(bookResourceEvent).setBookedResourceId(resourceId);
    }

    @Test
    public void shouldNotBookResourceIfQueueIsEmpty() {
        //when
        initializeQueueWithSize(0);
        AvailableResourceEvent result = pool.bookResource(bookResourceEvent);

        //then
        verify(bookResourceEvent).setBookedResourceId(null);
        assertThat(result.isResourceAvailable()).isFalse();
    }

    @Test
    public void shouldNotBookResourceIfHolderHaveBookedResource() {
        //given
        String holderId = "holder";
        given(bookResourceEvent.getHolderId()).willReturn(holderId);
        given(repository.findByHolderId(holderId)).willReturn(of(resource));

        //when
        initializeQueueWithSize(1);
        AvailableResourceEvent result = pool.bookResource(bookResourceEvent);

        //then
        verify(bookResourceEvent).setBookedResourceId(null);
        assertThat(result.isResourceAvailable()).isTrue();
    }

    @Test
    public void shouldReleaseResource() {
        //given
        String holderId = "holder";
        given(repository.findByHolderId(holderId)).willReturn(of(resource));
        given(resource.isBookedBy(holderId)).willReturn(true);
        given(resource.release()).willReturn(resource);

        //when
        AvailableResourceEvent result = pool.releaseResource(new ReleaseResourceEvent(holderId));

        //then
        verify(repository).save(resource);
        assertThat(result.isResourceAvailable()).isTrue();

    }

    @Test
    public void shouldNotReleaseResourceIfWasNotBookedByHolder() {
        //given
        String holderId = "holder";
        given(repository.findByHolderId(holderId)).willReturn(of(resource));
        given(resource.isBookedBy(holderId)).willReturn(false);

        //when
        AvailableResourceEvent result = pool.releaseResource(new ReleaseResourceEvent(holderId));

        //then
        verify(repository, never()).save(any(Resource.class));
        assertThat(result.isResourceAvailable()).isFalse();

    }


    private void initializeQueueWithSize(int size) {
        given(repository.findAll()).willReturn(generate(Resource::new)
                .limit(size).collect(toList()));
        pool.initializeQueue();
    }

}
