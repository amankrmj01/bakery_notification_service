package com.shah_s.bakery_notification_service.controller;

import com.shah_s.bakery_notification_service.dto.CampaignRequest;
import com.shah_s.bakery_notification_service.dto.CampaignResponse;
import com.shah_s.bakery_notification_service.entity.NotificationCampaign;
import com.shah_s.bakery_notification_service.service.CampaignService;
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
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/campaigns")
@Validated
@Tag(name = "Campaigns", description = "Notification campaign management APIs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CampaignController {

    private static final Logger logger = LoggerFactory.getLogger(CampaignController.class);

    @Autowired
    private CampaignService campaignService;

    @Operation(summary = "Create a new campaign", description = "Create a new notification campaign")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Campaign created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Campaign name already exists")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<CampaignResponse> createCampaign(
            @Valid @RequestBody CampaignRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Creating campaign: name={}, type={}, requester={}",
                   request.getName(), request.getCampaignType(), requestingUserId);

        try {
            if (request.getCreatedBy() == null) {
                request.setCreatedBy(requestingUserId);
            }

            CampaignResponse response = campaignService.createCampaign(request);

            logger.info("Campaign created successfully: id={}, name={}", response.getId(), response.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Failed to create campaign: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "Update a campaign", description = "Update an existing campaign (only in draft status)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data or campaign not in draft status"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    @PutMapping("/{campaignId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @Valid @RequestBody CampaignRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Updating campaign: id={}, name={}, requester={}",
                   campaignId, request.getName(), requestingUserId);

        try {
            CampaignResponse response = campaignService.updateCampaign(campaignId, request);

            logger.info("Campaign updated successfully: id={}, name={}", campaignId, response.getName());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to update campaign {}: {}", campaignId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "Get campaign by ID", description = "Retrieve a specific campaign by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign found"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    @GetMapping("/{campaignId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING') or hasRole('USER')")
    public ResponseEntity<CampaignResponse> getCampaignById(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId) {

        logger.debug("Getting campaign by ID: {}", campaignId);

        try {
            CampaignResponse response = campaignService.getCampaignById(campaignId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get campaign {}: {}", campaignId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get campaigns by status", description = "Retrieve campaigns by their status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaigns retrieved successfully")
    })
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<List<CampaignResponse>> getCampaignsByStatus(
            @Parameter(description = "Campaign status", required = true)
            @PathVariable NotificationCampaign.CampaignStatus status) {

        logger.debug("Getting campaigns by status: {}", status);

        try {
            List<CampaignResponse> responses = campaignService.getCampaignsByStatus(status);
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Failed to get campaigns by status {}: {}", status, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get campaigns by type", description = "Retrieve campaigns by their type")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaigns retrieved successfully")
    })
    @GetMapping("/type/{campaignType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<List<CampaignResponse>> getCampaignsByType(
            @Parameter(description = "Campaign type", required = true)
            @PathVariable NotificationCampaign.CampaignType campaignType) {

        logger.debug("Getting campaigns by type: {}", campaignType);

        try {
            List<CampaignResponse> responses = campaignService.getCampaignsByType(campaignType);
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Failed to get campaigns by type {}: {}", campaignType, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get all active campaigns", description = "Retrieve all active campaigns")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaigns retrieved successfully")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<List<CampaignResponse>> getAllActiveCampaigns() {

        logger.debug("Getting all active campaigns");

        try {
            List<CampaignResponse> responses = campaignService.getAllActiveCampaigns();
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Failed to get all active campaigns: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get campaigns with pagination", description = "Retrieve active campaigns with pagination")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaigns retrieved successfully")
    })
    @GetMapping("/paginated")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<Page<CampaignResponse>> getAllActiveCampaignsPaginated(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") String sortDir) {

        logger.debug("Getting paginated campaigns: page={}, size={}", page, size);

        try {
            Page<CampaignResponse> responses = campaignService.getAllActiveCampaigns(page, size, sortBy, sortDir);
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Failed to get paginated campaigns: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Search campaigns", description = "Search campaigns by name or description")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<List<CampaignResponse>> searchCampaigns(
            @Parameter(description = "Search term", required = true)
            @RequestParam String q) {

        logger.debug("Searching campaigns: query={}", q);

        try {
            List<CampaignResponse> responses = campaignService.searchCampaigns(q);
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Failed to search campaigns: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Start campaign", description = "Start a campaign execution")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign started successfully"),
        @ApiResponse(responseCode = "400", description = "Campaign cannot be started"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    @PutMapping("/{campaignId}/start")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<Map<String, Object>> startCampaign(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Starting campaign: id={}, requester={}", campaignId, requestingUserId);

        try {
            campaignService.startCampaign(campaignId);

            Map<String, Object> response = Map.of(
                "status", "STARTED",
                "campaignId", campaignId,
                "message", "Campaign started successfully",
                "startedBy", requestingUserId,
                "startedAt", LocalDateTime.now()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to start campaign {}: {}", campaignId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "START_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Pause campaign", description = "Pause a running campaign")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign paused successfully"),
        @ApiResponse(responseCode = "400", description = "Campaign cannot be paused"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    @PutMapping("/{campaignId}/pause")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<Map<String, Object>> pauseCampaign(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Pausing campaign: id={}, requester={}", campaignId, requestingUserId);

        try {
            campaignService.pauseCampaign(campaignId);

            Map<String, Object> response = Map.of(
                "status", "PAUSED",
                "campaignId", campaignId,
                "message", "Campaign paused successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to pause campaign {}: {}", campaignId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "PAUSE_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Resume campaign", description = "Resume a paused campaign")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign resumed successfully"),
        @ApiResponse(responseCode = "400", description = "Campaign cannot be resumed"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    @PutMapping("/{campaignId}/resume")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<Map<String, Object>> resumeCampaign(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Resuming campaign: id={}, requester={}", campaignId, requestingUserId);

        try {
            campaignService.resumeCampaign(campaignId);

            Map<String, Object> response = Map.of(
                "status", "RESUMED",
                "campaignId", campaignId,
                "message", "Campaign resumed successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to resume campaign {}: {}", campaignId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "RESUME_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Complete campaign", description = "Complete a running campaign")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign completed successfully"),
        @ApiResponse(responseCode = "400", description = "Campaign cannot be completed"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    @PutMapping("/{campaignId}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<Map<String, Object>> completeCampaign(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Completing campaign: id={}, requester={}", campaignId, requestingUserId);

        try {
            campaignService.completeCampaign(campaignId);

            Map<String, Object> response = Map.of(
                "status", "COMPLETED",
                "campaignId", campaignId,
                "message", "Campaign completed successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to complete campaign {}: {}", campaignId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "COMPLETE_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Cancel campaign", description = "Cancel a campaign")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Campaign cannot be cancelled"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    @PutMapping("/{campaignId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING')")
    public ResponseEntity<Map<String, Object>> cancelCampaign(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Cancelling campaign: id={}, requester={}", campaignId, requestingUserId);

        try {
            campaignService.cancelCampaign(campaignId);

            Map<String, Object> response = Map.of(
                "status", "CANCELLED",
                "campaignId", campaignId,
                "message", "Campaign cancelled successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to cancel campaign {}: {}", campaignId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "CANCEL_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Delete campaign", description = "Delete a campaign permanently")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Campaign cannot be deleted"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    @DeleteMapping("/{campaignId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteCampaign(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Deleting campaign: id={}, requester={}", campaignId, requestingUserId);

        try {
            campaignService.deleteCampaign(campaignId);

            Map<String, Object> response = Map.of(
                "status", "DELETED",
                "campaignId", campaignId,
                "message", "Campaign deleted successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to delete campaign {}: {}", campaignId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "DELETION_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Get campaign statistics", description = "Get campaign performance and analytics statistics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING') or hasRole('ANALYTICS')")
    public ResponseEntity<Map<String, Object>> getCampaignStatistics(
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Getting campaign statistics: startDate={}, endDate={}, requester={}",
                    startDate, endDate, requestingUserId);

        try {
            Map<String, Object> statistics = campaignService.getCampaignStatistics(startDate, endDate);
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            logger.error("Failed to get campaign statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Cleanup old campaigns", description = "Manually trigger cleanup of old campaigns")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Cleanup started"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<Map<String, Object>> cleanupOldCampaigns(
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Cleaning up old campaigns: requester={}", requestingUserId);

        try {
            campaignService.cleanupOldCampaigns();

            Map<String, Object> response = Map.of(
                "status", "CLEANING",
                "message", "Old campaigns cleanup started"
            );

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            logger.error("Failed to cleanup old campaigns: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Health check", description = "Check campaign service health")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.debug("Campaign service health check");

        try {
            Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "campaign-service",
                "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage());
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "service", "campaign-service",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
}
