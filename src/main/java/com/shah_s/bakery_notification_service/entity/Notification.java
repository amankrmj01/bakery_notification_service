package com.shah_s.bakery_notification_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "user_id"),
        @Index(name = "idx_notification_type", columnList = "type"),
        @Index(name = "idx_notification_status", columnList = "status"),
        @Index(name = "idx_notification_priority", columnList = "priority"),
        @Index(name = "idx_notification_created", columnList = "created_at"),
        @Index(name = "idx_notification_scheduled", columnList = "scheduled_at"),
        @Index(name = "idx_notification_sent", columnList = "sent_at"),
        @Index(name = "idx_notification_template", columnList = "template_id"),
        @Index(name = "idx_notification_campaign", columnList = "campaign_id"),
        @Index(name = "idx_notification_user_type", columnList = "user_id, type"),
        @Index(name = "idx_notification_status_scheduled", columnList = "status, scheduled_at")
})
public class Notification {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId; // NULL for broadcast notifications

    @Column(name = "recipient_email", length = 255)
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String recipientEmail;

    @Column(name = "recipient_phone", length = 20)
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String recipientPhone;

    @Column(name = "recipient_name", length = 100)
    @Size(max = 100, message = "Recipient name must not exceed 100 characters")
    private String recipientName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(nullable = false, length = 500)
    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Column(columnDefinition = "TEXT")
    @NotBlank(message = "Content is required")
    private String content;

    @Column(name = "html_content", columnDefinition = "TEXT")
    private String htmlContent; // For email notifications

    @Column(name = "subject", length = 255)
    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject; // For email notifications

    // Push notification specific fields
    @Column(name = "push_token", length = 255)
    @Size(max = 255, message = "Push token must not exceed 255 characters")
    private String pushToken;

    @Column(name = "sns_endpoint_arn", length = 500)
    @Size(max = 500, message = "SNS endpoint ARN must not exceed 500 characters")
    private String snsEndpointArn;

    @Column(name = "sns_message_id", length = 255)
    @Size(max = 255, message = "SNS message ID must not exceed 255 characters")
    private String snsMessageId;

    @Column(name = "platform", length = 20)
    @Size(max = 20, message = "Platform must not exceed 20 characters")
    private String platform; // iOS, ANDROID, WEB

    // SMS specific fields
    @Column(name = "twilio_message_sid", length = 255)
    @Size(max = 255, message = "Twilio message SID must not exceed 255 characters")
    private String twilioMessageSid;

    // Email specific fields
    @Column(name = "email_message_id", length = 255)
    @Size(max = 255, message = "Email message ID must not exceed 255 characters")
    private String emailMessageId;

    @Column(name = "bounce_count", nullable = false)
    @Min(value = 0, message = "Bounce count cannot be negative")
    private Integer bounceCount = 0;

    @Column(name = "retry_count", nullable = false)
    @Min(value = 0, message = "Retry count cannot be negative")
    private Integer retryCount = 0;

    @Column(name = "max_retry_count", nullable = false)
    @Min(value = 0, message = "Max retry count cannot be negative")
    private Integer maxRetryCount = 3;

    // Scheduling
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;

    // Error tracking
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_code", length = 50)
    @Size(max = 50, message = "Error code must not exceed 50 characters")
    private String errorCode;

    @Column(name = "last_error_at")
    private LocalDateTime lastErrorAt;

    // Additional metadata
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data

    @Column(name = "tracking_data", columnDefinition = "TEXT")
    private String trackingData; // JSON string for tracking information

    // Reference to related entities
    @Column(name = "related_entity_type", length = 50)
    @Size(max = 50, message = "Related entity type must not exceed 50 characters")
    private String relatedEntityType; // ORDER, CART, USER, PRODUCT

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    // Source information
    @Column(name = "source", length = 50)
    @Size(max = 50, message = "Source must not exceed 50 characters")
    private String source; // WEB, MOBILE, API, SYSTEM

    @Column(name = "triggered_by", length = 100)
    @Size(max = 100, message = "Triggered by must not exceed 100 characters")
    private String triggeredBy; // EVENT, MANUAL, SCHEDULED

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Constructors
    public Notification() {}

    public Notification(NotificationType type, String title, String content) {
        this.type = type;
        this.title = title;
        this.content = content;
        setDefaultExpiration();
    }

    public Notification(UUID userId, NotificationType type, String title, String content) {
        this(type, title, content);
        this.userId = userId;
    }

    // Business Logic Methods
    public void markAsSent(String messageId) {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();

        // Set appropriate message ID based on type
        switch (type) {
            case EMAIL -> this.emailMessageId = messageId;
            case SMS -> this.twilioMessageSid = messageId;
            case PUSH -> this.snsMessageId = messageId;
        }
    }

    public void markAsDelivered() {
        this.status = NotificationStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage, String errorCode) {
        this.status = NotificationStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.lastErrorAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.retryCount++;
    }

    public void markAsBounced() {
        this.status = NotificationStatus.BOUNCED;
        this.bounceCount++;
        this.lastErrorAt = LocalDateTime.now();
    }

    public void markAsCancelled() {
        this.status = NotificationStatus.CANCELLED;
    }

    public void markAsOpened() {
        this.openedAt = LocalDateTime.now();
    }

    public void markAsClicked() {
        this.clickedAt = LocalDateTime.now();
    }

    public boolean canRetry() {
        return retryCount < maxRetryCount &&
                status == NotificationStatus.FAILED &&
                (expiresAt == null || LocalDateTime.now().isBefore(expiresAt));
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isPending() {
        return status == NotificationStatus.PENDING;
    }

    public boolean isScheduled() {
        return scheduledAt != null && LocalDateTime.now().isBefore(scheduledAt);
    }

    public boolean shouldSendNow() {
        return isPending() && !isScheduled() && !isExpired();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    private void setDefaultExpiration() {
        // Default expiration: 7 days for regular notifications, 1 day for urgent
        int days = priority == NotificationPriority.URGENT ? 1 : 7;
        this.expiresAt = LocalDateTime.now().plusDays(days);
    }

    // Enum definitions
    public enum NotificationType {
        EMAIL, SMS, PUSH, IN_APP
    }

    public enum NotificationStatus {
        PENDING,    // Notification created but not sent
        SENT,       // Notification sent to provider
        DELIVERED,  // Notification delivered to recipient
        FAILED,     // Notification failed to send
        BOUNCED,    // Notification bounced (invalid email/phone)
        CANCELLED   // Notification cancelled
    }

    public enum NotificationPriority {
        LOW,        // Marketing, promotional
        NORMAL,     // Standard notifications
        HIGH,       // Order updates, important info
        URGENT      // Security alerts, critical updates
    }

    @Override
    public String toString() {
        return String.format("Notification{id=%s, type=%s, status=%s, title='%s', userId=%s}",
                id, type, status, title, userId);
    }
}
