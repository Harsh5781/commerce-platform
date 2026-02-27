package com.crm.commerce.platform.common.service.impl;

import com.crm.commerce.platform.common.service.SequenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SequenceGeneratorImpl implements SequenceGenerator {

    private static final String COLLECTION = "counters";

    private final MongoTemplate mongoTemplate;

    /**
     * Atomically increments the named sequence and returns the new value.
     * Uses MongoDB findAndModify with upsert — safe across restarts and concurrent access.
     */
    public long nextValue(String sequenceName) {
        Query query = new Query(Criteria.where("_id").is(sequenceName));
        Update update = new Update().inc("seq", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options()
                .returnNew(true)
                .upsert(true);

        Map result = mongoTemplate.findAndModify(query, update, options, Map.class, COLLECTION);
        if (result == null || result.get("seq") == null) {
            throw new IllegalStateException("Failed to generate sequence for: " + sequenceName);
        }

        Object seq = result.get("seq");
        if (seq instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Unexpected seq type: " + seq.getClass());
    }
}
