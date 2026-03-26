package com.prashant.message_relay.repository;

import com.prashant.message_relay.model.DeliveryRecord;
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

    List<DeliveryRecord> findByRecipientAndCreatedAtAfter(String recipient, LocalDateTime after);

    List<DeliveryRecord> findByStatusAndAttemptCountLessThan(
            DeliveryRecord.DeliveryStatus status, int maxAttempts);

    @Query("{ 'status': 'DEAD_LETTERED', 'createdAt': { $gte: ?0 } }")
    List<DeliveryRecord> findRecentDeadLettered(LocalDateTime since);

    long countByStatus(DeliveryRecord.DeliveryStatus status);

    long countByClientIdAndStatusAndCreatedAtAfter(
            String clientId, DeliveryRecord.DeliveryStatus status, LocalDateTime after);
}
