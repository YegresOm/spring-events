package eu.yegres.resources.application;


import eu.yegres.resources.domain.event.AvailableResourceEvent;
import eu.yegres.resources.domain.event.BookResourceEvent;
import eu.yegres.resources.domain.event.InitializePoolEvent;
import eu.yegres.resources.domain.event.ReleaseResourceEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ResourceControllerTest {

    @InjectMocks
    private ResourceController controller;

    @Mock
    private ApplicationEventPublisher publisher;

    @Test
    public void shouldPublishCorrectBookingEvenAndExtractResourceId() throws ExecutionException, InterruptedException {
        //given
        long bookedResourceId = 123L;
        ArgumentCaptor<BookResourceEvent> captor = forClass(BookResourceEvent.class);
        controller.onAvailableResourceEvent(new AvailableResourceEvent(true));

        HttpServletRequest request = mock(HttpServletRequest.class);
        String holderId = "holderId";
        given(request.getRemoteHost()).willReturn(holderId);

        //when
        //Since events are async we have to use different thread to have possibility set result
        newSingleThreadExecutor().submit(() -> controller.bookResource(request));

        //then
        verify(publisher).publishEvent(captor.capture());

        //We have to set value or TimeoutException will be thrown
        captor.getValue().setBookedResourceId(bookedResourceId);

        assertThat(captor.getValue().getHolderId()).isEqualTo(holderId);
        assertThat(captor.getValue().getBookedResourceId().isDone()).isTrue();
        assertThat(captor.getValue().getBookedResourceId().get()).isEqualTo(bookedResourceId);
    }

    @Test
    public void shouldPublishReleaseResourceEventIfBookedResourceIdWasNotSetted() {
        //given
        controller.onAvailableResourceEvent(new AvailableResourceEvent(true));
        ArgumentCaptor<Object> captor = forClass(Object.class);

        HttpServletRequest request = mock(HttpServletRequest.class);
        String holderId = "holderId";
        given(request.getRemoteHost()).willReturn(holderId);

        //when
        controller.bookResource(request);

        //then
        verify(publisher, times(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().get(0)).isInstanceOf(BookResourceEvent.class);
        assertThat(captor.getAllValues().get(1)).isInstanceOf(ReleaseResourceEvent.class);

        ReleaseResourceEvent event = (ReleaseResourceEvent) captor.getAllValues().get(1);
        assertThat(event.getHolderId()).isEqualTo(holderId);
    }

    @Test
    public void shouldNotPublishAnyEventIfResourceIsNotAvailable() {
        //given
        controller.onAvailableResourceEvent(new AvailableResourceEvent(false));

        //when
        controller.bookResource(mock(HttpServletRequest.class));

        //then
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    public void shouldPublishCorrectReleaseResourceEvent() {
        //given
        ArgumentCaptor<ReleaseResourceEvent> captor = forClass(ReleaseResourceEvent.class);

        HttpServletRequest request = mock(HttpServletRequest.class);
        String holderId = "holderId";
        given(request.getRemoteHost()).willReturn(holderId);

        //when
        controller.releaseResource(request);

        //then
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getHolderId()).isEqualTo(holderId);
    }

    @Test
    public void shouldPublishCorrectPoolCreationEvent() {
        //given
        ArgumentCaptor<InitializePoolEvent> captor = forClass(InitializePoolEvent.class);
        int poolSize = 10;

        //when
        controller.createPool(poolSize);

        //then
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getPoolSize()).isEqualTo(poolSize);
    }
}
