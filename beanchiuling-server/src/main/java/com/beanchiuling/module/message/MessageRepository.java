package com.beanchiuling.module.message;

import com.beanchiuling.module.message.document.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(
            String channelId, Pageable pageable);

    List<Message> findByChannelIdAndIdLessThanAndIsDeletedFalseOrderByCreatedAtDesc(
            String channelId, String beforeId, Pageable pageable);

    List<Message> findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
            String conversationId, Pageable pageable);

    Optional<Message> findByIdAndIsDeletedFalse(String id);

    long countByChannelIdAndIsDeletedFalse(String channelId);
}
