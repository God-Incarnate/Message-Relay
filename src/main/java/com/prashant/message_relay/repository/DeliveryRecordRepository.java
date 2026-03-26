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


}
