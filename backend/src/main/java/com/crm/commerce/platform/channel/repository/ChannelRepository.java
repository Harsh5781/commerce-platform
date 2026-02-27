package com.crm.commerce.platform.channel.repository;

import com.crm.commerce.platform.channel.model.Channel;
import com.crm.commerce.platform.channel.model.ChannelCode;
import com.crm.commerce.platform.channel.model.ChannelStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends MongoRepository<Channel, String> {

    Optional<Channel> findByCode(ChannelCode code);

    List<Channel> findByStatus(ChannelStatus status);

    boolean existsByCode(ChannelCode code);
}
