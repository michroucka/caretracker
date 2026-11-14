package cz.zcu.kiv.caretracker.service;

import cz.zcu.kiv.caretracker.dto.department.MunicipalityDTO;
import cz.zcu.kiv.caretracker.dto.department.MunicipalityRequestDTO;
import cz.zcu.kiv.caretracker.entity.Department;
import cz.zcu.kiv.caretracker.entity.Municipality;
import cz.zcu.kiv.caretracker.exception.ResourceNotFoundException;
import cz.zcu.kiv.caretracker.exception.ValidationException;
import cz.zcu.kiv.caretracker.repository.DepartmentRepository;
import cz.zcu.kiv.caretracker.repository.MunicipalityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MunicipalityService extends BaseRoleFilteringService<Municipality, MunicipalityDTO> {
    public static final String DUPLICATE_MUNICIPALITY_MESSAGE = "Obec už ve středisku existuje";

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private MunicipalityRepository municipalityRepository;

    @Transactional(readOnly = true)
    public List<MunicipalityDTO> getMunicipalities(Long departmentId) {
        findReadableDepartment(departmentId);

        return municipalityRepository.findByDepartmentIdOrderByNameAsc(departmentId)
                .stream()
                .map(MunicipalityService::toDTO)
                .toList();
    }

    public MunicipalityDTO createMunicipality(Long departmentId, MunicipalityRequestDTO dto) {
        Department department = findEditableDepartment(departmentId);

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ValidationException("Zadejte název obce");
        }
        String name = dto.getName().trim();

        if (municipalityRepository.existsByDepartmentIdAndNameIgnoreCase(departmentId, name)) {
            throw new ValidationException(DUPLICATE_MUNICIPALITY_MESSAGE);
        }

        Municipality municipality = new Municipality();
        municipality.setName(name);
        municipality.setDepartment(department);

        return toDTO(municipalityRepository.save(municipality));
    }

    public void deleteMunicipality(Long departmentId, Long municipalityId) {
        findEditableDepartment(departmentId);
        Municipality municipality = municipalityRepository.findByIdAndDepartmentId(municipalityId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Obec nebyla nalezena"));

        municipalityRepository.delete(municipality);
    }

    private Department findDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Středisko nebylo nalezeno"));
    }

    private Department findReadableDepartment(Long departmentId) {
        Department department = findDepartment(departmentId);

        validateOrganizationAccess(department, d -> d.getOrganization().getId());

        return department;
    }

    private Department findEditableDepartment(Long departmentId) {
        Department department = findDepartment(departmentId);

        validateDepartmentAccess(department, d -> d.getOrganization().getId(), Department::getId);

        return department;
    }

    private static MunicipalityDTO toDTO(Municipality municipality) {
        return new MunicipalityDTO(municipality.getId(), municipality.getName());
    }
}
