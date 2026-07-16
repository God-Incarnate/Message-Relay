package com.prashant.message_relay.repository;

import com.prashant.message_relay.model.DeliveryRecord;
import com.prashant.message_relay.model.NotificationEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRecordRepository extends MongoRepository<DeliveryRecord, String> {

    Optional<DeliveryRecord> findByEventId(String eventId);

    List<DeliveryRecord> findByClientIdAndStatus(String clientId, DeliveryRecord.DeliveryStatus status);

    Page<DeliveryRecord> findByClientIdAndStatus(String clientId, DeliveryRecord.DeliveryStatus status, Pageable pageable);

    Page<DeliveryRecord> findByChannelAndStatus(NotificationEvent.Channel channel, DeliveryRecord.DeliveryStatus status, Pageable pageable);

    Page<DeliveryRecord> findByRecipient(String recipient, Pageable pageable);

    Page<DeliveryRecord> findByClientId(String clientId, Pageable pageable);

    Page<DeliveryRecord> findByStatus(DeliveryRecord.DeliveryStatus status, Pageable pageable);

    List<DeliveryRecord> findByRecipientAndCreatedAtAfter(String recipient, LocalDateTime after);

    List<DeliveryRecord> findByStatusAndAttemptCountLessThan(
            DeliveryRecord.DeliveryStatus status, int maxAttempts);

    @Query("{ 'status': 'DEAD_LETTERED', 'createdAt': { $gte: ?0 } }")
    List<DeliveryRecord> findRecentDeadLettered(LocalDateTime since);

    long countByStatus(DeliveryRecord.DeliveryStatus status);

    long countByClientIdAndStatusAndCreatedAtAfter(
            String clientId, DeliveryRecord.DeliveryStatus status, LocalDateTime after);
}
