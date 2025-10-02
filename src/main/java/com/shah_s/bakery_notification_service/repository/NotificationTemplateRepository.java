package com.shah_s.bakery_notification_service.repository;

import com.shah_s.bakery_notification_service.entity.NotificationTemplate;
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
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    // Find by name
    Optional<NotificationTemplate> findByName(String name);

    // Find by template type
    List<NotificationTemplate> findByTemplateTypeAndIsActiveTrue(NotificationTemplate.TemplateType templateType);

    Page<NotificationTemplate> findByTemplateTypeAndIsActiveTrue(NotificationTemplate.TemplateType templateType,
                                                                Pageable pageable);

    // Find active templates
    List<NotificationTemplate> findByIsActiveTrueOrderByNameAsc();

    Page<NotificationTemplate> findByIsActiveTrueOrderByNameAsc(Pageable pageable);

    // Find inactive templates
    List<NotificationTemplate> findByIsActiveFalseOrderByNameAsc();

    // Find default templates
    List<NotificationTemplate> findByIsDefaultTrueOrderByTemplateTypeAsc();

    // Find templates by category
    List<NotificationTemplate> findByCategoryAndIsActiveTrueOrderByNameAsc(String category);

    // Find templates by language
    List<NotificationTemplate> findByLanguageAndIsActiveTrueOrderByNameAsc(String language);

    // Find templates by type and language
    List<NotificationTemplate> findByTemplateTypeAndLanguageAndIsActiveTrueOrderByNameAsc(
            NotificationTemplate.TemplateType templateType, String language);

    // Find default template for type
    @Query("SELECT t FROM NotificationTemplate t " +
           "WHERE t.templateType = :templateType AND t.isDefault = true AND t.isActive = true")
    Optional<NotificationTemplate> findDefaultByTemplateType(
            @Param("templateType") NotificationTemplate.TemplateType templateType);

    // Find default template for type and language
    @Query("SELECT t FROM NotificationTemplate t " +
           "WHERE t.templateType = :templateType AND t.language = :language " +
           "AND t.isDefault = true AND t.isActive = true")
    Optional<NotificationTemplate> findDefaultByTemplateTypeAndLanguage(
            @Param("templateType") NotificationTemplate.TemplateType templateType,
            @Param("language") String language);

    // Find templates by created by
    List<NotificationTemplate> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    // Find templates by updated by
    List<NotificationTemplate> findByUpdatedByOrderByUpdatedAtDesc(String updatedBy);

    // Search templates by name or description
    @Query("SELECT t FROM NotificationTemplate t " +
           "WHERE (LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND t.isActive = true " +
           "ORDER BY t.name ASC")
    List<NotificationTemplate> searchActiveTemplates(@Param("searchTerm") String searchTerm);

    Page<NotificationTemplate> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndIsActiveTrue(
            String name, String description, Pageable pageable);

    // Find templates by date range
    List<NotificationTemplate> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate,
                                                                         LocalDateTime endDate);

    // Find recently used templates
    @Query("SELECT t FROM NotificationTemplate t " +
           "WHERE t.lastUsedAt IS NOT NULL " +
           "ORDER BY t.lastUsedAt DESC")
    List<NotificationTemplate> findRecentlyUsedTemplates(Pageable pageable);

    // Find most used templates
    @Query("SELECT t FROM NotificationTemplate t " +
           "WHERE t.usageCount > 0 " +
           "ORDER BY t.usageCount DESC")
    List<NotificationTemplate> findMostUsedTemplates(Pageable pageable);

    // Find unused templates
    @Query("SELECT t FROM NotificationTemplate t " +
           "WHERE t.usageCount = 0 AND t.createdAt < :cutoffDate")
    List<NotificationTemplate> findUnusedTemplates(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Count templates by type
    Long countByTemplateType(NotificationTemplate.TemplateType templateType);

    // Count active templates
    Long countByIsActiveTrue();

    // Count templates by category
    Long countByCategory(String category);

    // Count templates by language
    Long countByLanguage(String language);

    // Statistics queries
    @Query("SELECT t.templateType, COUNT(t) FROM NotificationTemplate t " +
           "WHERE t.isActive = true GROUP BY t.templateType")
    List<Object[]> getTemplateStatisticsByType();

    @Query("SELECT t.category, COUNT(t) FROM NotificationTemplate t " +
           "WHERE t.isActive = true AND t.category IS NOT NULL " +
           "GROUP BY t.category")
    List<Object[]> getTemplateStatisticsByCategory();

    @Query("SELECT t.language, COUNT(t) FROM NotificationTemplate t " +
           "WHERE t.isActive = true GROUP BY t.language")
    List<Object[]> getTemplateStatisticsByLanguage();

    // Usage statistics
    @Query("SELECT t.id, t.name, t.templateType, t.usageCount, t.lastUsedAt " +
           "FROM NotificationTemplate t " +
           "ORDER BY t.usageCount DESC")
    List<Object[]> getTemplateUsageStatistics();

    // Update operations
    @Modifying
    @Query("UPDATE NotificationTemplate t SET t.usageCount = t.usageCount + 1, " +
           "t.lastUsedAt = :now WHERE t.id = :templateId")
    int incrementUsageCount(@Param("templateId") UUID templateId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE NotificationTemplate t SET t.isActive = false WHERE t.id = :templateId")
    int deactivateTemplate(@Param("templateId") UUID templateId);

    @Modifying
    @Query("UPDATE NotificationTemplate t SET t.isActive = true WHERE t.id = :templateId")
    int activateTemplate(@Param("templateId") UUID templateId);

    @Modifying
    @Query("UPDATE NotificationTemplate t SET t.isDefault = false " +
           "WHERE t.templateType = :templateType AND t.language = :language")
    int unsetDefaultForTypeAndLanguage(@Param("templateType") NotificationTemplate.TemplateType templateType,
                                      @Param("language") String language);

    @Modifying
    @Query("UPDATE NotificationTemplate t SET t.isDefault = true WHERE t.id = :templateId")
    int setAsDefault(@Param("templateId") UUID templateId);

    @Modifying
    @Query("UPDATE NotificationTemplate t SET t.version = t.version + 1 WHERE t.id = :templateId")
    int incrementVersion(@Param("templateId") UUID templateId);

    // Validation queries
    @Query("SELECT COUNT(t) > 0 FROM NotificationTemplate t " +
           "WHERE t.name = :name AND t.id != :excludeId")
    Boolean existsByNameAndNotId(@Param("name") String name, @Param("excludeId") UUID excludeId);

    @Query("SELECT COUNT(t) FROM NotificationTemplate t " +
           "WHERE t.templateType = :templateType AND t.isDefault = true AND t.isActive = true")
    Long countDefaultTemplatesByType(@Param("templateType") NotificationTemplate.TemplateType templateType);

    // Template versioning
    @Query("SELECT t FROM NotificationTemplate t " +
           "WHERE t.name LIKE CONCAT(:baseName, '%') " +
           "ORDER BY t.version DESC")
    List<NotificationTemplate> findTemplateVersions(@Param("baseName") String baseName);

    @Query("SELECT MAX(t.version) FROM NotificationTemplate t " +
           "WHERE t.name LIKE CONCAT(:baseName, '%')")
    Integer findMaxVersionForTemplate(@Param("baseName") String baseName);

    // Content analysis
    @Query("SELECT t FROM NotificationTemplate t " +
           "WHERE t.contentTemplate LIKE CONCAT('%', :variable, '%') " +
           "OR t.htmlTemplate LIKE CONCAT('%', :variable, '%') " +
           "OR t.smsTemplate LIKE CONCAT('%', :variable, '%')")
    List<NotificationTemplate> findTemplatesUsingVariable(@Param("variable") String variable);

    // Duplicate detection
    @Query("SELECT t FROM NotificationTemplate t " +
           "WHERE t.contentTemplate = :content AND t.id != :excludeId")
    List<NotificationTemplate> findDuplicateTemplatesByContent(@Param("content") String content,
                                                              @Param("excludeId") UUID excludeId);

    // Templates needing review (old, unused, or never used)
    @Query("SELECT t FROM NotificationTemplate t WHERE " +
           "(t.usageCount = 0 AND t.createdAt < :oldCutoff) OR " +
           "(t.lastUsedAt IS NOT NULL AND t.lastUsedAt < :unusedCutoff) OR " +
           "(t.updatedAt < :outdatedCutoff)")
    List<NotificationTemplate> findTemplatesNeedingReview(@Param("oldCutoff") LocalDateTime oldCutoff,
                                                         @Param("unusedCutoff") LocalDateTime unusedCutoff,
                                                         @Param("outdatedCutoff") LocalDateTime outdatedCutoff);

    // Backup and export
    @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true " +
           "ORDER BY t.templateType, t.name")
    List<NotificationTemplate> findAllActiveForExport();

    // Performance monitoring
    @Query("SELECT AVG(LENGTH(t.contentTemplate)), MAX(LENGTH(t.contentTemplate)), " +
           "AVG(LENGTH(t.htmlTemplate)), MAX(LENGTH(t.htmlTemplate)) " +
           "FROM NotificationTemplate t WHERE t.isActive = true")
    Object[] getTemplateContentStatistics();
}
