package com.prashant.message_relay.controller;

import com.prashant.message_relay.model.DeliveryDocument;
import com.prashant.message_relay.model.DeliveryRecord;
import com.prashant.message_relay.model.NotificationEvent;
import com.prashant.message_relay.repository.DeliveryRecordRepository;
import com.prashant.message_relay.repository.DeliverySearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class DeliverySearchController {
    @GetMapping("/search")
    public ResponseEntity<Page<DeliveryDocument>> search(
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) DeliveryRecord.DeliveryStatus status,
            @RequestParam(required = false) NotificationEvent.Channel channel,
            @RequestParam(required = false) String clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String statusValue = status != null ? status.name() : null;
        String channelValue = channel != null ? channel.name() : null;

        if (clientId != null && status != null) {
            return ResponseEntity.ok(searchRepository.findByClientIdAndStatus(clientId, statusValue, pageable));
        }
        if (channel != null && status != null) {
            return ResponseEntity.ok(searchRepository.findByChannelAndStatus(channelValue, statusValue, pageable));
        }
        if (recipient != null) {
            return ResponseEntity.ok(searchRepository.findByRecipient(recipient, pageable));
        }
        if (clientId != null) {
            return ResponseEntity.ok(searchRepository.findByClientId(clientId, pageable));
        }
        if (status != null) {
            return ResponseEntity.ok(searchRepository.findByStatus(statusValue, pageable));
        }
        return ResponseEntity.ok(searchRepository.findAll(pageable));
    }


    private final DeliveryRecordRepository recordRepository;
    private final DeliverySearchRepository searchRepository;


    /**
     * Get full delivery record with status history from MongoDB.
     * GET /api/messages/{eventId}
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<DeliveryRecord> getByEventId(@PathVariable String eventId) {
        return recordRepository.findByEventId(eventId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DLQ stats endpoint.
     * GET /api/messages/stats/dlq
     */
    @GetMapping("/stats/dlq")
    public ResponseEntity<Map<String, Object>> dlqStats() {
        long dlqCount = recordRepository.countByStatus(DeliveryRecord.DeliveryStatus.DEAD_LETTERED);
        long sentCount = recordRepository.countByStatus(DeliveryRecord.DeliveryStatus.SENT);
        long pendingCount = recordRepository.countByStatus(DeliveryRecord.DeliveryStatus.PENDING);
        long total = recordRepository.count();

        double successRate = total > 0 ? (double) sentCount / total * 100 : 0;

        return ResponseEntity.ok(Map.of(
                "total", total,
                "sent", sentCount,
                "deadLettered", dlqCount,
                "pending", pendingCount,
                "successRatePct", String.format("%.2f", successRate)
        ));
    }
}
