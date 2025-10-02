package com.shah_s.bakery_notification_service.controller;

import com.shah_s.bakery_notification_service.dto.NotificationResponse;
import com.shah_s.bakery_notification_service.dto.SendNotificationRequest;
import com.shah_s.bakery_notification_service.entity.Notification;
import com.shah_s.bakery_notification_service.service.NotificationService;
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
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/notifications")
@Validated
@Tag(name = "Notifications", description = "Notification management APIs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    @Operation(summary = "Send a notification", description = "Send a single notification via email, SMS, push, or in-app")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notification sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Failed to send notification")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM') or hasRole('MARKETING')")
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody SendNotificationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId,
            @RequestHeader(value = "X-User-Role", required = false) String requestingUserRole) {

        logger.info("Sending notification: type={}, userId={}, requester={}",
                request.getType(), request.getUserId(), requestingUserId);

        try {
            // Set source and triggered by if not provided
            if (request.getSource() == null) {
                request.setSource("API");
            }
            if (request.getTriggeredBy() == null) {
                request.setTriggeredBy("MANUAL");
            }

            NotificationResponse response = notificationService.sendNotification(request);

            logger.info("Notification sent successfully: id={}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Failed to send notification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Send bulk notifications", description = "Send multiple notifications in batch")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Bulk notifications accepted for processing"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Failed to process bulk notifications")
    })
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM') or hasRole('MARKETING')")
    public ResponseEntity<Map<String, Object>> sendBulkNotifications(
            @Valid @RequestBody List<SendNotificationRequest> requests,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Sending bulk notifications: count={}, requester={}", requests.size(), requestingUserId);

        try {
            // Set default values for all requests
            for (SendNotificationRequest request : requests) {
                if (request.getSource() == null) {
                    request.setSource("API_BULK");
                }
                if (request.getTriggeredBy() == null) {
                    request.setTriggeredBy("BULK_MANUAL");
                }
            }

            CompletableFuture<List<NotificationResponse>> future =
                    notificationService.sendBulkNotifications(requests);

            Map<String, Object> response = Map.of(
                    "status", "ACCEPTED",
                    "count", requests.size(),
                    "message", "Bulk notifications accepted for processing"
            );

            logger.info("Bulk notifications accepted: count={}", requests.size());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (Exception e) {
            logger.error("Failed to process bulk notifications: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get notification by ID", description = "Retrieve a specific notification by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification found"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @GetMapping("/{notificationId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM') or hasRole('USER')")
    public ResponseEntity<NotificationResponse> getNotificationById(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID notificationId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Getting notification: id={}, requester={}", notificationId, requestingUserId);

        try {
            NotificationResponse notification = notificationService.getNotificationById(notificationId);
            return ResponseEntity.ok(notification);

        } catch (Exception e) {
            logger.error("Failed to get notification {}: {}", notificationId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get notifications by user", description = "Retrieve all notifications for a specific user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM') or (#userId.toString() == authentication.name)")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Getting notifications by user: userId={}, requester={}", userId, requestingUserId);

        try {
            List<NotificationResponse> notifications = notificationService.getNotificationsByUser(userId);
            return ResponseEntity.ok(notifications);

        } catch (Exception e) {
            logger.error("Failed to get notifications for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get notifications by user with pagination",
            description = "Retrieve paginated notifications for a specific user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/user/{userId}/paginated")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM') or (#userId.toString() == authentication.name)")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsByUserPaginated(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Getting paginated notifications: userId={}, page={}, size={}", userId, page, size);

        try {
            Page<NotificationResponse> notifications = notificationService.getNotificationsByUser(
                    userId, page, size, sortBy, sortDir);
            return ResponseEntity.ok(notifications);

        } catch (Exception e) {
            logger.error("Failed to get paginated notifications for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get notifications by status", description = "Retrieve notifications by their status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
    })
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByStatus(
            @Parameter(description = "Notification status", required = true)
            @PathVariable Notification.NotificationStatus status,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Getting notifications by status: status={}, requester={}", status, requestingUserId);

        try {
            List<NotificationResponse> notifications = notificationService.getNotificationsByStatus(status);
            return ResponseEntity.ok(notifications);

        } catch (Exception e) {
            logger.error("Failed to get notifications by status {}: {}", status, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get notifications by type", description = "Retrieve notifications by their type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
    })
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByType(
            @Parameter(description = "Notification type", required = true)
            @PathVariable Notification.NotificationType type,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Getting notifications by type: type={}, requester={}", type, requestingUserId);

        try {
            List<NotificationResponse> notifications = notificationService.getNotificationsByType(type);
            return ResponseEntity.ok(notifications);

        } catch (Exception e) {
            logger.error("Failed to get notifications by type {}: {}", type, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get notifications by campaign", description = "Retrieve notifications for a specific campaign")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
    })
    @GetMapping("/campaign/{campaignId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM') or hasRole('MARKETING')")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByCampaign(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Getting notifications by campaign: campaignId={}, requester={}", campaignId, requestingUserId);

        try {
            List<NotificationResponse> notifications = notificationService.getNotificationsByCampaign(campaignId);
            return ResponseEntity.ok(notifications);

        } catch (Exception e) {
            logger.error("Failed to get notifications by campaign {}: {}", campaignId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Cancel notification", description = "Cancel a pending notification")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Notification not found"),
            @ApiResponse(responseCode = "400", description = "Notification cannot be cancelled")
    })
    @PutMapping("/{notificationId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<Map<String, Object>> cancelNotification(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID notificationId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Cancelling notification: id={}, requester={}", notificationId, requestingUserId);

        try {
            notificationService.cancelNotification(notificationId);

            Map<String, Object> response = Map.of(
                    "status", "CANCELLED",
                    "notificationId", notificationId,
                    "message", "Notification cancelled successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to cancel notification {}: {}", notificationId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                    "error", "CANCELLATION_FAILED",
                    "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Mark notification as opened", description = "Track when a notification is opened by user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as opened"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PutMapping("/{notificationId}/opened")
    public ResponseEntity<Map<String, Object>> markNotificationAsOpened(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID notificationId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Marking notification as opened: id={}, requester={}", notificationId, requestingUserId);

        try {
            notificationService.markNotificationAsOpened(notificationId);

            Map<String, Object> response = Map.of(
                    "status", "OPENED",
                    "notificationId", notificationId,
                    "openedAt", LocalDateTime.now()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to mark notification as opened {}: {}", notificationId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Mark notification as clicked", description = "Track when a notification is clicked by user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as clicked"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PutMapping("/{notificationId}/clicked")
    public ResponseEntity<Map<String, Object>> markNotificationAsClicked(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable UUID notificationId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Marking notification as clicked: id={}, requester={}", notificationId, requestingUserId);

        try {
            notificationService.markNotificationAsClicked(notificationId);

            Map<String, Object> response = Map.of(
                    "status", "CLICKED",
                    "notificationId", notificationId,
                    "clickedAt", LocalDateTime.now()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to mark notification as clicked {}: {}", notificationId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get notification statistics", description = "Get notification delivery and engagement statistics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM') or hasRole('ANALYTICS')")
    public ResponseEntity<Map<String, Object>> getNotificationStatistics(
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Getting notification statistics: startDate={}, endDate={}, requester={}",
                startDate, endDate, requestingUserId);

        try {
            Map<String, Object> statistics = notificationService.getNotificationStatistics(startDate, endDate);
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            logger.error("Failed to get notification statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Process pending notifications", description = "Manually trigger processing of pending notifications")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Processing started"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/process-pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<Map<String, Object>> processPendingNotifications(
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Processing pending notifications: requester={}", requestingUserId);

        try {
            notificationService.processPendingNotifications();

            Map<String, Object> response = Map.of(
                    "status", "PROCESSING",
                    "message", "Pending notifications processing started"
            );

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            logger.error("Failed to process pending notifications: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Retry failed notifications", description = "Manually trigger retry of failed notifications")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Retry started"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/retry-failed")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<Map<String, Object>> retryFailedNotifications(
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Retrying failed notifications: requester={}", requestingUserId);

        try {
            notificationService.retryFailedNotifications();

            Map<String, Object> response = Map.of(
                    "status", "RETRYING",
                    "message", "Failed notifications retry started"
            );

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            logger.error("Failed to retry failed notifications: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Cleanup old notifications", description = "Manually trigger cleanup of old notifications")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Cleanup started"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<Map<String, Object>> cleanupOldNotifications(
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Cleaning up old notifications: requester={}", requestingUserId);

        try {
            notificationService.cleanupOldNotifications();

            Map<String, Object> response = Map.of(
                    "status", "CLEANING",
                    "message", "Old notifications cleanup started"
            );

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            logger.error("Failed to cleanup old notifications: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Health check", description = "Check notification service health")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service is healthy"),
            @ApiResponse(responseCode = "503", description = "Service is unhealthy")
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.debug("Notification service health check");

        try {
            Map<String, Object> health = Map.of(
                    "status", "UP",
                    "service", "notification-service",
                    "timestamp", LocalDateTime.now(),
                    "version", "1.0.0"
            );

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage());
            Map<String, Object> health = Map.of(
                    "status", "DOWN",
                    "service", "notification-service",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
}
