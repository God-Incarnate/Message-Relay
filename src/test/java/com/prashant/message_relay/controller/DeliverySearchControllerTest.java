package com.prashant.message_relay.controller;

import com.prashant.message_relay.model.DeliveryDocument;
import com.prashant.message_relay.model.DeliveryRecord;
import com.prashant.message_relay.model.NotificationEvent;
import com.prashant.message_relay.repository.DeliveryRecordRepository;
import com.prashant.message_relay.repository.DeliverySearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliverySearchControllerTest {

    @Mock
    private DeliveryRecordRepository recordRepository;

    @Mock
    private DeliverySearchRepository searchRepository;

    @Test
    void shouldUseClientIdStatusBranchAndApplyDefaultPagination() {
        DeliverySearchController controller = new DeliverySearchController(recordRepository, searchRepository);
        Page<DeliveryDocument> expected = new PageImpl<>(List.of());

        when(searchRepository.findByClientIdAndStatus(eq("client-1"), eq(DeliveryRecord.DeliveryStatus.SENT.name()), any(Pageable.class)))
                .thenReturn(expected);

        ResponseEntity<Page<DeliveryDocument>> response = controller.search(
                null,
                DeliveryRecord.DeliveryStatus.SENT,
                NotificationEvent.Channel.SMS,
                "client-1",
                0,
                20
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(searchRepository).findByClientIdAndStatus(eq("client-1"), eq(DeliveryRecord.DeliveryStatus.SENT.name()), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals("createdAt: DESC", pageable.getSort().toString());
        assertEquals(expected, response.getBody());
    }
}


