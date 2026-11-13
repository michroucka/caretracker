package cz.zcu.kiv.caretracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Reduced maximum for hourly statutory activities of a client above a number of hours in a calendar month
 * (decree 505/2006 Sb., § 6 odst. 2 písm. a). The table has a single row.
 */
@Getter
@Setter
@Entity
@Table(name = "statutory_hourly_limit")
public class StatutoryHourlyLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monthly_hours_threshold", nullable = false)
    private Integer monthlyHoursThreshold;

    @Column(name = "max_unit_price_over_threshold", nullable = false)
    private Integer maxUnitPriceOverThreshold;
}
