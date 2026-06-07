package net.thesphynx.espritmarket.Common.Event;

import lombok.Getter;

@Getter
public class StatusTransitionEvent {
    private final Long entityId;
    private final String entityType;
    private final String fromStatus;
    private final String toStatus;
    private final Long changedBy;

    public StatusTransitionEvent(Long entityId, String entityType, String fromStatus, String toStatus, Long changedBy) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
    }
}
