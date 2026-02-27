package com.crm.commerce.platform.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
@CompoundIndex(name = "entity_lookup", def = "{'entityType': 1, 'entityId': 1}")
public class AuditLog {

    @Id
    private String id;

    private String userId;

    private String userName;

    private String action;

    private String entityType;

    private String entityId;

    private Map<String, Object> details;

    private String ipAddress;

    @Indexed(direction = org.springframework.data.mongodb.core.index.IndexDirection.DESCENDING)
    @CreatedDate
    private LocalDateTime createdAt;
}
