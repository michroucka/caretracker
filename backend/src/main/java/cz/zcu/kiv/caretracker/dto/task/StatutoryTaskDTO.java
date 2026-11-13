package cz.zcu.kiv.caretracker.dto.task;

/** Statutory catalog item; {@code importedTaskId} is the organization's task linked to it, or null. */
public record StatutoryTaskDTO(
        Long id,
        String code,
        String name,
        String shortName,
        String unitType,
        Integer maxUnitPrice,
        Long importedTaskId
) {
}
