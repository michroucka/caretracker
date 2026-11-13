package cz.zcu.kiv.caretracker.entity;

import cz.zcu.kiv.caretracker.enums.UnitType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

/** Activity of home care with its maximum fee per unit as set by decree 505/2006 Sb., § 6. Reference data. */
@Getter
@Setter
@Entity
@Table(name = "statutory_task")
public class StatutoryTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "short_name", nullable = false, length = 30)
    private String shortName;

    @Enumerated
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "unit_type", nullable = false)
    private UnitType unitType;

    @Column(name = "max_unit_price", nullable = false)
    private Integer maxUnitPrice;
}
