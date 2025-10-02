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
@Table(name = "device_tokens", indexes = {
    @Index(name = "idx_device_user", columnList = "user_id"),
    @Index(name = "idx_device_token", columnList = "device_token"),
    @Index(name = "idx_device_platform", columnList = "platform"),
    @Index(name = "idx_device_active", columnList = "is_active"),
    @Index(name = "idx_device_endpoint", columnList = "sns_endpoint_arn"),
    @Index(name = "idx_device_user_platform", columnList = "user_id, platform"),
    @Index(name = "idx_device_created", columnList = "created_at")
})
public class DeviceToken {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId; // NULL for guest tokens

    @Column(name = "device_token", nullable = false, length = 500)
    @NotBlank(message = "Device token is required")
    @Size(max = 500, message = "Device token must not exceed 500 characters")
    private String deviceToken;

    @Column(name = "sns_endpoint_arn", length = 500)
    @Size(max = 500, message = "SNS endpoint ARN must not exceed 500 characters")
    private String snsEndpointArn;

    @Column(nullable = false, length = 20)
    @NotBlank(message = "Platform is required")
    @Size(max = 20, message = "Platform must not exceed 20 characters")
    private String platform; // iOS, ANDROID, WEB

    @Column(name = "device_id", length = 255)
    @Size(max = 255, message = "Device ID must not exceed 255 characters")
    private String deviceId; // Unique device identifier

    @Column(name = "app_version", length = 20)
    @Size(max = 20, message = "App version must not exceed 20 characters")
    private String appVersion;

    @Column(name = "os_version", length = 20)
    @Size(max = 20, message = "OS version must not exceed 20 characters")
    private String osVersion;

    @Column(name = "device_model", length = 100)
    @Size(max = 100, message = "Device model must not exceed 100 characters")
    private String deviceModel;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_valid", nullable = false)
    private Boolean isValid = true;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled = true;

    // Topic subscriptions (JSON array of topic ARNs)
    @Column(name = "subscribed_topics", columnDefinition = "TEXT")
    private String subscribedTopics;

    // Error tracking
    @Column(name = "error_count", nullable = false)
    @Min(value = 0, message = "Error count cannot be negative")
    private Integer errorCount = 0;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "last_error_at")
    private LocalDateTime lastErrorAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "last_validated_at")
    private LocalDateTime lastValidatedAt;

    // Registration information
    @Column(name = "registered_from", length = 50)
    @Size(max = 50, message = "Registered from must not exceed 50 characters")
    private String registeredFrom; // WEB, MOBILE_APP, API

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    @Size(max = 45, message = "IP address must not exceed 45 characters")
    private String ipAddress; // IPv4 or IPv6

    // Geographic information
    @Column(name = "country", length = 2)
    @Size(max = 2, message = "Country must be 2 characters")
    private String country; // ISO country code

    @Column(name = "timezone", length = 50)
    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;

    // Metadata
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Constructors
    public DeviceToken() {}

    public DeviceToken(String deviceToken, String platform) {
        this.deviceToken = deviceToken;
        this.platform = platform;
        setDefaultExpiration();
    }

    public DeviceToken(UUID userId, String deviceToken, String platform) {
        this(deviceToken, platform);
        this.userId = userId;
    }

    // Business Logic Methods
    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
        this.isActive = true;
    }

    public void markAsValid() {
        this.isValid = true;
        this.errorCount = 0;
        this.lastErrorMessage = null;
        this.lastValidatedAt = LocalDateTime.now();
    }

    public void markAsInvalid(String errorMessage) {
        this.isValid = false;
        this.errorCount++;
        this.lastErrorMessage = errorMessage;
        this.lastErrorAt = LocalDateTime.now();

        // Deactivate after too many errors
        if (errorCount >= 5) {
            this.isActive = false;
        }
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void enableNotifications() {
        this.notificationEnabled = true;
    }

    public void disableNotifications() {
        this.notificationEnabled = false;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canReceiveNotifications() {
        return isActive && isValid && notificationEnabled && !isExpired();
    }

    public boolean isIOS() {
        return "iOS".equalsIgnoreCase(platform);
    }

    public boolean isAndroid() {
        return "ANDROID".equalsIgnoreCase(platform);
    }

    public boolean isWeb() {
        return "WEB".equalsIgnoreCase(platform);
    }

    private void setDefaultExpiration() {
        // Tokens expire after 90 days by default
        this.expiresAt = LocalDateTime.now().plusDays(90);
    }

    public void refreshExpiration() {
        setDefaultExpiration();
    }

    @Override
    public String toString() {
        return String.format("DeviceToken{id=%s, platform=%s, userId=%s, active=%s, valid=%s}",
                           id, platform, userId, isActive, isValid);
    }
}
