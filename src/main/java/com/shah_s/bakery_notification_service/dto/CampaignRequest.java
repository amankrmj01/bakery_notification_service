package com.shah_s.bakery_notification_service.dto;

import com.shah_s.bakery_notification_service.entity.NotificationCampaign;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Setter
@Getter
public class CampaignRequest {

    // Getters and Setters
    @NotBlank(message = "Campaign name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Campaign type is required")
    private NotificationCampaign.CampaignType campaignType;

    private UUID templateId;

    // Targeting criteria
    private Map<String, Object> targetAudience; // Criteria for targeting

    private List<UUID> targetUserIds; // Specific user IDs

    private List<String> targetSegments; // User segments

    // Scheduling
    private LocalDateTime scheduledStartAt;

    private LocalDateTime scheduledEndAt;

    // Campaign settings
    private Boolean isActive = true;

    private Boolean isRecurring = false;

    @Size(max = 100, message = "Recurrence pattern must not exceed 100 characters")
    private String recurrencePattern; // DAILY, WEEKLY, MONTHLY, CUSTOM

    @Min(value = 1, message = "Max recipients must be at least 1")
    private Integer maxRecipients;

    @Size(max = 20, message = "Priority must not exceed 20 characters")
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT

    // Budget and limits
    @DecimalMin(value = "0.00", message = "Budget limit cannot be negative")
    private BigDecimal budgetLimit;

    @DecimalMin(value = "0.0000", message = "Cost per notification cannot be negative")
    private BigDecimal costPerNotification;

    // A/B Testing
    private Boolean isAbTest = false;

    @DecimalMin(value = "0.00", message = "A/B test percentage cannot be negative")
    @DecimalMax(value = "100.00", message = "A/B test percentage cannot exceed 100")
    private BigDecimal abTestPercentage;

    @Size(max = 10, message = "A/B variant must not exceed 10 characters")
    private String abVariant; // A, B

    // Content variations
    private List<Map<String, Object>> contentVariations; // Content variations

    // Personalization
    private Map<String, Object> personalizationData; // Personalization rules

    // Metadata and tracking
    private List<String> tags; // Tags

    private Map<String, Object> metadata; // Additional data

    private Map<String, Object> trackingParams; // Tracking parameters

    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    // Constructors
    public CampaignRequest() {}

    public CampaignRequest(String name, NotificationCampaign.CampaignType campaignType) {
        this.name = name;
        this.campaignType = campaignType;
    }

}
