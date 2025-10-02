package com.shah_s.bakery_notification_service.dto;

import com.shah_s.bakery_notification_service.entity.NotificationTemplate;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TemplateRequest {

    // Getters and Setters
    @NotBlank(message = "Template name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Template type is required")
    private NotificationTemplate.TemplateType templateType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 255, message = "Subject template must not exceed 255 characters")
    private String subjectTemplate; // For email templates

    @Size(max = 500, message = "Title template must not exceed 500 characters")
    private String titleTemplate;

    @NotBlank(message = "Content template is required")
    private String contentTemplate;

    private String htmlTemplate; // For email HTML content

    private String smsTemplate; // For SMS content

    private String pushTemplate; // For push notification content

    private List<String> variables; // List of variable names used in template

    private Map<String, Object> sampleData; // Sample variable values for testing

    private Boolean isActive = true;

    private Boolean isDefault = false;

    @Size(max = 10, message = "Language must not exceed 10 characters")
    private String language = "en"; // ISO language code

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category; // TRANSACTIONAL, MARKETING, SYSTEM

    private List<String> tags; // List of tags

    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    // Constructors
    public TemplateRequest() {}

    public TemplateRequest(String name, NotificationTemplate.TemplateType templateType, String contentTemplate) {
        this.name = name;
        this.templateType = templateType;
        this.contentTemplate = contentTemplate;
    }

}
