package cz.zcu.kiv.caretracker.dto.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientRequestDTO {
    private String firstName;
    private String lastName;
    private String gender;
    private String personalNumber;
    private LocalDate dateOfBirth;
    private String email;
    private String phone;
    private String street;
    private String city;
    private String postalCode;
    private Boolean legallyCompetent;
    private String benefits;
    private String relativesContact;
    private String generalPractitioner;
    private String notes;
    private Long departmentId;
    private Long caregiverId;
    private List<Long> taskIds;
    private LocalDate terminationDate;
    private String terminationReason;
    private String accommodationType;
    private Long supportedHousingId;
    private Integer travelDistanceKm;
    private Boolean hasElevator;
    private String floorOrRoom;
    private String accessMethod;
    private String lockCode;
    private String accessNote;
    private String titleBefore;
    private String titleAfter;
    private String phone2;
    private String guardianName;
    private String guardianContact;
    private LocalDate careFrom;
    private LocalDate careTo;
    private LocalDate contractFrom;
    private LocalDate contractTo;
    private Boolean contractIndefinite;
    private Boolean noCharge;
    private LocalDate intakeAssessmentDate;
}
