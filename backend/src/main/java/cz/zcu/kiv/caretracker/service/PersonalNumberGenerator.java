package cz.zcu.kiv.caretracker.service;

import cz.zcu.kiv.caretracker.entity.Department;
import cz.zcu.kiv.caretracker.exception.ValidationException;
import cz.zcu.kiv.caretracker.repository.ClientRepository;
import org.springframework.stereotype.Component;

/**
 * Personal numbers have the form XXYYYY: the two-digit department number and a four-digit sequence. The number
 * is part of the payment variable symbol, so a client keeps it after moving to another department and it is never
 * given again; the next number therefore follows the highest one with the department's prefix in the whole
 * organization, terminated and moved clients included.
 */
@Component
public class PersonalNumberGenerator {

    private static final int MAX_SEQUENCE = 9_999;

    private final ClientRepository clientRepository;

    public PersonalNumberGenerator(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public String next(Department department) {
        String prefix = "%02d".formatted(department.getDepartmentNumber());
        String highest = clientRepository.findHighestPersonalNumber(department.getOrganization().getId(), prefix);
        int sequence = highest == null ? 1 : Integer.parseInt(highest.substring(prefix.length())) + 1;
        if (sequence > MAX_SEQUENCE) {
            throw new ValidationException("Ve středisku došla osobní čísla");
        }
        return prefix + "%04d".formatted(sequence);
    }
}
