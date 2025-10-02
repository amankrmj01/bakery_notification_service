package com.shah_s.bakery_notification_service.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class DeviceRegistrationRequest {

    // Getters and Setters
    private UUID userId; // NULL for guest registration

    @NotBlank(message = "Device token is required")
    @Size(max = 500, message = "Device token must not exceed 500 characters")
    private String deviceToken;

    @NotBlank(message = "Platform is required")
    @Size(max = 20, message = "Platform must not exceed 20 characters")
    private String platform; // iOS, ANDROID, WEB

    @Size(max = 255, message = "Device ID must not exceed 255 characters")
    private String deviceId; // Unique device identifier

    @Size(max = 20, message = "App version must not exceed 20 characters")
    private String appVersion;

    @Size(max = 20, message = "OS version must not exceed 20 characters")
    private String osVersion;

    @Size(max = 100, message = "Device model must not exceed 100 characters")
    private String deviceModel;

    private Boolean notificationEnabled = true;

    // Topic subscriptions
    private List<String> topicSubscriptions;

    // Registration information
    @Size(max = 50, message = "Registered from must not exceed 50 characters")
    private String registeredFrom; // WEB, MOBILE_APP, API

    private String userAgent;

    @Size(max = 45, message = "IP address must not exceed 45 characters")
    private String ipAddress; // IPv4 or IPv6

    // Geographic information
    @Size(max = 2, message = "Country must be 2 characters")
    private String country; // ISO country code

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;

    // Additional metadata
    private Map<String, Object> metadata;

    // Constructors
    public DeviceRegistrationRequest() {}

    public DeviceRegistrationRequest(String deviceToken, String platform) {
        this.deviceToken = deviceToken;
        this.platform = platform;
    }

    public DeviceRegistrationRequest(UUID userId, String deviceToken, String platform) {
        this(deviceToken, platform);
        this.userId = userId;
    }

}
