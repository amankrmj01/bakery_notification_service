package com.shah_s.bakery_notification_service.controller;

import com.shah_s.bakery_notification_service.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@Validated
@Tag(name = "Admin", description = "System administration and monitoring APIs")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private DeviceTokenService deviceTokenService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private PushNotificationService pushNotificationService;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port}")
    private String serverPort;

    @Operation(summary = "Get system overview", description = "Get comprehensive system status and statistics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "System overview retrieved successfully")
    })
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSystemOverview(
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Getting system overview: requester={}", requestingUserId);

        try {
            Map<String, Object> overview = new HashMap<>();

            // System information
            overview.put("system", Map.of(
                    "applicationName", applicationName,
                    "serverPort", serverPort,
                    "timestamp", LocalDateTime.now(),
                    "version", "1.0.0",
                    "environment", "production"
            ));

            // Service health status
            Map<String, Object> serviceHealth = new HashMap<>();
            serviceHealth.put("email", emailService.getEmailServiceHealth());
            serviceHealth.put("sms", smsService.getSmsServiceHealth());
            serviceHealth.put("push", pushNotificationService.getPushServiceHealth());
            overview.put("serviceHealth", serviceHealth);

            // Quick statistics (last 24 hours)
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            LocalDateTime now = LocalDateTime.now();

            try {
                Map<String, Object> stats = notificationService.getNotificationStatistics(yesterday, now);
                overview.put("last24Hours", stats);
            } catch (Exception e) {
                logger.warn("Failed to get 24h statistics: {}", e.getMessage());
                overview.put("last24Hours", Map.of("error", "Statistics unavailable"));
            }

            // Template statistics
            try {
                Map<String, Object> templateStats = templateService.getTemplateStatistics();
                overview.put("templates", templateStats);
            } catch (Exception e) {
                logger.warn("Failed to get template statistics: {}", e.getMessage());
                overview.put("templates", Map.of("error", "Template statistics unavailable"));
            }

            // Device token statistics
            try {
                Map<String, Object> deviceStats = deviceTokenService.getDeviceTokenStatistics();
                overview.put("devices", deviceStats);
            } catch (Exception e) {
                logger.warn("Failed to get device statistics: {}", e.getMessage());
                overview.put("devices", Map.of("error", "Device statistics unavailable"));
            }

            return ResponseEntity.ok(overview);

        } catch (Exception e) {
            logger.error("Failed to get system overview: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get comprehensive statistics", description = "Get detailed statistics for all services")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getComprehensiveStatistics(
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime startDate,
            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime endDate,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Getting comprehensive statistics: startDate={}, endDate={}, requester={}",
                startDate, endDate, requestingUserId);

        try {
            Map<String, Object> allStats = new HashMap<>();

            // Notification statistics
            try {
                Map<String, Object> notificationStats = notificationService.getNotificationStatistics(startDate, endDate);
                allStats.put("notifications", notificationStats);
            } catch (Exception e) {
                logger.warn("Failed to get notification statistics: {}", e.getMessage());
                allStats.put("notifications", Map.of("error", e.getMessage()));
            }

            // Campaign statistics
            try {
                Map<String, Object> campaignStats = campaignService.getCampaignStatistics(startDate, endDate);
                allStats.put("campaigns", campaignStats);
            } catch (Exception e) {
                logger.warn("Failed to get campaign statistics: {}", e.getMessage());
                allStats.put("campaigns", Map.of("error", e.getMessage()));
            }

            // Template statistics
            try {
                Map<String, Object> templateStats = templateService.getTemplateStatistics();
                allStats.put("templates", templateStats);
            } catch (Exception e) {
                logger.warn("Failed to get template statistics: {}", e.getMessage());
                allStats.put("templates", Map.of("error", e.getMessage()));
            }

            // Device token statistics
            try {
                Map<String, Object> deviceStats = deviceTokenService.getDeviceTokenStatistics();
                allStats.put("devices", deviceStats);
            } catch (Exception e) {
                logger.warn("Failed to get device statistics: {}", e.getMessage());
                allStats.put("devices", Map.of("error", e.getMessage()));
            }

            // Add metadata
            allStats.put("dateRange", Map.of(
                    "startDate", startDate,
                    "endDate", endDate,
                    "generatedAt", LocalDateTime.now(),
                    "generatedBy", requestingUserId
            ));

            return ResponseEntity.ok(allStats);

        } catch (Exception e) {
            logger.error("Failed to get comprehensive statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Test email service", description = "Test email service connectivity and functionality")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email test completed"),
            @ApiResponse(responseCode = "500", description = "Email test failed")
    })
    @PostMapping("/test/email")
    public ResponseEntity<Map<String, Object>> testEmailService(
            @Parameter(description = "Test email address")
            @RequestParam(required = false) String testEmail,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Testing email service: testEmail={}, requester={}", testEmail, requestingUserId);

        try {
            boolean connectivityTest = emailService.testEmailConnection();

            Map<String, Object> result = new HashMap<>();
            result.put("service", "email");
            result.put("connectivity", connectivityTest);
            result.put("timestamp", LocalDateTime.now());
            result.put("testedBy", requestingUserId);

            if (testEmail != null && connectivityTest) {
                try {
                    emailService.sendTemplatedEmail(
                            testEmail,
                            "Test User",
                            "Email Service Test",
                            "test-email",
                            Map.of("testTime", LocalDateTime.now())
                    );
                    result.put("testEmail", "sent");
                    result.put("testEmailAddress", testEmail);
                } catch (Exception e) {
                    result.put("testEmail", "failed");
                    result.put("testEmailError", e.getMessage());
                }
            }

            result.put("health", emailService.getEmailServiceHealth());

            HttpStatus status = connectivityTest ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(result);

        } catch (Exception e) {
            logger.error("Email service test failed: {}", e.getMessage());
            Map<String, Object> errorResult = Map.of(
                    "service", "email",
                    "status", "ERROR",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    @Operation(summary = "Test SMS service", description = "Test SMS service connectivity and functionality")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SMS test completed"),
            @ApiResponse(responseCode = "500", description = "SMS test failed")
    })
    @PostMapping("/test/sms")
    public ResponseEntity<Map<String, Object>> testSmsService(
            @Parameter(description = "Test phone number")
            @RequestParam(required = false) String testPhone,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Testing SMS service: testPhone={}, requester={}",
                testPhone != null ? "***" + testPhone.substring(Math.max(0, testPhone.length() - 4)) : null,
                requestingUserId);

        try {
            boolean connectivityTest = smsService.testSmsConnection();

            Map<String, Object> result = new HashMap<>();
            result.put("service", "sms");
            result.put("connectivity", connectivityTest);
            result.put("timestamp", LocalDateTime.now());
            result.put("testedBy", requestingUserId);

            if (testPhone != null && connectivityTest) {
                try {
                    if (smsService.isValidPhoneNumber(testPhone)) {
                        String formattedPhone = smsService.formatPhoneNumber(testPhone);
                        String testMessage = String.format("SMS Service Test - %s", LocalDateTime.now());

                        String messageId = smsService.sendSms(formattedPhone, testMessage);
                        result.put("testSms", "sent");
                        result.put("testPhoneNumber", "***" + testPhone.substring(Math.max(0, testPhone.length() - 4)));
                        result.put("messageId", messageId);
                    } else {
                        result.put("testSms", "failed");
                        result.put("testSmsError", "Invalid phone number format");
                    }
                } catch (Exception e) {
                    result.put("testSms", "failed");
                    result.put("testSmsError", e.getMessage());
                }
            }

            result.put("health", smsService.getSmsServiceHealth());

            HttpStatus status = connectivityTest ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(result);

        } catch (Exception e) {
            logger.error("SMS service test failed: {}", e.getMessage());
            Map<String, Object> errorResult = Map.of(
                    "service", "sms",
                    "status", "ERROR",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    @Operation(summary = "Test push notification service", description = "Test push notification service functionality")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Push notification test completed"),
            @ApiResponse(responseCode = "500", description = "Push notification test failed")
    })
    @PostMapping("/test/push")
    public ResponseEntity<Map<String, Object>> testPushService(
            @Parameter(description = "Test endpoint ARN")
            @RequestParam(required = false) String testEndpointArn,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Testing push notification service: requester={}", requestingUserId);

        try {
            Map<String, Object> result = new HashMap<>();
            result.put("service", "push");
            result.put("timestamp", LocalDateTime.now());
            result.put("testedBy", requestingUserId);

            if (testEndpointArn != null) {
                try {
                    pushNotificationService.sendTestPushNotification(testEndpointArn);
                    result.put("testPush", "sent");
                    result.put("testEndpoint", "***" + testEndpointArn.substring(Math.max(0, testEndpointArn.length() - 8)));
                } catch (Exception e) {
                    result.put("testPush", "failed");
                    result.put("testPushError", e.getMessage());
                }
            }

            result.put("health", pushNotificationService.getPushServiceHealth());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Push notification service test failed: {}", e.getMessage());
            Map<String, Object> errorResult = Map.of(
                    "service", "push",
                    "status", "ERROR",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    @Operation(summary = "Run system maintenance", description = "Execute system maintenance tasks")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Maintenance tasks started"),
            @ApiResponse(responseCode = "500", description = "Failed to start maintenance")
    })
    @PostMapping("/maintenance")
    public ResponseEntity<Map<String, Object>> runSystemMaintenance(
            @Parameter(description = "Maintenance tasks to run")
            @RequestParam(defaultValue = "all") String tasks,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Running system maintenance: tasks={}, requester={}", tasks, requestingUserId);

        try {
            Map<String, Object> result = new HashMap<>();
            result.put("maintenanceStarted", LocalDateTime.now());
            result.put("startedBy", requestingUserId);
            result.put("tasks", new HashMap<String, String>());

            @SuppressWarnings("unchecked")
            Map<String, String> taskResults = (Map<String, String>) result.get("tasks");

            if ("all".equals(tasks) || tasks.contains("notifications")) {
                try {
                    notificationService.cleanupOldNotifications();
                    notificationService.processPendingNotifications();
                    notificationService.retryFailedNotifications();
                    taskResults.put("notifications", "started");
                } catch (Exception e) {
                    taskResults.put("notifications", "failed: " + e.getMessage());
                }
            }

            if ("all".equals(tasks) || tasks.contains("devices")) {
                try {
                    deviceTokenService.cleanupDeviceTokens();
                    deviceTokenService.validateDeviceTokens();
                    taskResults.put("devices", "started");
                } catch (Exception e) {
                    taskResults.put("devices", "failed: " + e.getMessage());
                }
            }

            if ("all".equals(tasks) || tasks.contains("campaigns")) {
                try {
                    campaignService.cleanupOldCampaigns();
                    taskResults.put("campaigns", "started");
                } catch (Exception e) {
                    taskResults.put("campaigns", "failed: " + e.getMessage());
                }
            }

            result.put("status", "MAINTENANCE_STARTED");
            result.put("message", "System maintenance tasks have been initiated");

            return ResponseEntity.accepted().body(result);

        } catch (Exception e) {
            logger.error("Failed to run system maintenance: {}", e.getMessage());
            Map<String, Object> errorResult = Map.of(
                    "status", "MAINTENANCE_FAILED",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    @Operation(summary = "Send test notification", description = "Send a test notification to verify system functionality")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Test notification sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid test parameters")
    })
    @PostMapping("/test/notification")
    public ResponseEntity<Map<String, Object>> sendTestNotification(
            @Parameter(description = "Notification type (EMAIL, SMS, PUSH)")
            @RequestParam String type,
            @Parameter(description = "Recipient (email, phone, or endpoint ARN)")
            @RequestParam String recipient,
            @Parameter(description = "Test message")
            @RequestParam(defaultValue = "This is a test notification from Shah's Bakery notification service") String message,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Sending test notification: type={}, requester={}", type, requestingUserId);

        try {
            Map<String, Object> result = new HashMap<>();
            result.put("testType", type);
            result.put("timestamp", LocalDateTime.now());
            result.put("sentBy", requestingUserId);

            switch (type.toUpperCase()) {
                case "EMAIL" -> {
                    emailService.sendTemplatedEmail(
                            recipient,
                            "Test User",
                            "Test Notification",
                            "test-email",
                            Map.of("message", message, "timestamp", LocalDateTime.now())
                    );
                    result.put("status", "sent");
                    result.put("recipient", recipient);
                }
                case "SMS" -> {
                    String messageId = smsService.sendSms(recipient, message);
                    result.put("status", "sent");
                    result.put("recipient", "***" + recipient.substring(Math.max(0, recipient.length() - 4)));
                    result.put("messageId", messageId);
                }
                case "PUSH" -> {
                    pushNotificationService.sendTestPushNotification(recipient);
                    result.put("status", "sent");
                    result.put("recipient", "***" + recipient.substring(Math.max(0, recipient.length() - 8)));
                }
                default -> {
                    result.put("status", "failed");
                    result.put("error", "Invalid notification type: " + type);
                    return ResponseEntity.badRequest().body(result);
                }
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (Exception e) {
            logger.error("Failed to send test notification: {}", e.getMessage());
            Map<String, Object> errorResult = Map.of(
                    "status", "failed",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    @Operation(summary = "Get service health", description = "Get health status of all notification services")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service health retrieved successfully")
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getServiceHealth() {
        logger.debug("Getting service health");

        try {
            Map<String, Object> health = new HashMap<>();

            // Overall system health
            health.put("system", Map.of(
                    "status", "UP",
                    "application", applicationName,
                    "port", serverPort,
                    "timestamp", LocalDateTime.now()
            ));

            // Individual service health
            Map<String, Object> services = new HashMap<>();

            try {
                services.put("email", emailService.getEmailServiceHealth());
            } catch (Exception e) {
                services.put("email", Map.of("status", "DOWN", "error", e.getMessage()));
            }

            try {
                services.put("sms", smsService.getSmsServiceHealth());
            } catch (Exception e) {
                services.put("sms", Map.of("status", "DOWN", "error", e.getMessage()));
            }

            try {
                services.put("push", pushNotificationService.getPushServiceHealth());
            } catch (Exception e) {
                services.put("push", Map.of("status", "DOWN", "error", e.getMessage()));
            }

            health.put("services", services);

            // Determine overall health
            boolean allHealthy = services.values().stream()
                    .allMatch(service -> {
                        if (service instanceof Map) {
                            Object enabled = ((Map<?, ?>) service).get("enabled");
                            Object connectivity = ((Map<?, ?>) service).get("connectivity");
                            return enabled == Boolean.FALSE || connectivity == Boolean.TRUE;
                        }
                        return true;
                    });

            health.put("overallStatus", allHealthy ? "HEALTHY" : "DEGRADED");

            HttpStatus status = allHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(health);

        } catch (Exception e) {
            logger.error("Failed to get service health: {}", e.getMessage());
            Map<String, Object> errorHealth = Map.of(
                    "overallStatus", "DOWN",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorHealth);
        }
    }

    @Operation(summary = "Get system info", description = "Get detailed system information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "System information retrieved successfully")
    })
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        logger.debug("Getting system information");

        try {
            Map<String, Object> info = new HashMap<>();

            // Application info
            info.put("application", Map.of(
                    "name", applicationName,
                    "version", "1.0.0",
                    "port", serverPort,
                    "profile", "production",
                    "startTime", "N/A", // Would need to track this
                    "timestamp", LocalDateTime.now()
            ));

            // Java runtime info
            Runtime runtime = Runtime.getRuntime();
            info.put("runtime", Map.of(
                    "javaVersion", System.getProperty("java.version"),
                    "javaVendor", System.getProperty("java.vendor"),
                    "totalMemory", runtime.totalMemory(),
                    "freeMemory", runtime.freeMemory(),
                    "maxMemory", runtime.maxMemory(),
                    "availableProcessors", runtime.availableProcessors()
            ));

            // Operating system info
            info.put("os", Map.of(
                    "name", System.getProperty("os.name"),
                    "version", System.getProperty("os.version"),
                    "architecture", System.getProperty("os.arch")
            ));

            // Service configuration
            Map<String, Object> serviceConfig = new HashMap<>();
            serviceConfig.put("email", emailService.getEmailServiceHealth());
            serviceConfig.put("sms", smsService.getSmsServiceHealth());
            serviceConfig.put("push", pushNotificationService.getPushServiceHealth());
            info.put("services", serviceConfig);

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            logger.error("Failed to get system info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Force garbage collection", description = "Manually trigger garbage collection")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Garbage collection triggered")
    })
    @PostMapping("/gc")
    public ResponseEntity<Map<String, Object>> forceGarbageCollection(
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Forcing garbage collection: requester={}", requestingUserId);

        try {
            Runtime runtime = Runtime.getRuntime();
            long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

            System.gc();

            // Give GC a moment to work
            Thread.sleep(100);

            long afterMemory = runtime.totalMemory() - runtime.freeMemory();
            long freedMemory = beforeMemory - afterMemory;

            Map<String, Object> result = Map.of(
                    "status", "completed",
                    "memoryBefore", beforeMemory,
                    "memoryAfter", afterMemory,
                    "freedMemory", freedMemory,
                    "timestamp", LocalDateTime.now(),
                    "requestedBy", requestingUserId
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to force garbage collection: {}", e.getMessage());
            Map<String, Object> errorResult = Map.of(
                    "status", "failed",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
}
