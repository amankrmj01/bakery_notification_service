package com.shah_s.bakery_notification_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "notification_campaigns", indexes = {
        @Index(name = "idx_campaign_status", columnList = "status"),
        @Index(name = "idx_campaign_type", columnList = "campaign_type"),
        @Index(name = "idx_campaign_scheduled", columnList = "scheduled_start_at"),
        @Index(name = "idx_campaign_created", columnList = "created_at"),
        @Index(name = "idx_campaign_active", columnList = "is_active")
})
public class NotificationCampaign {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "Campaign name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Column(length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_type", nullable = false)
    private CampaignType campaignType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "template_id")
    private UUID templateId;

    // Targeting criteria
    @Column(name = "target_audience", columnDefinition = "TEXT")
    private String targetAudience; // JSON criteria for targeting

    @Column(name = "target_user_ids", columnDefinition = "TEXT")
    private String targetUserIds; // JSON array of specific user IDs

    @Column(name = "target_segments", columnDefinition = "TEXT")
    private String targetSegments; // JSON array of user segments

    // Scheduling
    @Column(name = "scheduled_start_at")
    private LocalDateTime scheduledStartAt;

    @Column(name = "scheduled_end_at")
    private LocalDateTime scheduledEndAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // Campaign settings
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = false;

    @Column(name = "recurrence_pattern", length = 100)
    @Size(max = 100, message = "Recurrence pattern must not exceed 100 characters")
    private String recurrencePattern; // DAILY, WEEKLY, MONTHLY, CUSTOM

    @Column(name = "max_recipients")
    @Min(value = 1, message = "Max recipients must be at least 1")
    private Integer maxRecipients;

    @Column(name = "priority", length = 20)
    @Size(max = 20, message = "Priority must not exceed 20 characters")
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT

    // Budget and limits
    @Column(name = "budget_limit", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Budget limit cannot be negative")
    private BigDecimal budgetLimit;

    @Column(name = "cost_per_notification", precision = 6, scale = 4)
    @DecimalMin(value = "0.0000", message = "Cost per notification cannot be negative")
    private BigDecimal costPerNotification;

    // Statistics
    @Column(name = "total_recipients", nullable = false)
    @Min(value = 0, message = "Total recipients cannot be negative")
    private Integer totalRecipients = 0;

    @Column(name = "sent_count", nullable = false)
    @Min(value = 0, message = "Sent count cannot be negative")
    private Integer sentCount = 0;

    @Column(name = "delivered_count", nullable = false)
    @Min(value = 0, message = "Delivered count cannot be negative")
    private Integer deliveredCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Min(value = 0, message = "Failed count cannot be negative")
    private Integer failedCount = 0;

    @Column(name = "opened_count", nullable = false)
    @Min(value = 0, message = "Opened count cannot be negative")
    private Integer openedCount = 0;

    @Column(name = "clicked_count", nullable = false)
    @Min(value = 0, message = "Clicked count cannot be negative")
    private Integer clickedCount = 0;

    @Column(name = "bounced_count", nullable = false)
    @Min(value = 0, message = "Bounced count cannot be negative")
    private Integer bouncedCount = 0;

    @Column(name = "unsubscribed_count", nullable = false)
    @Min(value = 0, message = "Unsubscribed count cannot be negative")
    private Integer unsubscribedCount = 0;

    // Cost tracking
    @Column(name = "total_cost", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Total cost cannot be negative")
    private BigDecimal totalCost = BigDecimal.ZERO;

    // A/B Testing
    @Column(name = "is_ab_test", nullable = false)
    private Boolean isAbTest = false;

    @Column(name = "ab_test_percentage", precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "A/B test percentage cannot be negative")
    @DecimalMax(value = "100.00", message = "A/B test percentage cannot exceed 100")
    private BigDecimal abTestPercentage;

    @Column(name = "ab_variant", length = 10)
    @Size(max = 10, message = "A/B variant must not exceed 10 characters")
    private String abVariant; // A, B

    // Content variations
    @Column(name = "content_variations", columnDefinition = "TEXT")
    private String contentVariations; // JSON array of content variations

    // Personalization
    @Column(name = "personalization_data", columnDefinition = "TEXT")
    private String personalizationData; // JSON object with personalization rules

    // Metadata and tracking
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON array of tags

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data

    @Column(name = "tracking_params", columnDefinition = "TEXT")
    private String trackingParams; // JSON object with tracking parameters

    // Campaign creator
    @Column(name = "created_by", length = 100)
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    @Size(max = 100, message = "Updated by must not exceed 100 characters")
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationship to notifications
    @OneToMany(mappedBy = "campaignId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications = new ArrayList<>();

    // Constructors
    public NotificationCampaign() {}

    public NotificationCampaign(String name, CampaignType campaignType) {
        this.name = name;
        this.campaignType = campaignType;
    }

    // Business Logic Methods
    public void start() {
        this.status = CampaignStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = CampaignStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = CampaignStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public void pause() {
        this.status = CampaignStatus.PAUSED;
    }

    public void resume() {
        this.status = CampaignStatus.RUNNING;
    }

    public void incrementSentCount() {
        this.sentCount++;
    }

    public void incrementDeliveredCount() {
        this.deliveredCount++;
    }

    public void incrementFailedCount() {
        this.failedCount++;
    }

    public void incrementOpenedCount() {
        this.openedCount++;
    }

    public void incrementClickedCount() {
        this.clickedCount++;
    }

    public void incrementBouncedCount() {
        this.bouncedCount++;
    }

    public void incrementUnsubscribedCount() {
        this.unsubscribedCount++;
    }

    public void addToCost(BigDecimal cost) {
        this.totalCost = this.totalCost.add(cost);
    }

    // Calculated fields
    public Double getDeliveryRate() {
        return sentCount > 0 ? (deliveredCount.doubleValue() / sentCount.doubleValue()) * 100 : 0.0;
    }

    public Double getOpenRate() {
        return deliveredCount > 0 ? (openedCount.doubleValue() / deliveredCount.doubleValue()) * 100 : 0.0;
    }

    public Double getClickRate() {
        return deliveredCount > 0 ? (clickedCount.doubleValue() / deliveredCount.doubleValue()) * 100 : 0.0;
    }

    public Double getBounceRate() {
        return sentCount > 0 ? (bouncedCount.doubleValue() / sentCount.doubleValue()) * 100 : 0.0;
    }

    public Double getUnsubscribeRate() {
        return deliveredCount > 0 ? (unsubscribedCount.doubleValue() / deliveredCount.doubleValue()) * 100 : 0.0;
    }

    public boolean isScheduled() {
        return scheduledStartAt != null && LocalDateTime.now().isBefore(scheduledStartAt);
    }

    public boolean canStart() {
        return status == CampaignStatus.DRAFT || status == CampaignStatus.SCHEDULED;
    }

    public boolean isRunning() {
        return status == CampaignStatus.RUNNING;
    }

    public boolean isCompleted() {
        return status == CampaignStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == CampaignStatus.CANCELLED;
    }

    public boolean withinBudget(BigDecimal additionalCost) {
        return budgetLimit == null || totalCost.add(additionalCost).compareTo(budgetLimit) <= 0;
    }

    // Enums
    public enum CampaignType {
        EMAIL_MARKETING,        // Email marketing campaign
        SMS_MARKETING,          // SMS marketing campaign
        PUSH_MARKETING,         // Push notification marketing
        CART_ABANDONMENT,       // Cart abandonment recovery
        ORDER_FOLLOW_UP,        // Post-order follow-up
        WELCOME_SERIES,         // Welcome new users
        RE_ENGAGEMENT,          // Re-engage inactive users
        BIRTHDAY_CAMPAIGN,      // Birthday special offers
        LOYALTY_PROGRAM,        // Loyalty program updates
        PRODUCT_LAUNCH,         // New product announcements
        SEASONAL_PROMOTION,     // Seasonal promotions
        FEEDBACK_REQUEST,       // Request reviews/feedback
        NEWSLETTER,             // Regular newsletter
        SYSTEM_MAINTENANCE      // System maintenance notifications
    }

    public enum CampaignStatus {
        DRAFT,          // Campaign is being created
        SCHEDULED,      // Campaign is scheduled to run
        RUNNING,        // Campaign is currently running
        PAUSED,         // Campaign is paused
        COMPLETED,      // Campaign has completed successfully
        CANCELLED,      // Campaign was cancelled
        FAILED          // Campaign failed to run
    }

    @Override
    public String toString() {
        return String.format("NotificationCampaign{id=%s, name='%s', type=%s, status=%s, sent=%d}",
                id, name, campaignType, status, sentCount);
    }
}
