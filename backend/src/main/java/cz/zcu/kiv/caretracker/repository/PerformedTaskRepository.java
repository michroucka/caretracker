package cz.zcu.kiv.caretracker.repository;

import cz.zcu.kiv.caretracker.entity.PerformedTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PerformedTaskRepository extends JpaRepository<PerformedTask, Long>, JpaSpecificationExecutor<PerformedTask> {
    List<PerformedTask> findByOrganizationId(Long organizationId);
    List<PerformedTask> findByDepartmentId(Long departmentId);

    /** Records from {@code start} up to, but not including, {@code end}. */
    List<PerformedTask> findByCaregivers_IdAndDateGreaterThanEqualAndDateLessThan(Long caregiverId, LocalDateTime start, LocalDateTime end);

    List<PerformedTask> findTop5ByOrganizationIdOrderByDateDesc(Long organizationId);
    List<PerformedTask> findTop5ByDepartmentIdOrderByDateDesc(Long departmentId);
    List<PerformedTask> findTop5ByCaregivers_IdOrderByDateDesc(Long caregiverId);

    /** Returns rows of [organizationId, latest task date] for all organizations. */
    @Query("SELECT pt.organization.id, MAX(pt.date) FROM PerformedTask pt GROUP BY pt.organization.id")
    List<Object[]> findLastActivityByOrganization();

    /*
     * Dashboard aggregates. Price per task comes from the unit price stored with it and is rounded like
     * PerformedTask#calculatePrice (HOUR tasks are stored in minutes), then summed.
     */

    /**
     * Records of whole calendar months around [start, end) with the price they are billed at, as on the receipt
     * (ReceiptFormatting#billingLines): minutes of statutory hourly tasks over the client's monthly threshold
     * cost at most the reduced statutory rate, each part rounded on its own. Whole months are read because the
     * threshold counts all of the client's records of the month; callers filter the range afterwards.
     */
    String BILLED_RECORDS = """
            WITH hourly_limit AS (
                SELECT monthly_hours_threshold * 60 AS threshold_minutes, max_unit_price_over_threshold AS reduced_price
                FROM statutory_hourly_limit ORDER BY id LIMIT 1
            ),
            records AS (
                SELECT pt.id, pt.date, pt.organization_id, pt.department_id, pt.unit_count, pt.unit_price, t.unit_type,
                       (t.unit_type = 'HOUR' AND t.statutory_task_id IS NOT NULL) AS capped,
                       COALESCE(SUM(CASE WHEN t.unit_type = 'HOUR' AND t.statutory_task_id IS NOT NULL THEN pt.unit_count ELSE 0 END)
                           OVER (PARTITION BY pt.client_id, date_trunc('month', pt.date) ORDER BY pt.date, pt.id
                                 ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) AS capped_minutes_before
                FROM performed_task pt
                JOIN task t ON t.id = pt.task_id
                WHERE pt.date >= date_trunc('month', CAST(:start AS timestamp))
                  AND pt.date < date_trunc('month', CAST(:end AS timestamp) - INTERVAL '1 microsecond') + INTERVAL '1 month'
                  AND (CAST(:organizationId AS bigint) IS NULL OR pt.organization_id = :organizationId)
            ),
            billed AS (
                SELECT r.id, r.date, r.organization_id, r.department_id,
                       CASE
                           WHEN r.unit_type <> 'HOUR' THEN ROUND(r.unit_count * r.unit_price)
                           WHEN NOT r.capped OR l.reduced_price IS NULL OR r.unit_price <= l.reduced_price
                                OR r.capped_minutes_before + r.unit_count <= l.threshold_minutes
                               THEN ROUND(r.unit_count * r.unit_price / 60.0)
                           ELSE ROUND(GREATEST(LEAST(r.unit_count, l.threshold_minutes - r.capped_minutes_before), 0) * r.unit_price / 60.0)
                              + ROUND((r.unit_count - GREATEST(LEAST(r.unit_count, l.threshold_minutes - r.capped_minutes_before), 0))
                                      * l.reduced_price / 60.0)
                       END AS price
                FROM records r
                LEFT JOIN hourly_limit l ON TRUE
            )
            """;

    /**
     * Returns rows of [year, month, task count, income] for tasks in [start, end).
     * Each scope parameter is optional (null = no restriction), all null = whole application.
     */
    @Query(value = BILLED_RECORDS + """
            SELECT EXTRACT(YEAR FROM b.date) AS year, EXTRACT(MONTH FROM b.date) AS month,
                   COUNT(*) AS task_count,
                   COALESCE(SUM(b.price), 0) AS income
            FROM billed b
            WHERE b.date >= :start AND b.date < :end
              AND (CAST(:departmentId AS bigint) IS NULL OR b.department_id = :departmentId)
              AND (CAST(:caregiverId AS bigint) IS NULL OR EXISTS (
                    SELECT 1 FROM caregiver_performed_task cpt
                    WHERE cpt.performed_task_id = b.id AND cpt.caregiver_id = :caregiverId))
            GROUP BY 1, 2
            """, nativeQuery = true)
    List<Object[]> sumByMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                              @Param("organizationId") Long organizationId,
                              @Param("departmentId") Long departmentId,
                              @Param("caregiverId") Long caregiverId);

    /** Returns rows of [caregiverId, task count, minutes of HOUR tasks] for one department in [start, end). */
    @Query(value = """
            SELECT cpt.caregiver_id, COUNT(*) AS task_count,
                   COALESCE(SUM(CASE WHEN t.unit_type = 'HOUR' THEN pt.unit_count ELSE 0 END), 0) AS minutes
            FROM caregiver_performed_task cpt
            JOIN performed_task pt ON pt.id = cpt.performed_task_id
            JOIN task t ON t.id = pt.task_id
            WHERE pt.department_id = :departmentId AND pt.date >= :start AND pt.date < :end
            GROUP BY cpt.caregiver_id
            """, nativeQuery = true)
    List<Object[]> sumByCaregiver(@Param("departmentId") Long departmentId,
                                  @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** Returns rows of [department name, task count, income] for one organization in [start, end). */
    @Query(value = BILLED_RECORDS + """
            SELECT d.city, COUNT(*) AS task_count, COALESCE(SUM(b.price), 0) AS income
            FROM billed b
            JOIN department d ON d.id = b.department_id
            WHERE b.date >= :start AND b.date < :end
            GROUP BY d.id, d.city
            """, nativeQuery = true)
    List<Object[]> sumByDepartment(@Param("organizationId") Long organizationId,
                                   @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** Returns rows of [organizationId, task count] in [start, end). */
    @Query(value = """
            SELECT pt.organization_id, COUNT(*) FROM performed_task pt
            WHERE pt.date >= :start AND pt.date < :end
            GROUP BY pt.organization_id
            """, nativeQuery = true)
    List<Object[]> countByOrganization(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
