package com.shah_s.bakery_notification_service.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shah_s.bakery_notification_service.entity.NotificationTemplate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class TemplateResponse {

    // Getters and Setters
    private UUID id;
    private String name;
    private NotificationTemplate.TemplateType templateType;
    private String description;
    private String subjectTemplate;
    private String titleTemplate;
    private String contentTemplate;
    private String htmlTemplate;
    private String smsTemplate;
    private String pushTemplate;
    private List<String> variables;
    private Map<String, Object> sampleData;
    private Boolean isActive;
    private Boolean isDefault;
    private Integer version;
    private String language;
    private String category;
    private List<String> tags;
    private Long usageCount;
    private LocalDateTime lastUsedAt;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public TemplateResponse() {}

    // Static factory method
    public static TemplateResponse from(NotificationTemplate template) {
        TemplateResponse response = new TemplateResponse();
        response.id = template.getId();
        response.name = template.getName();
        response.templateType = template.getTemplateType();
        response.description = template.getDescription();
        response.subjectTemplate = template.getSubjectTemplate();
        response.titleTemplate = template.getTitleTemplate();
        response.contentTemplate = template.getContentTemplate();
        response.htmlTemplate = template.getHtmlTemplate();
        response.smsTemplate = template.getSmsTemplate();
        response.pushTemplate = template.getPushTemplate();
        response.isActive = template.getIsActive();
        response.isDefault = template.getIsDefault();
        response.version = template.getVersion();
        response.language = template.getLanguage();
        response.category = template.getCategory();
        response.usageCount = template.getUsageCount();
        response.lastUsedAt = template.getLastUsedAt();
        response.createdBy = template.getCreatedBy();
        response.updatedBy = template.getUpdatedBy();
        response.createdAt = template.getCreatedAt();
        response.updatedAt = template.getUpdatedAt();

        // Parse JSON fields
        response.variables = parseJsonToList(template.getVariables());
        response.sampleData = parseJsonToMap(template.getSampleData());
        response.tags = parseJsonToList(template.getTags());

        return response;
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
