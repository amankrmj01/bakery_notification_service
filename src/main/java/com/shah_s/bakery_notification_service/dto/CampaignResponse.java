package com.shah_s.bakery_notification_service.dto;

import com.shah_s.bakery_notification_service.entity.NotificationCampaign;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {
    private UUID id;
    private String name;
    private String description;
    private NotificationCampaign.CampaignType campaignType;
    private NotificationCampaign.CampaignStatus status;
    private UUID templateId;
    private Map<String, Object> targetAudience;
    private List<UUID> targetUserIds;
    private List<String> targetSegments;
    private LocalDateTime scheduledStartAt;
    private LocalDateTime scheduledEndAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private Boolean isActive;
    private Boolean isRecurring;
    private String recurrencePattern;
    private Integer maxRecipients;
    private String priority;
    private BigDecimal budgetLimit;
    private BigDecimal costPerNotification;

    // Statistics
    private Integer totalRecipients;
    private Integer sentCount;
    private Integer deliveredCount;
    private Integer failedCount;
    private Integer openedCount;
    private Integer clickedCount;
    private Integer bouncedCount;
    private Integer unsubscribedCount;
    private BigDecimal totalCost;

    // A/B Testing
    private Boolean isAbTest;
    private BigDecimal abTestPercentage;
    private String abVariant;

    // Content and tracking
    private List<Map<String, Object>> contentVariations;
    private Map<String, Object> personalizationData;
    private List<String> tags;
    private Map<String, Object> metadata;
    private Map<String, Object> trackingParams;

    // Audit
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Calculated metrics
    private Double deliveryRate;
    private Double openRate;
    private Double clickRate;
    private Double bounceRate;
    private Double unsubscribeRate;

    // Derived fields
    private Boolean isScheduled;
    private Boolean canStart;
    private Boolean isRunning;
    private Boolean isCompleted;
    private Boolean isCancelled;

    // Static factory method
    public static CampaignResponse from(NotificationCampaign campaign) {
        CampaignResponse response = new CampaignResponse();
        response.id = campaign.getId();
        response.name = campaign.getName();
        response.description = campaign.getDescription();
        response.campaignType = campaign.getCampaignType();
        response.status = campaign.getStatus();
        response.templateId = campaign.getTemplateId();
        response.scheduledStartAt = campaign.getScheduledStartAt();
        response.scheduledEndAt = campaign.getScheduledEndAt();
        response.startedAt = campaign.getStartedAt();
        response.completedAt = campaign.getCompletedAt();
        response.cancelledAt = campaign.getCancelledAt();
        response.isActive = campaign.getIsActive();
        response.isRecurring = campaign.getIsRecurring();
        response.recurrencePattern = campaign.getRecurrencePattern();
        response.maxRecipients = campaign.getMaxRecipients();
        response.priority = campaign.getPriority();
        response.budgetLimit = campaign.getBudgetLimit();
        response.costPerNotification = campaign.getCostPerNotification();

        // Statistics
        response.totalRecipients = campaign.getTotalRecipients();
        response.sentCount = campaign.getSentCount();
        response.deliveredCount = campaign.getDeliveredCount();
        response.failedCount = campaign.getFailedCount();
        response.openedCount = campaign.getOpenedCount();
        response.clickedCount = campaign.getClickedCount();
        response.bouncedCount = campaign.getBouncedCount();
        response.unsubscribedCount = campaign.getUnsubscribedCount();
        response.totalCost = campaign.getTotalCost();

        // A/B Testing
        response.isAbTest = campaign.getIsAbTest();
        response.abTestPercentage = campaign.getAbTestPercentage();
        response.abVariant = campaign.getAbVariant();

        // Audit
        response.createdBy = campaign.getCreatedBy();
        response.updatedBy = campaign.getUpdatedBy();
        response.createdAt = campaign.getCreatedAt();
        response.updatedAt = campaign.getUpdatedAt();

        // Parse JSON fields
        response.targetAudience = parseJsonToMap(campaign.getTargetAudience());
        response.targetUserIds = parseJsonToUuidList(campaign.getTargetUserIds());
        response.targetSegments = parseJsonToStringList(campaign.getTargetSegments());
        response.contentVariations = parseJsonToMapList(campaign.getContentVariations());
        response.personalizationData = parseJsonToMap(campaign.getPersonalizationData());
        response.tags = parseJsonToStringList(campaign.getTags());
        response.metadata = parseJsonToMap(campaign.getMetadata());
        response.trackingParams = parseJsonToMap(campaign.getTrackingParams());

        // Calculate metrics
        response.deliveryRate = campaign.getDeliveryRate();
        response.openRate = campaign.getOpenRate();
        response.clickRate = campaign.getClickRate();
        response.bounceRate = campaign.getBounceRate();
        response.unsubscribeRate = campaign.getUnsubscribeRate();

        // Derived fields
        response.isScheduled = campaign.isScheduled();
        response.canStart = campaign.canStart();
        response.isRunning = campaign.isRunning();
        response.isCompleted = campaign.isCompleted();
        response.isCancelled = campaign.isCancelled();

        return response;
    }

    // Helper methods for JSON parsing
    private static Map<String, Object> parseJsonToMap(String json) {
        return Map.of(); // Simplified - use ObjectMapper in real implementation
    }

    private static List<UUID> parseJsonToUuidList(String json) {
        return List.of(); // Simplified - use ObjectMapper in real implementation
    }

    private static List<String> parseJsonToStringList(String json) {
        return List.of(); // Simplified - use ObjectMapper in real implementation
    }

    private static List<Map<String, Object>> parseJsonToMapList(String json) {
        return List.of(); // Simplified - use ObjectMapper in real implementation
    }
}
