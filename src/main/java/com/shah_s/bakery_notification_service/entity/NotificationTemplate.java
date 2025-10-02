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
@Table(name = "notification_templates", indexes = {
    @Index(name = "idx_template_type", columnList = "template_type"),
    @Index(name = "idx_template_name", columnList = "name"),
    @Index(name = "idx_template_active", columnList = "is_active"),
    @Index(name = "idx_template_created", columnList = "created_at"),
    @Index(name = "idx_template_type_active", columnList = "template_type, is_active")
})
public class NotificationTemplate {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    @NotBlank(message = "Template name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false)
    private TemplateType templateType;

    @Column(length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Column(name = "subject_template", length = 255)
    @Size(max = 255, message = "Subject template must not exceed 255 characters")
    private String subjectTemplate; // For email templates

    @Column(name = "title_template", length = 500)
    @Size(max = 500, message = "Title template must not exceed 500 characters")
    private String titleTemplate;

    @Column(name = "content_template", columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Content template is required")
    private String contentTemplate;

    @Column(name = "html_template", columnDefinition = "TEXT")
    private String htmlTemplate; // For email HTML content

    @Column(name = "sms_template", columnDefinition = "TEXT")
    private String smsTemplate; // For SMS content

    @Column(name = "push_template", columnDefinition = "TEXT")
    private String pushTemplate; // For push notification content

    @Column(name = "variables", columnDefinition = "TEXT")
    private String variables; // JSON array of variable names

    @Column(name = "sample_data", columnDefinition = "TEXT")
    private String sampleData; // JSON object with sample variable values

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "version", nullable = false)
    @Min(value = 1, message = "Version must be at least 1")
    private Integer version = 1;

    @Column(name = "language", length = 10)
    @Size(max = 10, message = "Language must not exceed 10 characters")
    private String language = "en"; // ISO language code

    @Column(name = "category", length = 50)
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category; // TRANSACTIONAL, MARKETING, SYSTEM

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON array of tags

    // Usage statistics
    @Column(name = "usage_count", nullable = false)
    @Min(value = 0, message = "Usage count cannot be negative")
    private Long usageCount = 0L;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    // Template metadata
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

    // Constructors
    public NotificationTemplate() {}

    public NotificationTemplate(String name, TemplateType templateType, String contentTemplate) {
        this.name = name;
        this.templateType = templateType;
        this.contentTemplate = contentTemplate;
    }

    // Business Logic Methods
    public void incrementUsageCount() {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void setAsDefault() {
        this.isDefault = true;
    }

    public void unsetAsDefault() {
        this.isDefault = false;
    }

    public void incrementVersion() {
        this.version++;
    }

    public boolean isActive() {
        return isActive != null && isActive;
    }

    public boolean isDefault() {
        return isDefault != null && isDefault;
    }

    // Template Type Enum
    public enum TemplateType {
        ORDER_CONFIRMATION,     // Order placed successfully
        ORDER_STATUS_UPDATE,    // Order status changed
        DELIVERY_NOTIFICATION,  // Order out for delivery / ready for pickup
        CART_ABANDONMENT,       // Cart left without checkout
        MARKETING_CAMPAIGN,     // Promotional campaigns
        PASSWORD_RESET,         // Password reset request
        WELCOME,               // Welcome new users
        RECEIPT,               // Order receipt/invoice
        FEEDBACK_REQUEST,      // Request for feedback/review
        PROMOTION,             // Special offers and discounts
        LOW_STOCK_ALERT,       // Product low stock (admin)
        SYSTEM_MAINTENANCE,    // System maintenance notification
        BIRTHDAY_WISHES,       // Birthday special offers
        LOYALTY_POINTS,        // Loyalty program updates
        NEWSLETTER             // Regular newsletter
    }

    @Override
    public String toString() {
        return String.format("NotificationTemplate{id=%s, name='%s', type=%s, active=%s}",
                           id, name, templateType, isActive);
    }
}
