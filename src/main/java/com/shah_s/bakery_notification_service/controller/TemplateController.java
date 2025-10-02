package com.shah_s.bakery_notification_service.controller;

import com.shah_s.bakery_notification_service.dto.TemplateRequest;
import com.shah_s.bakery_notification_service.dto.TemplateResponse;
import com.shah_s.bakery_notification_service.entity.NotificationTemplate;
import com.shah_s.bakery_notification_service.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
@Validated
@Tag(name = "Templates", description = "Notification template management APIs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TemplateController {

    private static final Logger logger = LoggerFactory.getLogger(TemplateController.class);

    @Autowired
    private TemplateService templateService;

    @Operation(summary = "Create a new template", description = "Create a new notification template")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Template created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Template name already exists")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<TemplateResponse> createTemplate(
            @Valid @RequestBody TemplateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Creating template: name={}, type={}, requester={}",
                   request.getName(), request.getTemplateType(), requestingUserId);

        try {
            if (request.getCreatedBy() == null) {
                request.setCreatedBy(requestingUserId);
            }

            TemplateResponse response = templateService.createTemplate(request);

            logger.info("Template created successfully: id={}, name={}", response.getId(), response.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Failed to create template: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "Update a template", description = "Update an existing notification template")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "409", description = "Template name already exists")
    })
    @PutMapping("/{templateId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable UUID templateId,
            @Valid @RequestBody TemplateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Updating template: id={}, name={}, requester={}",
                   templateId, request.getName(), requestingUserId);

        try {
            TemplateResponse response = templateService.updateTemplate(templateId, request);

            logger.info("Template updated successfully: id={}, name={}", templateId, response.getName());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to update template {}: {}", templateId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "Get template by ID", description = "Retrieve a specific template by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template found"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @GetMapping("/{templateId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING') or hasRole('USER')")
    public ResponseEntity<TemplateResponse> getTemplateById(
            @Parameter(description = "Template ID", required = true)
            @PathVariable UUID templateId) {

        logger.debug("Getting template by ID: {}", templateId);

        try {
            TemplateResponse response = templateService.getTemplateById(templateId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get template {}: {}", templateId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get template by name", description = "Retrieve a specific template by its name")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template found"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @GetMapping("/name/{templateName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING') or hasRole('USER')")
    public ResponseEntity<TemplateResponse> getTemplateByName(
            @Parameter(description = "Template name", required = true)
            @PathVariable String templateName) {

        logger.debug("Getting template by name: {}", templateName);

        try {
            TemplateResponse response = templateService.getTemplateByName(templateName);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get template by name {}: {}", templateName, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get templates by type", description = "Retrieve all active templates of a specific type")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Templates retrieved successfully")
    })
    @GetMapping("/type/{templateType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING') or hasRole('USER')")
    public ResponseEntity<List<TemplateResponse>> getTemplatesByType(
            @Parameter(description = "Template type", required = true)
            @PathVariable NotificationTemplate.TemplateType templateType) {

        logger.debug("Getting templates by type: {}", templateType);

        try {
            List<TemplateResponse> responses = templateService.getTemplatesByType(templateType);
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Failed to get templates by type {}: {}", templateType, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get all active templates", description = "Retrieve all active notification templates")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Templates retrieved successfully")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING') or hasRole('USER')")
    public ResponseEntity<List<TemplateResponse>> getAllActiveTemplates() {

        logger.debug("Getting all active templates");

        try {
            List<TemplateResponse> responses = templateService.getAllActiveTemplates();
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Failed to get all active templates: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get templates with pagination", description = "Retrieve active templates with pagination")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Templates retrieved successfully")
    })
    @GetMapping("/paginated")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING') or hasRole('USER')")
    public ResponseEntity<Page<TemplateResponse>> getAllActiveTemplatesPaginated(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "ASC") String sortDir) {

        logger.debug("Getting paginated templates: page={}, size={}", page, size);

        try {
            Page<TemplateResponse> responses = templateService.getAllActiveTemplates(page, size, sortBy, sortDir);
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Failed to get paginated templates: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Search templates", description = "Search templates by name or description")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING') or hasRole('USER')")
    public ResponseEntity<List<TemplateResponse>> searchTemplates(
            @Parameter(description = "Search term", required = true)
            @RequestParam String q) {

        logger.debug("Searching templates: query={}", q);

        try {
            List<TemplateResponse> responses = templateService.searchTemplates(q);
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Failed to search templates: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get default template", description = "Get the default template for a specific type and language")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Default template found"),
        @ApiResponse(responseCode = "404", description = "No default template found")
    })
    @GetMapping("/default/{templateType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING') or hasRole('USER')")
    public ResponseEntity<TemplateResponse> getDefaultTemplate(
            @Parameter(description = "Template type", required = true)
            @PathVariable NotificationTemplate.TemplateType templateType,
            @Parameter(description = "Language code")
            @RequestParam(defaultValue = "en") String language) {

        logger.debug("Getting default template: type={}, language={}", templateType, language);

        try {
            TemplateResponse response = templateService.getDefaultTemplateForType(templateType, language);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get default template for type {} language {}: {}",
                        templateType, language, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Set template as default", description = "Set a template as the default for its type")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template set as default successfully"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @PutMapping("/{templateId}/set-default")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<Map<String, Object>> setAsDefaultTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable UUID templateId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Setting template as default: id={}, requester={}", templateId, requestingUserId);

        try {
            templateService.setAsDefaultTemplate(templateId);

            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "templateId", templateId,
                "message", "Template set as default successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to set template as default {}: {}", templateId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "SET_DEFAULT_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Activate template", description = "Activate a template")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template activated successfully"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @PutMapping("/{templateId}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<Map<String, Object>> activateTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable UUID templateId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Activating template: id={}, requester={}", templateId, requestingUserId);

        try {
            templateService.activateTemplate(templateId);

            Map<String, Object> response = Map.of(
                "status", "ACTIVATED",
                "templateId", templateId,
                "message", "Template activated successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to activate template {}: {}", templateId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "ACTIVATION_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Deactivate template", description = "Deactivate a template")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @PutMapping("/{templateId}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<Map<String, Object>> deactivateTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable UUID templateId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Deactivating template: id={}, requester={}", templateId, requestingUserId);

        try {
            templateService.deactivateTemplate(templateId);

            Map<String, Object> response = Map.of(
                "status", "DEACTIVATED",
                "templateId", templateId,
                "message", "Template deactivated successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to deactivate template {}: {}", templateId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "DEACTIVATION_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Delete template", description = "Delete a template permanently")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Template not found"),
        @ApiResponse(responseCode = "400", description = "Template cannot be deleted")
    })
    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable UUID templateId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Deleting template: id={}, requester={}", templateId, requestingUserId);

        try {
            templateService.deleteTemplate(templateId);

            Map<String, Object> response = Map.of(
                "status", "DELETED",
                "templateId", templateId,
                "message", "Template deleted successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to delete template {}: {}", templateId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "DELETION_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Validate template", description = "Validate a template with test data")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template validated successfully")
    })
    @PostMapping("/{templateId}/validate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<Map<String, Object>> validateTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable UUID templateId,
            @RequestBody(required = false) Map<String, Object> testData) {

        logger.debug("Validating template: id={}", templateId);

        try {
            Map<String, Object> validationResult = templateService.validateTemplate(templateId, testData);
            return ResponseEntity.ok(validationResult);

        } catch (Exception e) {
            logger.error("Failed to validate template {}: {}", templateId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get template statistics", description = "Get template usage and performance statistics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYTICS')")
    public ResponseEntity<Map<String, Object>> getTemplateStatistics() {

        logger.debug("Getting template statistics");

        try {
            Map<String, Object> statistics = templateService.getTemplateStatistics();
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            logger.error("Failed to get template statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Health check", description = "Check template service health")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.debug("Template service health check");

        try {
            Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "template-service",
                "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage());
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "service", "template-service",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
}
