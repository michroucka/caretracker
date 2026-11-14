package cz.zcu.kiv.caretracker.entity;

import cz.zcu.kiv.caretracker.enums.AccessMethod;
import cz.zcu.kiv.caretracker.enums.AccommodationType;
import cz.zcu.kiv.caretracker.enums.BenefitLevel;
import cz.zcu.kiv.caretracker.enums.Gender;
import cz.zcu.kiv.caretracker.enums.TerminationReason;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "client")
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false)
    private Gender gender;

    @Column(name = "personal_number", nullable = false, length = 6)
    private String personalNumber;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Enumerated
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "termination_reason")
    private TerminationReason terminationReason;

    private String email;
    @Column(length = 16)
    private String phone;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String city;

    @Column(name = "postal_code", nullable = false, length = 6)
    private String postalCode;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "legally_competent", nullable = false)
    private Boolean legallyCompetent;

    @Enumerated
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false)
    private BenefitLevel benefits;

    @Column(columnDefinition = "TEXT", name = "relatives_contact")
    private String relativesContact;

    @Column(columnDefinition = "TEXT", name = "general_practitioner")
    private String generalPractitioner;

    private String notes;
    private LocalDate created = LocalDate.now();

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Enumerated
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "accommodation_type")
    private AccommodationType accommodationType;

    @Column(name = "travel_distance_km")
    private Integer travelDistanceKm;

    @Column(name = "has_elevator")
    private Boolean hasElevator;

    @Column(name = "floor_or_room", length = 50)
    private String floorOrRoom;

    @Enumerated
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "access_method")
    private AccessMethod accessMethod;

    @Column(name = "lock_code", length = 20)
    private String lockCode;

    @Column(name = "access_note", columnDefinition = "TEXT")
    private String accessNote;

    @Column(name = "title_before", length = 20)
    private String titleBefore;

    @Column(name = "title_after", length = 20)
    private String titleAfter;

    @Column(length = 16)
    private String phone2;

    @Column(name = "guardian_name")
    private String guardianName;

    @Column(name = "guardian_contact", columnDefinition = "TEXT")
    private String guardianContact;

    @Column(name = "care_from")
    private LocalDate careFrom;

    @Column(name = "care_to")
    private LocalDate careTo;

    @Column(name = "contract_from")
    private LocalDate contractFrom;

    @Column(name = "contract_to")
    private LocalDate contractTo;

    @Column(name = "contract_indefinite")
    private Boolean contractIndefinite = Boolean.TRUE;

    @Column(name = "no_charge", nullable = false)
    private Boolean noCharge = Boolean.FALSE;

    @Column(name = "intake_assessment_date")
    private LocalDate intakeAssessmentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supported_housing_id")
    private SupportedHousing supportedHousing;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    private Department department;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    private Organization organization;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "caregiver_id", nullable = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    private Employee caregiver;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "picture_id")
    private Picture picture;

    @OneToOne(mappedBy = "client")
    private User user;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "client_task",
            joinColumns = @JoinColumn(name = "client_id"),
            inverseJoinColumns = @JoinColumn(name = "task_id")
    )
    private List<Task> tasks;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** Id of the picture without loading its data (the lazy proxy knows its id); used as a cache version. */
    public Long getPictureId() {
        return picture != null ? picture.getId() : null;
    }
}
