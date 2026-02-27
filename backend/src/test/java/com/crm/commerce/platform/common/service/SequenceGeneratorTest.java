package com.crm.commerce.platform.common.service;

import com.crm.commerce.platform.common.service.impl.SequenceGeneratorImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SequenceGeneratorTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private SequenceGeneratorImpl sequenceGenerator;

    @Test
    void nextValue_returnsIncrementedSequence() {
        Map<String, Object> result = Map.of("_id", "order_sequence", "seq", 42);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Map.class), eq("counters")))
                .thenReturn(result);

        long value = sequenceGenerator.nextValue("order_sequence");

        assertThat(value).isEqualTo(42L);
    }

    @Test
    void nextValue_withLongSeq_returnsLong() {
        Map<String, Object> result = Map.of("_id", "order_sequence", "seq", 100000L);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Map.class), eq("counters")))
                .thenReturn(result);

        long value = sequenceGenerator.nextValue("order_sequence");

        assertThat(value).isEqualTo(100000L);
    }

    @Test
    void nextValue_nullResult_throwsIllegalState() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Map.class), eq("counters")))
                .thenReturn(null);

        assertThatThrownBy(() -> sequenceGenerator.nextValue("order_sequence"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to generate sequence");
    }

    @Test
    void nextValue_nullSeqField_throwsIllegalState() {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("_id", "order_sequence");
        result.put("seq", null);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Map.class), eq("counters")))
                .thenReturn(result);

        assertThatThrownBy(() -> sequenceGenerator.nextValue("order_sequence"))
                .isInstanceOf(IllegalStateException.class);
    }
}
