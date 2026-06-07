package net.thesphynx.espritmarket.Common.Event;

import lombok.Getter;

@Getter
public class NotificationEvent {
    private final Long recipientId;
    private final String type;
    private final String title;
    private final String message;
    private final Long relatedEntityId;
    private final String relatedEntityType;

    public NotificationEvent(Long recipientId, String type, String title, String message,
                             Long relatedEntityId, String relatedEntityType) {
        this.recipientId = recipientId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedEntityId = relatedEntityId;
        this.relatedEntityType = relatedEntityType;
    }
}
