package com.shah_s.bakery_notification_service.dto;

import com.shah_s.bakery_notification_service.entity.Notification;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class NotificationResponse {

    // Getters and Setters
    private UUID id;
    private UUID userId;
    private String recipientEmail;
    private String recipientPhone;
    private String recipientName;
    private Notification.NotificationType type;
    private Notification.NotificationStatus status;
    private Notification.NotificationPriority priority;
    private UUID templateId;
    private UUID campaignId;
    private String title;
    private String content;
    private String htmlContent;
    private String subject;
    private String pushToken;
    private String snsEndpointArn;
    private String snsMessageId;
    private String platform;
    private String twilioMessageSid;
    private String emailMessageId;
    private Integer bounceCount;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime failedAt;
    private LocalDateTime openedAt;
    private LocalDateTime clickedAt;
    private String errorMessage;
    private String errorCode;
    private LocalDateTime lastErrorAt;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private String source;
    private String triggeredBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private Map<String, Object> metadata;
    private Map<String, Object> trackingData;

    // Derived fields
    private Boolean canRetry;
    private Boolean isExpired;
    private Boolean isPending;
    private Boolean isScheduled;
    private Boolean shouldSendNow;

    // Constructors
    public NotificationResponse() {}

    // Static factory method
    public static NotificationResponse from(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.id = notification.getId();
        response.userId = notification.getUserId();
        response.recipientEmail = notification.getRecipientEmail();
        response.recipientPhone = notification.getRecipientPhone();
        response.recipientName = notification.getRecipientName();
        response.type = notification.getType();
        response.status = notification.getStatus();
        response.priority = notification.getPriority();
        response.templateId = notification.getTemplateId();
        response.campaignId = notification.getCampaignId();
        response.title = notification.getTitle();
        response.content = notification.getContent();
        response.htmlContent = notification.getHtmlContent();
        response.subject = notification.getSubject();
        response.pushToken = notification.getPushToken();
        response.snsEndpointArn = notification.getSnsEndpointArn();
        response.snsMessageId = notification.getSnsMessageId();
        response.platform = notification.getPlatform();
        response.twilioMessageSid = notification.getTwilioMessageSid();
        response.emailMessageId = notification.getEmailMessageId();
        response.bounceCount = notification.getBounceCount();
        response.retryCount = notification.getRetryCount();
        response.maxRetryCount = notification.getMaxRetryCount();
        response.scheduledAt = notification.getScheduledAt();
        response.sentAt = notification.getSentAt();
        response.deliveredAt = notification.getDeliveredAt();
        response.failedAt = notification.getFailedAt();
        response.openedAt = notification.getOpenedAt();
        response.clickedAt = notification.getClickedAt();
        response.errorMessage = notification.getErrorMessage();
        response.errorCode = notification.getErrorCode();
        response.lastErrorAt = notification.getLastErrorAt();
        response.relatedEntityType = notification.getRelatedEntityType();
        response.relatedEntityId = notification.getRelatedEntityId();
        response.source = notification.getSource();
        response.triggeredBy = notification.getTriggeredBy();
        response.createdAt = notification.getCreatedAt();
        response.updatedAt = notification.getUpdatedAt();
        response.expiresAt = notification.getExpiresAt();

        // Parse JSON fields
        response.metadata = parseJsonToMap(notification.getMetadata());
        response.trackingData = parseJsonToMap(notification.getTrackingData());

        // Calculate derived fields
        response.canRetry = notification.canRetry();
        response.isExpired = notification.isExpired();
        response.isPending = notification.isPending();
        response.isScheduled = notification.isScheduled();
        response.shouldSendNow = notification.shouldSendNow();

        return response;
    }

    private static Map<String, Object> parseJsonToMap(String json) {
        // Simple implementation - in real app, use ObjectMapper
        return Map.of();
    }

}
