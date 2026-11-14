package cz.zcu.kiv.caretracker.controller;

import cz.zcu.kiv.caretracker.dto.client.PersonalNumberDTO;
import cz.zcu.kiv.caretracker.dto.department.DepartmentDTO;
import cz.zcu.kiv.caretracker.dto.department.DepartmentRequestDTO;
import cz.zcu.kiv.caretracker.dto.department.MunicipalityDTO;
import cz.zcu.kiv.caretracker.dto.department.MunicipalityRequestDTO;
import cz.zcu.kiv.caretracker.dto.department.SupportedHousingDTO;
import cz.zcu.kiv.caretracker.dto.department.SupportedHousingRequestDTO;
import cz.zcu.kiv.caretracker.entity.Department;
import cz.zcu.kiv.caretracker.mapper.DepartmentMapper;
import cz.zcu.kiv.caretracker.service.DepartmentService;
import cz.zcu.kiv.caretracker.service.MunicipalityService;
import cz.zcu.kiv.caretracker.service.SupportedHousingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for department management.
 */
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    private static final Logger log = LoggerFactory.getLogger(DepartmentController.class);

    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private DepartmentMapper departmentMapper;
    @Autowired
    private MunicipalityService municipalityService;
    @Autowired
    private SupportedHousingService supportedHousingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'COORDINATOR', 'CAREGIVER')")
    public ResponseEntity<List<DepartmentDTO>> getDepartments(
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Boolean status
    ) {
        log.info("Fetching departments" + (organizationId != null ? " for organization: " + organizationId : ""));
        List<DepartmentDTO> departments = departmentService.getDepartments(organizationId, status);

        return ResponseEntity.ok(departments);
    }

    @GetMapping("/{id}/next-personal-number")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'COORDINATOR')")
    public ResponseEntity<PersonalNumberDTO> getNextPersonalNumber(@PathVariable Long id) {
        return ResponseEntity.ok(new PersonalNumberDTO(departmentService.nextPersonalNumber(id)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'COORDINATOR', 'CAREGIVER')")
    public ResponseEntity<DepartmentDTO> getDepartmentById(@PathVariable Long id) {
        log.info("Fetching department with id: {}", id);
        return departmentService.getDepartmentById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<DepartmentDTO> createDepartment(@RequestBody DepartmentRequestDTO dto) {
        log.info("Creating new department: {}", dto.getCity());
        Department savedDepartment = departmentService.createDepartment(dto);
        return ResponseEntity.ok(departmentMapper.toDTO(savedDepartment));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<DepartmentDTO> updateDepartment(@PathVariable Long id, @RequestBody DepartmentRequestDTO dto) {
        log.info("Updating department with id: {}", id);
        Department updatedDepartment = departmentService.updateDepartment(id, dto);
        return ResponseEntity.ok(departmentMapper.toDTO(updatedDepartment));
    }

    @PutMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<DepartmentDTO> terminateDepartment(@PathVariable Long id) {
        log.info("Terminating department with id: {}", id);
        Department updatedDepartment = departmentService.terminateDepartment(id);
        return ResponseEntity.ok(departmentMapper.toDTO(updatedDepartment));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<DepartmentDTO> activateDepartment(@PathVariable Long id) {
        log.info("Activating department with id: {}", id);
        Department updatedDepartment = departmentService.activateDepartment(id);
        return ResponseEntity.ok(departmentMapper.toDTO(updatedDepartment));
    }

    @GetMapping("/{id}/municipalities")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'COORDINATOR', 'CAREGIVER')")
    public ResponseEntity<List<MunicipalityDTO>> getMunicipalities(@PathVariable Long id) {
        log.info("Fetching municipalities for department with id: {}", id);
        List<MunicipalityDTO> municipalities = municipalityService.getMunicipalities(id);
        return ResponseEntity.ok(municipalities);
    }

    @PostMapping("/{id}/municipalities")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'COORDINATOR')")
    public ResponseEntity<MunicipalityDTO> createMunicipality(@PathVariable Long id, @RequestBody MunicipalityRequestDTO dto) {
        log.info("Creating municipality '{}' for department with id: {}", dto.getName(), id);
        MunicipalityDTO savedMunicipalityDTO = municipalityService.createMunicipality(id, dto);
        return ResponseEntity.ok(savedMunicipalityDTO);
    }

    @DeleteMapping("/{id}/municipalities/{municipalityId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'COORDINATOR')")
    public ResponseEntity<Void> deleteMunicipality(@PathVariable Long id, @PathVariable Long municipalityId) {
        log.info("Deleting municipality with id: {} in department with id : {}", municipalityId, id);
        municipalityService.deleteMunicipality(id, municipalityId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/supported-housings")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'COORDINATOR', 'CAREGIVER')")
    public ResponseEntity<List<SupportedHousingDTO>> getSupportedHousings(@PathVariable Long id) {
        log.info("Fetching supported housings for department with id: {}", id);
        return ResponseEntity.ok(supportedHousingService.getSupportedHousings(id));
    }

    @PostMapping("/{id}/supported-housings")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'COORDINATOR')")
    public ResponseEntity<SupportedHousingDTO> createSupportedHousing(@PathVariable Long id, @RequestBody SupportedHousingRequestDTO dto) {
        log.info("Creating supported housing for department with id: {}", id);
        return ResponseEntity.ok(supportedHousingService.createSupportedHousing(id, dto));
    }

    @PutMapping("/{id}/supported-housings/{housingId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'COORDINATOR')")
    public ResponseEntity<SupportedHousingDTO> updateSupportedHousing(@PathVariable Long id, @PathVariable Long housingId,
                                                                      @RequestBody SupportedHousingRequestDTO dto) {
        log.info("Updating supported housing with id: {} in department with id: {}", housingId, id);
        return ResponseEntity.ok(supportedHousingService.updateSupportedHousing(id, housingId, dto));
    }

    @PutMapping("/{id}/supported-housings/{housingId}/terminate")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'COORDINATOR')")
    public ResponseEntity<SupportedHousingDTO> terminateSupportedHousing(@PathVariable Long id, @PathVariable Long housingId) {
        log.info("Terminating supported housing with id: {} in department with id: {}", housingId, id);
        return ResponseEntity.ok(supportedHousingService.terminateSupportedHousing(id, housingId));
    }
}
