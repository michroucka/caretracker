package cz.zcu.kiv.caretracker.service;

import cz.zcu.kiv.caretracker.dto.department.SupportedHousingDTO;
import cz.zcu.kiv.caretracker.dto.department.SupportedHousingRequestDTO;
import cz.zcu.kiv.caretracker.entity.Client;
import cz.zcu.kiv.caretracker.entity.Department;
import cz.zcu.kiv.caretracker.entity.SupportedHousing;
import cz.zcu.kiv.caretracker.exception.ResourceNotFoundException;
import cz.zcu.kiv.caretracker.exception.ValidationException;
import cz.zcu.kiv.caretracker.repository.ClientRepository;
import cz.zcu.kiv.caretracker.repository.DepartmentRepository;
import cz.zcu.kiv.caretracker.repository.SupportedHousingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class SupportedHousingService extends BaseRoleFilteringService<SupportedHousing, SupportedHousingDTO> {
    private static final Pattern POSTAL_CODE = Pattern.compile("\\d{3} \\d{2}");

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private SupportedHousingRepository supportedHousingRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private GeocodingService geocodingService;

    @Transactional(readOnly = true)
    public List<SupportedHousingDTO> getSupportedHousings(Long departmentId) {
        findReadableDepartment(departmentId);

        return supportedHousingRepository.findByDepartmentIdOrderByNameAsc(departmentId)
                .stream()
                .map(SupportedHousingService::toDTO)
                .toList();
    }

    public SupportedHousingDTO createSupportedHousing(Long departmentId, SupportedHousingRequestDTO dto) {
        Department department = findEditableDepartment(departmentId);

        SupportedHousing housing = new SupportedHousing();
        housing.setDepartment(department);
        applyRequest(housing, dto);

        return toDTO(supportedHousingRepository.save(housing));
    }

    /** A changed address of the house is copied to every client living in it. */
    @Transactional
    public SupportedHousingDTO updateSupportedHousing(Long departmentId, Long housingId, SupportedHousingRequestDTO dto) {
        findEditableDepartment(departmentId);
        SupportedHousing housing = findHousing(departmentId, housingId);

        List<String> previousAddress = addressOf(housing);

        applyRequest(housing, dto);
        boolean addressChanged = !previousAddress.equals(addressOf(housing));
        if (addressChanged) {
            moveClientsToHousingAddress(housing);
        }

        return toDTO(supportedHousingRepository.save(housing));
    }

    private static List<String> addressOf(SupportedHousing housing) {
        return Arrays.asList(housing.getStreet(), housing.getCity(), housing.getPostalCode());
    }

    private void moveClientsToHousingAddress(SupportedHousing housing) {
        List<Client> clients = clientRepository.findBySupportedHousingId(housing.getId());
        if (clients.isEmpty()) {
            return;
        }
        Optional<GeocodingService.GeocodeResult> position = geocodingService.geocode(housing.getStreet(), housing.getCity(), housing.getPostalCode());
        for (Client client : clients) {
            client.setStreet(housing.getStreet());
            client.setCity(housing.getCity());
            client.setPostalCode(housing.getPostalCode());
            client.setLatitude(position.map(GeocodingService.GeocodeResult::latitude).orElse(null));
            client.setLongitude(position.map(GeocodingService.GeocodeResult::longitude).orElse(null));
        }
    }

    public SupportedHousingDTO terminateSupportedHousing(Long departmentId, Long housingId) {
        findEditableDepartment(departmentId);
        SupportedHousing housing = findHousing(departmentId, housingId);

        housing.setActive(false);
        return toDTO(supportedHousingRepository.save(housing));
    }

    private static void applyRequest(SupportedHousing housing, SupportedHousingRequestDTO dto) {
        housing.setName(requireText(dto.getName(), "Prosím zadejte název domu"));
        housing.setStreet(requireText(dto.getStreet(), "Prosím zadejte ulici a číslo popisné"));
        housing.setCity(requireText(dto.getCity(), "Prosím zadejte město"));
        String postalCode = requireText(dto.getPostalCode(), "Prosím zadejte PSČ");
        if (!POSTAL_CODE.matcher(postalCode).matches()) {
            throw new ValidationException("PSČ ve tvaru 123 45");
        }
        housing.setPostalCode(postalCode);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
        return value.trim();
    }

    private SupportedHousing findHousing(Long departmentId, Long housingId) {
        return supportedHousingRepository.findByIdAndDepartmentId(housingId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Dům s pečovatelskou službou nebyl nalezen"));
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

    private Department findDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Středisko nebylo nalezeno"));
    }

    private static SupportedHousingDTO toDTO(SupportedHousing housing) {
        return new SupportedHousingDTO(housing.getId(), housing.getName(), housing.getStreet(), housing.getCity(),
                housing.getPostalCode(), housing.getActive());
    }
}
