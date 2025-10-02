package com.shah_s.bakery_notification_service.repository;

import com.shah_s.bakery_notification_service.entity.NotificationCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationCampaignRepository extends JpaRepository<NotificationCampaign, UUID> {

    // Find by name
    Optional<NotificationCampaign> findByName(String name);

    // Find by status
    List<NotificationCampaign> findByStatusOrderByCreatedAtDesc(NotificationCampaign.CampaignStatus status);

    Page<NotificationCampaign> findByStatusOrderByCreatedAtDesc(NotificationCampaign.CampaignStatus status,
                                                               Pageable pageable);

    // Find by campaign type
    List<NotificationCampaign> findByCampaignTypeOrderByCreatedAtDesc(NotificationCampaign.CampaignType campaignType);

    Page<NotificationCampaign> findByCampaignTypeOrderByCreatedAtDesc(NotificationCampaign.CampaignType campaignType,
                                                                     Pageable pageable);

    // Find active campaigns
    List<NotificationCampaign> findByIsActiveTrueOrderByCreatedAtDesc();

    Page<NotificationCampaign> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    // Find campaigns by template
    List<NotificationCampaign> findByTemplateIdOrderByCreatedAtDesc(UUID templateId);

    // Find campaigns by creator
    List<NotificationCampaign> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    // Find scheduled campaigns
    @Query("SELECT c FROM NotificationCampaign c WHERE c.status = 'SCHEDULED' " +
           "AND c.scheduledStartAt IS NOT NULL " +
           "AND c.scheduledStartAt <= :now")
    List<NotificationCampaign> findCampaignsReadyToStart(@Param("now") LocalDateTime now);

    // Find campaigns to complete (past end time)
    @Query("SELECT c FROM NotificationCampaign c WHERE c.status = 'RUNNING' " +
           "AND c.scheduledEndAt IS NOT NULL " +
           "AND c.scheduledEndAt <= :now")
    List<NotificationCampaign> findCampaignsToComplete(@Param("now") LocalDateTime now);

    // Find running campaigns
    List<NotificationCampaign> findByStatusAndIsActiveTrueOrderByStartedAtDesc(
            NotificationCampaign.CampaignStatus status);

    // Find recurring campaigns
    List<NotificationCampaign> findByIsRecurringTrueAndIsActiveTrueOrderByCreatedAtDesc();

    // Find campaigns by date range
    List<NotificationCampaign> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate,
                                                                         LocalDateTime endDate);

    // Find campaigns by scheduled date range
    @Query("SELECT c FROM NotificationCampaign c WHERE c.scheduledStartAt BETWEEN :startDate AND :endDate " +
           "ORDER BY c.scheduledStartAt ASC")
    List<NotificationCampaign> findByScheduledDateRange(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    // Find A/B test campaigns
    List<NotificationCampaign> findByIsAbTestTrueOrderByCreatedAtDesc();

    // Find campaigns within budget
    @Query("SELECT c FROM NotificationCampaign c WHERE c.budgetLimit IS NOT NULL " +
           "AND c.totalCost < c.budgetLimit " +
           "ORDER BY c.createdAt DESC")
    List<NotificationCampaign> findCampaignsWithinBudget();

    // Find campaigns over budget
    @Query("SELECT c FROM NotificationCampaign c WHERE c.budgetLimit IS NOT NULL " +
           "AND c.totalCost >= c.budgetLimit " +
           "ORDER BY c.totalCost DESC")
    List<NotificationCampaign> findCampaignsOverBudget();

    // Search campaigns
    @Query("SELECT c FROM NotificationCampaign c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY c.createdAt DESC")
    List<NotificationCampaign> searchCampaigns(@Param("searchTerm") String searchTerm);

    Page<NotificationCampaign> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name, String description, Pageable pageable);

    // Count campaigns by status
    Long countByStatus(NotificationCampaign.CampaignStatus status);

    // Count campaigns by type
    Long countByCampaignType(NotificationCampaign.CampaignType campaignType);

    // Count active campaigns
    Long countByIsActiveTrue();

    // Count campaigns by creator
    Long countByCreatedBy(String createdBy);

    // Statistics queries
    @Query("SELECT c.status, COUNT(c) FROM NotificationCampaign c " +
           "WHERE c.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY c.status")
    List<Object[]> getCampaignStatisticsByStatus(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c.campaignType, COUNT(c) FROM NotificationCampaign c " +
           "WHERE c.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY c.campaignType")
    List<Object[]> getCampaignStatisticsByType(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    @Query("SELECT DATE(c.createdAt), COUNT(c) FROM NotificationCampaign c " +
           "WHERE c.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(c.createdAt) " +
           "ORDER BY DATE(c.createdAt)")
    List<Object[]> getDailyCampaignStatistics(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    // Performance statistics
    @Query("SELECT " +
           "AVG(c.sentCount) as avgSent, " +
           "AVG(c.deliveredCount) as avgDelivered, " +
           "AVG(c.openedCount) as avgOpened, " +
           "AVG(c.clickedCount) as avgClicked, " +
           "AVG(CASE WHEN c.sentCount > 0 THEN (c.deliveredCount * 100.0 / c.sentCount) END) as avgDeliveryRate, " +
           "AVG(CASE WHEN c.deliveredCount > 0 THEN (c.openedCount * 100.0 / c.deliveredCount) END) as avgOpenRate, " +
           "AVG(CASE WHEN c.deliveredCount > 0 THEN (c.clickedCount * 100.0 / c.deliveredCount) END) as avgClickRate " +
           "FROM NotificationCampaign c " +
           "WHERE c.status = 'COMPLETED' AND c.createdAt BETWEEN :startDate AND :endDate")
    Object[] getCampaignPerformanceStatistics(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    // Top performing campaigns
    @Query("SELECT c.id, c.name, c.campaignType, " +
           "CASE WHEN c.deliveredCount > 0 THEN (c.openedCount * 100.0 / c.deliveredCount) ELSE 0 END as openRate, " +
           "CASE WHEN c.deliveredCount > 0 THEN (c.clickedCount * 100.0 / c.deliveredCount) ELSE 0 END as clickRate " +
           "FROM NotificationCampaign c " +
           "WHERE c.status = 'COMPLETED' AND c.deliveredCount > 0 " +
           "ORDER BY openRate DESC, clickRate DESC")
    List<Object[]> getTopPerformingCampaigns(Pageable pageable);

    // Budget analysis
    @Query("SELECT " +
           "SUM(c.totalCost) as totalSpent, " +
           "AVG(c.totalCost) as avgCost, " +
           "SUM(c.budgetLimit) as totalBudget, " +
           "COUNT(CASE WHEN c.totalCost >= c.budgetLimit THEN 1 END) as overBudgetCount " +
           "FROM NotificationCampaign c " +
           "WHERE c.createdAt BETWEEN :startDate AND :endDate " +
           "AND c.budgetLimit IS NOT NULL")
    Object[] getBudgetAnalysis(@Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);

    // A/B test results
    @Query("SELECT c.abVariant, " +
           "AVG(CASE WHEN c.deliveredCount > 0 THEN (c.openedCount * 100.0 / c.deliveredCount) END) as avgOpenRate, " +
           "AVG(CASE WHEN c.deliveredCount > 0 THEN (c.clickedCount * 100.0 / c.deliveredCount) END) as avgClickRate, " +
           "COUNT(c) as campaignCount " +
           "FROM NotificationCampaign c " +
           "WHERE c.isAbTest = true AND c.status = 'COMPLETED' " +
           "AND c.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY c.abVariant")
    List<Object[]> getAbTestResults(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    // Update operations
    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.status = 'RUNNING', c.startedAt = :now " +
           "WHERE c.id = :campaignId AND c.status IN ('DRAFT', 'SCHEDULED')")
    int startCampaign(@Param("campaignId") UUID campaignId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.status = 'COMPLETED', c.completedAt = :now " +
           "WHERE c.id = :campaignId AND c.status = 'RUNNING'")
    int completeCampaign(@Param("campaignId") UUID campaignId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.status = 'CANCELLED', c.cancelledAt = :now " +
           "WHERE c.id = :campaignId AND c.status IN ('DRAFT', 'SCHEDULED', 'RUNNING')")
    int cancelCampaign(@Param("campaignId") UUID campaignId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.status = 'PAUSED' " +
           "WHERE c.id = :campaignId AND c.status = 'RUNNING'")
    int pauseCampaign(@Param("campaignId") UUID campaignId);

    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.status = 'RUNNING' " +
           "WHERE c.id = :campaignId AND c.status = 'PAUSED'")
    int resumeCampaign(@Param("campaignId") UUID campaignId);

    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.isActive = false " +
           "WHERE c.id = :campaignId")
    int deactivateCampaign(@Param("campaignId") UUID campaignId);

    // Increment counters
    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.sentCount = c.sentCount + 1 " +
           "WHERE c.id = :campaignId")
    int incrementSentCount(@Param("campaignId") UUID campaignId);

    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.deliveredCount = c.deliveredCount + 1 " +
           "WHERE c.id = :campaignId")
    int incrementDeliveredCount(@Param("campaignId") UUID campaignId);

    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.failedCount = c.failedCount + 1 " +
           "WHERE c.id = :campaignId")
    int incrementFailedCount(@Param("campaignId") UUID campaignId);

    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.openedCount = c.openedCount + 1 " +
           "WHERE c.id = :campaignId")
    int incrementOpenedCount(@Param("campaignId") UUID campaignId);

    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.clickedCount = c.clickedCount + 1 " +
           "WHERE c.id = :campaignId")
    int incrementClickedCount(@Param("campaignId") UUID campaignId);

    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.bouncedCount = c.bouncedCount + 1 " +
           "WHERE c.id = :campaignId")
    int incrementBouncedCount(@Param("campaignId") UUID campaignId);

    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.unsubscribedCount = c.unsubscribedCount + 1 " +
           "WHERE c.id = :campaignId")
    int incrementUnsubscribedCount(@Param("campaignId") UUID campaignId);

    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.totalCost = c.totalCost + :additionalCost " +
           "WHERE c.id = :campaignId")
    int addToCost(@Param("campaignId") UUID campaignId, @Param("additionalCost") BigDecimal additionalCost);

    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.totalRecipients = :totalRecipients " +
           "WHERE c.id = :campaignId")
    int updateTotalRecipients(@Param("campaignId") UUID campaignId,
                             @Param("totalRecipients") Integer totalRecipients);

    // Bulk operations
    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.status = 'COMPLETED', c.completedAt = :now " +
           "WHERE c.status = 'RUNNING' AND c.scheduledEndAt IS NOT NULL AND c.scheduledEndAt <= :now")
    int completeExpiredCampaigns(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE NotificationCampaign c SET c.status = 'RUNNING', c.startedAt = :now " +
           "WHERE c.status = 'SCHEDULED' AND c.scheduledStartAt IS NOT NULL AND c.scheduledStartAt <= :now")
    int startScheduledCampaigns(@Param("now") LocalDateTime now);

    // Validation queries
    @Query("SELECT COUNT(c) > 0 FROM NotificationCampaign c " +
           "WHERE c.name = :name AND c.id != :excludeId")
    Boolean existsByNameAndNotId(@Param("name") String name, @Param("excludeId") UUID excludeId);

    // Template usage tracking
    @Query("SELECT COUNT(c) FROM NotificationCampaign c " +
           "WHERE c.templateId = :templateId AND c.status != 'CANCELLED'")
    Long countActiveCampaignsByTemplate(@Param("templateId") UUID templateId);

    // Find campaigns needing attention
    @Query("SELECT c FROM NotificationCampaign c WHERE " +
           "(c.status = 'RUNNING' AND c.scheduledEndAt < :now) OR " +
           "(c.status = 'SCHEDULED' AND c.scheduledStartAt < :now) OR " +
           "(c.budgetLimit IS NOT NULL AND c.totalCost >= c.budgetLimit)")
    List<NotificationCampaign> findCampaignsNeedingAttention(@Param("now") LocalDateTime now);

    // Cleanup operations
    @Modifying
    @Query("DELETE FROM NotificationCampaign c WHERE c.status = 'CANCELLED' " +
           "AND c.cancelledAt < :cutoffDate")
    int cleanupCancelledCampaigns(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Performance monitoring
    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, c.startedAt, c.completedAt)) " +
           "FROM NotificationCampaign c " +
           "WHERE c.status = 'COMPLETED' AND c.startedAt IS NOT NULL AND c.completedAt IS NOT NULL " +
           "AND c.createdAt BETWEEN :startDate AND :endDate")
    Double getAverageCampaignDuration(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    // Recently completed campaigns
    @Query("SELECT c FROM NotificationCampaign c WHERE c.status = 'COMPLETED' " +
           "ORDER BY c.completedAt DESC")
    List<NotificationCampaign> findRecentlyCompletedCampaigns(Pageable pageable);

    // Failed campaigns analysis
    @Query("SELECT c FROM NotificationCampaign c WHERE c.status = 'FAILED' " +
           "OR (c.status = 'COMPLETED' AND c.deliveredCount = 0 AND c.sentCount > 0)")
    List<NotificationCampaign> findFailedCampaigns();
}
