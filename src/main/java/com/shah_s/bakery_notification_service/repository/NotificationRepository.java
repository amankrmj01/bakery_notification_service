package com.shah_s.bakery_notification_service.repository;

import com.shah_s.bakery_notification_service.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Find notifications by user
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Find notifications by status
    List<Notification> findByStatusOrderByCreatedAtDesc(Notification.NotificationStatus status);

    Page<Notification> findByStatusOrderByCreatedAtDesc(Notification.NotificationStatus status, Pageable pageable);

    // Find notifications by type
    List<Notification> findByTypeOrderByCreatedAtDesc(Notification.NotificationType type);

    Page<Notification> findByTypeOrderByCreatedAtDesc(Notification.NotificationType type, Pageable pageable);

    // Find notifications by priority
    List<Notification> findByPriorityOrderByCreatedAtDesc(Notification.NotificationPriority priority);

    // Find notifications by template
    List<Notification> findByTemplateIdOrderByCreatedAtDesc(UUID templateId);

    // Find notifications by campaign
    List<Notification> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId);

    Page<Notification> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId, Pageable pageable);

    // Find notifications by user and status
    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, Notification.NotificationStatus status);

    // Find notifications by user and type
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, Notification.NotificationType type);

    // Find pending notifications (ready to send)
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' " +
           "AND (n.scheduledAt IS NULL OR n.scheduledAt <= :now) " +
           "AND (n.expiresAt IS NULL OR n.expiresAt > :now)")
    List<Notification> findPendingNotifications(@Param("now") LocalDateTime now);

    // Find scheduled notifications (for specific time)
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' " +
           "AND n.scheduledAt IS NOT NULL " +
           "AND n.scheduledAt BETWEEN :startTime AND :endTime")
    List<Notification> findScheduledNotifications(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    // Find failed notifications that can be retried
    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' " +
           "AND n.retryCount < n.maxRetryCount " +
           "AND (n.expiresAt IS NULL OR n.expiresAt > :now) " +
           "AND n.lastErrorAt < :retryAfter")
    List<Notification> findRetryableNotifications(@Param("now") LocalDateTime now,
                                                 @Param("retryAfter") LocalDateTime retryAfter);

    // Find expired notifications
    @Query("SELECT n FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt <= :now " +
           "AND n.status NOT IN ('DELIVERED', 'CANCELLED')")
    List<Notification> findExpiredNotifications(@Param("now") LocalDateTime now);

    // Find notifications by related entity
    List<Notification> findByRelatedEntityTypeAndRelatedEntityIdOrderByCreatedAtDesc(
            String relatedEntityType, UUID relatedEntityId);

    // Find notifications by date range
    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    Page<Notification> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate,
                                                                 LocalDateTime endDate,
                                                                 Pageable pageable);

    // Find notifications by recipient email
    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);

    // Find notifications by recipient phone
    List<Notification> findByRecipientPhoneOrderByCreatedAtDesc(String recipientPhone);

    // Find notifications by SNS endpoint ARN
    List<Notification> findBySnsEndpointArnOrderByCreatedAtDesc(String snsEndpointArn);

    // Find notifications by source
    List<Notification> findBySourceOrderByCreatedAtDesc(String source);

    // Count notifications by status
    Long countByStatus(Notification.NotificationStatus status);

    // Count notifications by type
    Long countByType(Notification.NotificationType type);

    // Count notifications by user
    Long countByUserId(UUID userId);

    // Count notifications by campaign
    Long countByCampaignId(UUID campaignId);

    // Count notifications by date range
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate")
    Long countByDateRange(@Param("startDate") LocalDateTime startDate,
                         @Param("endDate") LocalDateTime endDate);

    // Statistics queries
    @Query("SELECT n.status, COUNT(n) FROM Notification n " +
           "WHERE n.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY n.status")
    List<Object[]> getNotificationStatisticsByStatus(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT n.type, COUNT(n) FROM Notification n " +
           "WHERE n.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY n.type")
    List<Object[]> getNotificationStatisticsByType(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT DATE(n.createdAt), n.status, COUNT(n) FROM Notification n " +
           "WHERE n.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(n.createdAt), n.status " +
           "ORDER BY DATE(n.createdAt)")
    List<Object[]> getDailyNotificationStatistics(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    // Delivery statistics
    @Query("SELECT " +
           "COUNT(CASE WHEN n.status = 'SENT' THEN 1 END) as sent, " +
           "COUNT(CASE WHEN n.status = 'DELIVERED' THEN 1 END) as delivered, " +
           "COUNT(CASE WHEN n.status = 'FAILED' THEN 1 END) as failed, " +
           "COUNT(CASE WHEN n.status = 'BOUNCED' THEN 1 END) as bounced, " +
           "COUNT(CASE WHEN n.openedAt IS NOT NULL THEN 1 END) as opened, " +
           "COUNT(CASE WHEN n.clickedAt IS NOT NULL THEN 1 END) as clicked " +
           "FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate")
    Object[] getDeliveryStatistics(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    // Campaign performance
    @Query("SELECT n.campaignId, " +
           "COUNT(n) as totalSent, " +
           "COUNT(CASE WHEN n.status = 'DELIVERED' THEN 1 END) as delivered, " +
           "COUNT(CASE WHEN n.openedAt IS NOT NULL THEN 1 END) as opened, " +
           "COUNT(CASE WHEN n.clickedAt IS NOT NULL THEN 1 END) as clicked " +
           "FROM Notification n " +
           "WHERE n.campaignId IS NOT NULL AND n.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY n.campaignId")
    List<Object[]> getCampaignPerformanceStatistics(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    // Update operations
    @Modifying
    @Query("UPDATE Notification n SET n.status = 'CANCELLED' " +
           "WHERE n.id = :notificationId AND n.status = 'PENDING'")
    int cancelNotification(@Param("notificationId") UUID notificationId);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'CANCELLED' " +
           "WHERE n.campaignId = :campaignId AND n.status = 'PENDING'")
    int cancelNotificationsByCampaign(@Param("campaignId") UUID campaignId);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'CANCELLED' " +
           "WHERE n.status = 'PENDING' AND n.expiresAt <= :now")
    int cancelExpiredNotifications(@Param("now") LocalDateTime now);

    // Cleanup operations
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.status IN ('DELIVERED', 'CANCELLED') " +
           "AND n.createdAt < :cutoffDate")
    int cleanupOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.status = 'FAILED' " +
           "AND n.retryCount >= n.maxRetryCount " +
           "AND n.lastErrorAt < :cutoffDate")
    int cleanupFailedNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    // User activity tracking
    @Query("SELECT DISTINCT n.userId FROM Notification n " +
           "WHERE n.userId IS NOT NULL AND n.createdAt BETWEEN :startDate AND :endDate")
    List<UUID> findActiveUserIds(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    // Most engaged users (by opens/clicks)
    @Query("SELECT n.userId, " +
           "COUNT(CASE WHEN n.openedAt IS NOT NULL THEN 1 END) as opens, " +
           "COUNT(CASE WHEN n.clickedAt IS NOT NULL THEN 1 END) as clicks " +
           "FROM Notification n " +
           "WHERE n.userId IS NOT NULL AND n.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY n.userId " +
           "HAVING COUNT(CASE WHEN n.openedAt IS NOT NULL THEN 1 END) > 0 " +
           "ORDER BY opens DESC, clicks DESC")
    List<Object[]> findMostEngagedUsers(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       Pageable pageable);

    // Bounce tracking
    @Query("SELECT n.recipientEmail, COUNT(n) as bounceCount " +
           "FROM Notification n " +
           "WHERE n.status = 'BOUNCED' AND n.recipientEmail IS NOT NULL " +
           "AND n.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY n.recipientEmail " +
           "HAVING COUNT(n) >= :minBounces " +
           "ORDER BY bounceCount DESC")
    List<Object[]> findBouncedEmails(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate,
                                    @Param("minBounces") Integer minBounces);

    // Template usage statistics
    @Query("SELECT n.templateId, COUNT(n) as usageCount " +
           "FROM Notification n " +
           "WHERE n.templateId IS NOT NULL AND n.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY n.templateId " +
           "ORDER BY usageCount DESC")
    List<Object[]> getTemplateUsageStatistics(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    // Find duplicate notifications (same user, type, content within timeframe)
    @Query("SELECT n FROM Notification n WHERE EXISTS (" +
           "SELECT 1 FROM Notification n2 WHERE n2.userId = n.userId " +
           "AND n2.type = n.type AND n2.title = n.title " +
           "AND n2.id != n.id " +
           "AND n2.createdAt BETWEEN :startTime AND :endTime" +
           ") AND n.createdAt BETWEEN :startTime AND :endTime")
    List<Notification> findDuplicateNotifications(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    // Performance monitoring
    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, n.createdAt, n.sentAt)) " +
           "FROM Notification n " +
           "WHERE n.sentAt IS NOT NULL AND n.createdAt BETWEEN :startDate AND :endDate")
    Double getAverageDeliveryTime(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    // Error analysis
    @Query("SELECT n.errorCode, n.errorMessage, COUNT(n) as errorCount " +
           "FROM Notification n " +
           "WHERE n.status = 'FAILED' AND n.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY n.errorCode, n.errorMessage " +
           "ORDER BY errorCount DESC")
    List<Object[]> getErrorAnalysis(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    // Recent notifications for user
    @Query("SELECT n FROM Notification n " +
           "WHERE n.userId = :userId " +
           "ORDER BY n.createdAt DESC " +
           "LIMIT :limit")
    List<Notification> findRecentNotificationsByUser(@Param("userId") UUID userId,
                                                    @Param("limit") Integer limit);

    // Check if notification exists for user and content
    @Query("SELECT COUNT(n) > 0 FROM Notification n " +
           "WHERE n.userId = :userId AND n.title = :title AND n.content = :content " +
           "AND n.createdAt > :since")
    Boolean existsByUserAndContentSince(@Param("userId") UUID userId,
                                       @Param("title") String title,
                                       @Param("content") String content,
                                       @Param("since") LocalDateTime since);

    // Find notifications needing webhook callbacks
    @Query("SELECT n FROM Notification n " +
           "WHERE n.status IN ('DELIVERED', 'FAILED', 'BOUNCED') " +
           "AND n.trackingData IS NOT NULL " +
           "AND n.trackingData LIKE '%webhook%' " +
           "AND n.updatedAt > :since")
    List<Notification> findNotificationsNeedingWebhookCallback(@Param("since") LocalDateTime since);
}
