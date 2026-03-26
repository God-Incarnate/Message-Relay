package com.prashant.message_relay.repository;

import com.prashant.message_relay.model.DeliveryDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliverySearchRepository extends ElasticsearchRepository<DeliveryDocument, String> {

    Page<DeliveryDocument> findByRecipient(String recipient, Pageable pageable);

    Page<DeliveryDocument> findByClientId(String clientId, Pageable pageable);

    Page<DeliveryDocument> findByStatus(String status, Pageable pageable);

    Page<DeliveryDocument> findByClientIdAndStatus(String clientId, String status, Pageable pageable);

    Page<DeliveryDocument> findByChannelAndStatus(String channel, String status, Pageable pageable);
}
