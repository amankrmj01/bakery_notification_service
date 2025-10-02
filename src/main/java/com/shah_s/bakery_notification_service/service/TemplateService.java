package com.shah_s.bakery_notification_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shah_s.bakery_notification_service.dto.TemplateRequest;
import com.shah_s.bakery_notification_service.dto.TemplateResponse;
import com.shah_s.bakery_notification_service.entity.NotificationTemplate;
import com.shah_s.bakery_notification_service.exception.NotificationServiceException;
import com.shah_s.bakery_notification_service.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class TemplateService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateService.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private ObjectMapper objectMapper;

    // Create template
    public TemplateResponse createTemplate(TemplateRequest request) {
        logger.info("Creating template: name={}, type={}", request.getName(), request.getTemplateType());

        try {
            // Validate template name uniqueness
            if (templateRepository.findByName(request.getName()).isPresent()) {
                throw new NotificationServiceException("Template with name already exists: " + request.getName());
            }

            // Extract variables from templates
            Set<String> extractedVariables = extractVariablesFromTemplates(request);

            NotificationTemplate template = createTemplateFromRequest(request);

            // Set variables as JSON
            if (!extractedVariables.isEmpty()) {
                template.setVariables(objectMapper.writeValueAsString(new ArrayList<>(extractedVariables)));
            }

            // Set sample data as JSON
            if (request.getSampleData() != null) {
                template.setSampleData(objectMapper.writeValueAsString(request.getSampleData()));
            }

            // Set tags as JSON
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                template.setTags(objectMapper.writeValueAsString(request.getTags()));
            }

            template = templateRepository.save(template);

            // Set as default if requested and no other default exists
            if (template.getIsDefault()) {
                setAsDefaultTemplate(template.getId());
            }

            logger.info("Template created successfully: {}", template.getId());
            return TemplateResponse.from(template);

        } catch (Exception e) {
            logger.error("Failed to create template: {}", e.getMessage(), e);
            throw new NotificationServiceException("Failed to create template: " + e.getMessage());
        }
    }

    // Update template
    @CacheEvict(value = "templates", key = "#templateId")
    public TemplateResponse updateTemplate(UUID templateId, TemplateRequest request) {
        logger.info("Updating template: id={}, name={}", templateId, request.getName());

        try {
            NotificationTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new NotificationServiceException("Template not found: " + templateId));

            // Validate name uniqueness (excluding current template)
            if (!template.getName().equals(request.getName()) &&
                templateRepository.existsByNameAndNotId(request.getName(), templateId)) {
                throw new NotificationServiceException("Template with name already exists: " + request.getName());
            }

            // Extract variables from updated templates
            Set<String> extractedVariables = extractVariablesFromTemplates(request);

            // Update template fields
            updateTemplateFromRequest(template, request);

            // Set variables as JSON
            if (!extractedVariables.isEmpty()) {
                template.setVariables(objectMapper.writeValueAsString(new ArrayList<>(extractedVariables)));
            }

            // Set sample data as JSON
            if (request.getSampleData() != null) {
                template.setSampleData(objectMapper.writeValueAsString(request.getSampleData()));
            }

            // Set tags as JSON
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                template.setTags(objectMapper.writeValueAsString(request.getTags()));
            }

            // Increment version
            template.incrementVersion();

            template = templateRepository.save(template);

            logger.info("Template updated successfully: {}", templateId);
            return TemplateResponse.from(template);

        } catch (Exception e) {
            logger.error("Failed to update template {}: {}", templateId, e.getMessage(), e);
            throw new NotificationServiceException("Failed to update template: " + e.getMessage());
        }
    }

    // Get template by ID
    @Cacheable(value = "templates", key = "#templateId")
    @Transactional(readOnly = true)
    public TemplateResponse getTemplateById(UUID templateId) {
        logger.debug("Getting template by ID: {}", templateId);

        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotificationServiceException("Template not found: " + templateId));

        return TemplateResponse.from(template);
    }

    // Get template by name
    @Transactional(readOnly = true)
    public TemplateResponse getTemplateByName(String name) {
        logger.debug("Getting template by name: {}", name);

        NotificationTemplate template = templateRepository.findByName(name)
                .orElseThrow(() -> new NotificationServiceException("Template not found: " + name));

        return TemplateResponse.from(template);
    }

    // Get templates by type
    @Transactional(readOnly = true)
    public List<TemplateResponse> getTemplatesByType(NotificationTemplate.TemplateType templateType) {
        logger.debug("Getting templates by type: {}", templateType);

        return templateRepository.findByTemplateTypeAndIsActiveTrue(templateType).stream()
                .map(TemplateResponse::from)
                .collect(Collectors.toList());
    }

    // Get all active templates
    @Transactional(readOnly = true)
    public List<TemplateResponse> getAllActiveTemplates() {
        logger.debug("Getting all active templates");

        return templateRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(TemplateResponse::from)
                .collect(Collectors.toList());
    }

    // Get templates with pagination
    @Transactional(readOnly = true)
    public Page<TemplateResponse> getAllActiveTemplates(int page, int size, String sortBy, String sortDir) {
        logger.debug("Getting active templates with pagination: page={}, size={}", page, size);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return templateRepository.findByIsActiveTrueOrderByNameAsc(pageable)
                .map(TemplateResponse::from);
    }

    // Search templates
    @Transactional(readOnly = true)
    public List<TemplateResponse> searchTemplates(String searchTerm) {
        logger.debug("Searching templates: {}", searchTerm);

        return templateRepository.searchActiveTemplates(searchTerm).stream()
                .map(TemplateResponse::from)
                .collect(Collectors.toList());
    }

    // Get default template for type
    @Transactional(readOnly = true)
    public TemplateResponse getDefaultTemplateForType(NotificationTemplate.TemplateType templateType,
                                                     String language) {
        logger.debug("Getting default template: type={}, language={}", templateType, language);

        Optional<NotificationTemplate> template;

        if (language != null) {
            template = templateRepository.findDefaultByTemplateTypeAndLanguage(templateType, language);
        } else {
            template = templateRepository.findDefaultByTemplateType(templateType);
        }

        if (template.isPresent()) {
            return TemplateResponse.from(template.get());
        }

        throw new NotificationServiceException("No default template found for type: " + templateType);
    }

    // Set template as default
    @CacheEvict(value = "templates", allEntries = true)
    public void setAsDefaultTemplate(UUID templateId) {
        logger.info("Setting template as default: {}", templateId);

        try {
            NotificationTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new NotificationServiceException("Template not found: " + templateId));

            // Unset current default for this type and language
            templateRepository.unsetDefaultForTypeAndLanguage(template.getTemplateType(), template.getLanguage());

            // Set this template as default
            templateRepository.setAsDefault(templateId);

            logger.info("Template set as default successfully: {}", templateId);

        } catch (Exception e) {
            logger.error("Failed to set template as default {}: {}", templateId, e.getMessage(), e);
            throw new NotificationServiceException("Failed to set template as default: " + e.getMessage());
        }
    }

    // Activate template
    @CacheEvict(value = "templates", key = "#templateId")
    public void activateTemplate(UUID templateId) {
        logger.info("Activating template: {}", templateId);

        int updated = templateRepository.activateTemplate(templateId);
        if (updated == 0) {
            throw new NotificationServiceException("Template not found: " + templateId);
        }

        logger.info("Template activated successfully: {}", templateId);
    }

    // Deactivate template
    @CacheEvict(value = "templates", key = "#templateId")
    public void deactivateTemplate(UUID templateId) {
        logger.info("Deactivating template: {}", templateId);

        int updated = templateRepository.deactivateTemplate(templateId);
        if (updated == 0) {
            throw new NotificationServiceException("Template not found: " + templateId);
        }

        logger.info("Template deactivated successfully: {}", templateId);
    }

    // Delete template
    @CacheEvict(value = "templates", key = "#templateId")
    public void deleteTemplate(UUID templateId) {
        logger.info("Deleting template: {}", templateId);

        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotificationServiceException("Template not found: " + templateId));

        // Check if template is being used
        // This would require checking with NotificationRepository
        // Long usageCount = notificationRepository.countByTemplateId(templateId);
        // if (usageCount > 0) {
        //     throw new NotificationServiceException("Cannot delete template that is being used");
        // }

        templateRepository.delete(template);
        logger.info("Template deleted successfully: {}", templateId);
    }

    // Process template with variables
    public String processTemplate(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        try {
            // Simple variable replacement using {{variable}} syntax
            if (variables != null && !variables.isEmpty()) {
                String result = template;
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    String placeholder = "{{" + entry.getKey() + "}}";
                    String value = entry.getValue() != null ? entry.getValue().toString() : "";
                    result = result.replace(placeholder, value);
                }
                return result;
            }

            return template;

        } catch (Exception e) {
            logger.error("Failed to process template: {}", e.getMessage());
            return template; // Return original template on error
        }
    }

    // Process Thymeleaf template
    public String processThymeleafTemplate(String template, Map<String, Object> variables) {
        try {
            Context context = new Context();
            if (variables != null) {
                context.setVariables(variables);
            }

            return templateEngine.process(template, context);

        } catch (Exception e) {
            logger.error("Failed to process Thymeleaf template: {}", e.getMessage());
            return template; // Return original template on error
        }
    }

    // Validate template
    public Map<String, Object> validateTemplate(UUID templateId, Map<String, Object> testData) {
        logger.debug("Validating template: {}", templateId);

        try {
            NotificationTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new NotificationServiceException("Template not found: " + templateId));

            Map<String, Object> result = new HashMap<>();
            result.put("templateId", templateId);
            result.put("isValid", true);
            result.put("errors", new ArrayList<String>());

            // Extract variables from templates
            Set<String> requiredVariables = extractVariablesFromTemplate(template);
            result.put("requiredVariables", requiredVariables);

            // Check if test data provides all required variables
            List<String> missingVariables = new ArrayList<>();
            if (testData != null) {
                for (String variable : requiredVariables) {
                    if (!testData.containsKey(variable)) {
                        missingVariables.add(variable);
                    }
                }
            } else {
                missingVariables.addAll(requiredVariables);
            }

            if (!missingVariables.isEmpty()) {
                result.put("isValid", false);
                ((List<String>) result.get("errors")).add("Missing variables: " + String.join(", ", missingVariables));
            }

            // Test template processing
            if (testData != null && missingVariables.isEmpty()) {
                try {
                    Map<String, String> processedTemplates = new HashMap<>();

                    if (template.getTitleTemplate() != null) {
                        processedTemplates.put("title", processTemplate(template.getTitleTemplate(), testData));
                    }
                    if (template.getContentTemplate() != null) {
                        processedTemplates.put("content", processTemplate(template.getContentTemplate(), testData));
                    }
                    if (template.getSubjectTemplate() != null) {
                        processedTemplates.put("subject", processTemplate(template.getSubjectTemplate(), testData));
                    }
                    if (template.getHtmlTemplate() != null) {
                        processedTemplates.put("html", processTemplate(template.getHtmlTemplate(), testData));
                    }
                    if (template.getSmsTemplate() != null) {
                        processedTemplates.put("sms", processTemplate(template.getSmsTemplate(), testData));
                    }
                    if (template.getPushTemplate() != null) {
                        processedTemplates.put("push", processTemplate(template.getPushTemplate(), testData));
                    }

                    result.put("processedTemplates", processedTemplates);

                } catch (Exception e) {
                    result.put("isValid", false);
                    ((List<String>) result.get("errors")).add("Template processing error: " + e.getMessage());
                }
            }

            return result;

        } catch (Exception e) {
            logger.error("Failed to validate template {}: {}", templateId, e.getMessage(), e);
            throw new NotificationServiceException("Failed to validate template: " + e.getMessage());
        }
    }

    // Get template statistics
    @Transactional(readOnly = true)
    public Map<String, Object> getTemplateStatistics() {
        logger.debug("Getting template statistics");

        Map<String, Object> stats = new HashMap<>();

        // Basic counts
        stats.put("totalTemplates", templateRepository.count());
        stats.put("activeTemplates", templateRepository.countByIsActiveTrue());

        // Statistics by type
        List<Object[]> typeStats = templateRepository.getTemplateStatisticsByType();
        Map<String, Long> typeCounts = typeStats.stream()
                .collect(Collectors.toMap(
                    arr -> arr[0].toString(),
                    arr -> ((Number) arr[1]).longValue()
                ));
        stats.put("byType", typeCounts);

        // Statistics by category
        List<Object[]> categoryStats = templateRepository.getTemplateStatisticsByCategory();
        Map<String, Long> categoryCounts = categoryStats.stream()
                .collect(Collectors.toMap(
                    arr -> arr[0].toString(),
                    arr -> ((Number) arr[1]).longValue()
                ));
        stats.put("byCategory", categoryCounts);

        // Statistics by language
        List<Object[]> languageStats = templateRepository.getTemplateStatisticsByLanguage();
        Map<String, Long> languageCounts = languageStats.stream()
                .collect(Collectors.toMap(
                    arr -> arr[0].toString(),
                    arr -> ((Number) arr[1]).longValue()
                ));
        stats.put("byLanguage", languageCounts);

        // Usage statistics
        List<Object[]> usageStats = templateRepository.getTemplateUsageStatistics();
        stats.put("usageStats", usageStats);

        return stats;
    }

    // Private helper methods
    private NotificationTemplate createTemplateFromRequest(TemplateRequest request) {
        NotificationTemplate template = new NotificationTemplate(
            request.getName(),
            request.getTemplateType(),
            request.getContentTemplate()
        );

        template.setDescription(request.getDescription());
        template.setSubjectTemplate(request.getSubjectTemplate());
        template.setTitleTemplate(request.getTitleTemplate());
        template.setHtmlTemplate(request.getHtmlTemplate());
        template.setSmsTemplate(request.getSmsTemplate());
        template.setPushTemplate(request.getPushTemplate());
        template.setIsActive(request.getIsActive());
        template.setIsDefault(request.getIsDefault());
        template.setLanguage(request.getLanguage());
        template.setCategory(request.getCategory());
        template.setCreatedBy(request.getCreatedBy());

        return template;
    }

    private void updateTemplateFromRequest(NotificationTemplate template, TemplateRequest request) {
        template.setName(request.getName());
        template.setTemplateType(request.getTemplateType());
        template.setDescription(request.getDescription());
        template.setSubjectTemplate(request.getSubjectTemplate());
        template.setTitleTemplate(request.getTitleTemplate());
        template.setContentTemplate(request.getContentTemplate());
        template.setHtmlTemplate(request.getHtmlTemplate());
        template.setSmsTemplate(request.getSmsTemplate());
        template.setPushTemplate(request.getPushTemplate());
        template.setIsActive(request.getIsActive());
        template.setIsDefault(request.getIsDefault());
        template.setLanguage(request.getLanguage());
        template.setCategory(request.getCategory());
        // Note: Not updating createdBy, but could set updatedBy if needed
    }

    private Set<String> extractVariablesFromTemplates(TemplateRequest request) {
        Set<String> variables = new HashSet<>();

        extractVariables(request.getTitleTemplate(), variables);
        extractVariables(request.getContentTemplate(), variables);
        extractVariables(request.getSubjectTemplate(), variables);
        extractVariables(request.getHtmlTemplate(), variables);
        extractVariables(request.getSmsTemplate(), variables);
        extractVariables(request.getPushTemplate(), variables);

        return variables;
    }

    private Set<String> extractVariablesFromTemplate(NotificationTemplate template) {
        Set<String> variables = new HashSet<>();

        extractVariables(template.getTitleTemplate(), variables);
        extractVariables(template.getContentTemplate(), variables);
        extractVariables(template.getSubjectTemplate(), variables);
        extractVariables(template.getHtmlTemplate(), variables);
        extractVariables(template.getSmsTemplate(), variables);
        extractVariables(template.getPushTemplate(), variables);

        return variables;
    }

    private void extractVariables(String template, Set<String> variables) {
        if (template == null) return;

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            variables.add(matcher.group(1).trim());
        }
    }
}
