package cz.zcu.kiv.caretracker.dto.task;

import java.util.List;

public record StatutoryTaskImportRequestDTO(List<Long> statutoryTaskIds) {
}
