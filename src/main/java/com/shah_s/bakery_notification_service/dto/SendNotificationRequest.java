package com.shah_s.bakery_notification_service.dto;

import com.shah_s.bakery_notification_service.entity.Notification;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class SendNotificationRequest {

    // Getters and Setters
    private UUID userId; // NULL for broadcast notifications

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String recipientEmail;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String recipientPhone;

    @Size(max = 100, message = "Recipient name must not exceed 100 characters")
    private String recipientName;

    @NotNull(message = "Notification type is required")
    private Notification.NotificationType type;

    @NotNull(message = "Priority is required")
    private Notification.NotificationPriority priority = Notification.NotificationPriority.NORMAL;

    private UUID templateId;

    private UUID campaignId;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private String htmlContent; // For email notifications

    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject; // For email notifications

    // Push notification specific fields
    @Size(max = 255, message = "Push token must not exceed 255 characters")
    private String pushToken;

    @Size(max = 20, message = "Platform must not exceed 20 characters")
    private String platform; // iOS, ANDROID, WEB

    // Scheduling
    private LocalDateTime scheduledAt;

    private LocalDateTime expiresAt;

    @Min(value = 1, message = "Max retry count must be at least 1")
    @Max(value = 10, message = "Max retry count cannot exceed 10")
    private Integer maxRetryCount = 3;

    // Reference to related entities
    @Size(max = 50, message = "Related entity type must not exceed 50 characters")
    private String relatedEntityType; // ORDER, CART, USER, PRODUCT

    private UUID relatedEntityId;

    @Size(max = 50, message = "Source must not exceed 50 characters")
    private String source; // WEB, MOBILE, API, SYSTEM

    @Size(max = 100, message = "Triggered by must not exceed 100 characters")
    private String triggeredBy; // EVENT, MANUAL, SCHEDULED

    // Template variables (for dynamic content)
    private Map<String, Object> templateVariables;

    // Push notification data
    private Map<String, Object> pushData;

    // Additional metadata
    private Map<String, Object> metadata;

    // Tracking parameters
    private Map<String, Object> trackingParams;

    // Constructors
    public SendNotificationRequest() {}

    public SendNotificationRequest(Notification.NotificationType type, String title, String content) {
        this.type = type;
        this.title = title;
        this.content = content;
    }

    // Static factory methods for common notification types
    public static SendNotificationRequest email(String recipientEmail, String subject, String title, String content) {
        SendNotificationRequest request = new SendNotificationRequest(Notification.NotificationType.EMAIL, title, content);
        request.setRecipientEmail(recipientEmail);
        request.setSubject(subject);
        return request;
    }

    public static SendNotificationRequest sms(String recipientPhone, String content) {
        SendNotificationRequest request = new SendNotificationRequest(Notification.NotificationType.SMS, "SMS", content);
        request.setRecipientPhone(recipientPhone);
        return request;
    }

    public static SendNotificationRequest push(String pushToken, String platform, String title, String content) {
        SendNotificationRequest request = new SendNotificationRequest(Notification.NotificationType.PUSH, title, content);
        request.setPushToken(pushToken);
        request.setPlatform(platform);
        return request;
    }

}
