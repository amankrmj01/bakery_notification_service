package com.shah_s.bakery_notification_service.dto;

import com.shah_s.bakery_notification_service.entity.DeviceToken;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class DeviceTokenResponse {

    // Getters and Setters
    private UUID id;
    private UUID userId;
    private String deviceToken;
    private String snsEndpointArn;
    private String platform;
    private String deviceId;
    private String appVersion;
    private String osVersion;
    private String deviceModel;
    private Boolean isActive;
    private Boolean isValid;
    private Boolean notificationEnabled;
    private List<String> subscribedTopics;
    private Integer errorCount;
    private String lastErrorMessage;
    private LocalDateTime lastErrorAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime lastValidatedAt;
    private String registeredFrom;
    private String userAgent;
    private String ipAddress;
    private String country;
    private String timezone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private Map<String, Object> metadata;

    // Derived fields
    private Boolean isExpired;
    private Boolean canReceiveNotifications;

    // Constructors
    public DeviceTokenResponse() {}

    // Static factory method
    public static DeviceTokenResponse from(DeviceToken deviceToken) {
        DeviceTokenResponse response = new DeviceTokenResponse();
        response.id = deviceToken.getId();
        response.userId = deviceToken.getUserId();
        response.deviceToken = maskToken(deviceToken.getDeviceToken()); // Mask for security
        response.snsEndpointArn = deviceToken.getSnsEndpointArn();
        response.platform = deviceToken.getPlatform();
        response.deviceId = deviceToken.getDeviceId();
        response.appVersion = deviceToken.getAppVersion();
        response.osVersion = deviceToken.getOsVersion();
        response.deviceModel = deviceToken.getDeviceModel();
        response.isActive = deviceToken.getIsActive();
        response.isValid = deviceToken.getIsValid();
        response.notificationEnabled = deviceToken.getNotificationEnabled();
        response.errorCount = deviceToken.getErrorCount();
        response.lastErrorMessage = deviceToken.getLastErrorMessage();
        response.lastErrorAt = deviceToken.getLastErrorAt();
        response.lastUsedAt = deviceToken.getLastUsedAt();
        response.lastValidatedAt = deviceToken.getLastValidatedAt();
        response.registeredFrom = deviceToken.getRegisteredFrom();
        response.userAgent = deviceToken.getUserAgent();
        response.ipAddress = deviceToken.getIpAddress();
        response.country = deviceToken.getCountry();
        response.timezone = deviceToken.getTimezone();
        response.createdAt = deviceToken.getCreatedAt();
        response.updatedAt = deviceToken.getUpdatedAt();
        response.expiresAt = deviceToken.getExpiresAt();

        // Parse JSON fields
        response.subscribedTopics = parseJsonToList(deviceToken.getSubscribedTopics());
        response.metadata = parseJsonToMap(deviceToken.getMetadata());

        // Calculate derived fields
        response.isExpired = deviceToken.isExpired();
        response.canReceiveNotifications = deviceToken.canReceiveNotifications();

        return response;
    }

    private static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    private static List<String> parseJsonToList(String json) {
        // Simple implementation - in real app, use ObjectMapper
        return List.of();
    }

    private static Map<String, Object> parseJsonToMap(String json) {
        // Simple implementation - in real app, use ObjectMapper
        return Map.of();
    }

}
