package cz.zcu.kiv.caretracker.service;

import cz.zcu.kiv.caretracker.dto.task.StatutoryTaskDTO;
import cz.zcu.kiv.caretracker.dto.task.TaskDTO;
import cz.zcu.kiv.caretracker.dto.task.TaskRequestDTO;
import cz.zcu.kiv.caretracker.entity.*;
import cz.zcu.kiv.caretracker.enums.UserRole;
import cz.zcu.kiv.caretracker.exception.ResourceNotFoundException;
import cz.zcu.kiv.caretracker.exception.ValidationException;
import cz.zcu.kiv.caretracker.mapper.TaskMapper;
import cz.zcu.kiv.caretracker.repository.OrganizationRepository;
import cz.zcu.kiv.caretracker.repository.StatutoryTaskRepository;
import cz.zcu.kiv.caretracker.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages billable service catalog entries scoped to an organization.
 */
@Service
public class TaskService extends BaseRoleFilteringService<Task, TaskDTO> {
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    TaskMapper taskMapper;
    @Autowired
    OrganizationRepository organizationRepository;
    @Autowired
    StatutoryTaskRepository statutoryTaskRepository;

    /**
     * Returns tasks filtered by the current user's role and optional status filter.
     *
     * @param organizationId optional organization filter (SUPERADMIN only)
     * @param status {@code true} = active, {@code false} = inactive, {@code null} = all (COORDINATOR and CAREGIVER always see active)
     * @return role-filtered list of task DTOs
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasks(Long organizationId, Boolean status) {
        // Calculate filters based on user role
        RoleBasedFilters roleFilters = calculateRoleBasedFilters(organizationId, null);

        // If user has no access, return empty list
        if (roleFilters.isNoAccess()) {
            return List.of();
        }

        UserRole role = getCurrentUser().getRole();
        if (role == UserRole.COORDINATOR || role == UserRole.CAREGIVER) {
            status = true;
        }

        // Fetch tasks based on computed organizationId
        // status: null = all, true = active only, false = inactive only
        Long orgId = roleFilters.getOrganizationId();
        List<Task> tasks;
        if (status == null) {
            tasks = taskRepository.findByOrganizationId(orgId);
        } else if (status) {
            tasks = taskRepository.findByActiveTrueAndOrganizationId(orgId);
        } else {
            tasks = taskRepository.findByActiveFalseAndOrganizationId(orgId);
        }

        return taskMapper.toDTOList(tasks);
    }

    /**
     * Returns a single task by ID with organization-level access control.
     *
     * @param id the task ID
     * @return the task DTO, or empty if not found or inaccessible
     */
    @Transactional(readOnly = true)
    public Optional<TaskDTO> getTaskById(Long id) {
        return getEntityByIdWithPermissionCheck(
                id,
                () -> taskRepository.findById(id),
                task -> task.getOrganization().getId(),
                taskMapper::toDTO
        );
    }

    /**
     * Persists a task from the supplied DTO, resolving the owning organization.
     * SUPERADMIN must supply an explicit organizationId; other roles use their own organization.
     */
    private Task saveTask(Task task, TaskRequestDTO dto) {
        if (dto.getUnitPrice() == null || dto.getUnitPrice() <= 0) {
            throw new ValidationException("Cena úkonu musí být větší než 0");
        }
        User user = getCurrentUser();

        Organization organization;
        if (user.getRole() == UserRole.SUPERADMIN) {
            if (task.getOrganization() != null) {
                organization = task.getOrganization();
            } else if (dto.getOrganizationId() != null) {
                organization = organizationRepository.findById(dto.getOrganizationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Organizace nebyla nalezena"));
            } else {
                throw new ValidationException("Pro vytvoření úkonu musí SUPERADMIN zvolit organizaci");
            }
        } else {
            Employee employee = user.getEmployee();
            if (employee == null) {
                throw new SecurityException("Pouze zaměstnanci mohou vytvářet nebo upravovat úkony");
            }
            organization = employee.getOrganization();
        }

        StatutoryTask statutoryTask = dto.getStatutoryTaskId() == null ? null
                : statutoryTaskRepository.findById(dto.getStatutoryTaskId())
                        .orElseThrow(() -> new ResourceNotFoundException("Položka vyhlášky nebyla nalezena"));
        if (statutoryTask != null) {
            validateAgainstStatutoryTask(dto, statutoryTask);
        }

        taskMapper.requestToTask(task, dto, organization, statutoryTask);

        return taskRepository.save(task);
    }

    private static void validateAgainstStatutoryTask(TaskRequestDTO dto, StatutoryTask statutoryTask) {
        if (!statutoryTask.getUnitType().name().equals(dto.getUnitType())) {
            throw new ValidationException("Jednotka úkonu neodpovídá položce vyhlášky");
        }
        if (dto.getUnitPrice() > statutoryTask.getMaxUnitPrice()) {
            throw new ValidationException("Cena převyšuje maximální úhradu podle vyhlášky (" + statutoryTask.getMaxUnitPrice() + " Kč)");
        }
    }

    /**
     * Returns the whole statutory catalog, each item with the organization's task linked to it (if any).
     * SUPERADMIN chooses the organization; without one no item is marked as imported.
     *
     * @param organizationId optional organization (SUPERADMIN only)
     * @return catalog items in the order of the decree
     */
    @Transactional(readOnly = true)
    public List<StatutoryTaskDTO> getStatutoryTasks(Long organizationId) {
        Long orgId = calculateRoleBasedFiltersOrganizationOnly(organizationId).getOrganizationId();
        Map<Long, Long> importedTaskIds = orgId == null ? Map.of() : linkedTaskIdsByStatutoryTaskId(orgId);
        return statutoryTaskRepository.findAllByOrderByIdAsc().stream()
                .map(statutoryTask -> taskMapper.toStatutoryDTO(statutoryTask, importedTaskIds.get(statutoryTask.getId())))
                .toList();
    }

    /**
     * Creates tasks of the organization for the chosen statutory items, named by their short names and priced
     * at their maxima. Items the organization already has a linked task for are skipped.
     *
     * @param organizationId the target organization (SUPERADMIN only, others import into their own)
     * @param statutoryTaskIds chosen catalog items
     * @return the created tasks
     */
    @Transactional
    public List<Task> importStatutoryTasks(Long organizationId, List<Long> statutoryTaskIds) {
        if (statutoryTaskIds == null || statutoryTaskIds.isEmpty()) {
            throw new ValidationException("Vyberte alespoň jeden úkon");
        }
        Long orgId = calculateRoleBasedFiltersOrganizationOnly(organizationId).getOrganizationId();
        if (orgId == null) {
            throw new ValidationException("Pro načtení úkonů musí SUPERADMIN zvolit organizaci");
        }
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizace nebyla nalezena"));
        List<StatutoryTask> chosen = statutoryTaskRepository.findAllById(statutoryTaskIds);
        if (chosen.size() != Set.copyOf(statutoryTaskIds).size()) {
            throw new ResourceNotFoundException("Položka vyhlášky nebyla nalezena");
        }

        Set<Long> alreadyLinked = linkedTaskIdsByStatutoryTaskId(orgId).keySet();
        List<Task> created = chosen.stream()
                .filter(statutoryTask -> !alreadyLinked.contains(statutoryTask.getId()))
                .sorted(Comparator.comparing(StatutoryTask::getId))
                .map(statutoryTask -> taskMapper.fromStatutoryTask(statutoryTask, organization))
                .toList();
        return taskRepository.saveAll(created);
    }

    private Map<Long, Long> linkedTaskIdsByStatutoryTaskId(Long organizationId) {
        return taskRepository.findByOrganizationIdAndStatutoryTaskIsNotNull(organizationId).stream()
                .collect(Collectors.toMap(task -> task.getStatutoryTask().getId(), Task::getId, Math::min));
    }

    /**
     * Creates a new task.
     *
     * @param dto the task creation data
     * @return the persisted task entity
     */
    public Task createTask(TaskRequestDTO dto) {
        Task task = new Task();
        return saveTask(task, dto);
    }

    /**
     * Updates an existing task.
     *
     * @param id the task ID
     * @param dto updated task data
     * @return the updated task entity
     */
    public Task updateTask(Long id, TaskRequestDTO dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Úkon nebyl nalezen"));

        validateOrganizationAccess(task, t -> t.getOrganization().getId());

        return saveTask(task, dto);
    }

    /** Sets the active flag on a task with organization-level access validation. */
    private Task setTaskStatus(Long id, boolean status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Úkon nebyl nalezen"));

        validateOrganizationAccess(task, t -> t.getOrganization().getId());

        task.setActive(status);

        return taskRepository.save(task);
    }

    /**
     * Deactivates a task (soft delete).
     *
     * @param id the task ID
     * @return the updated task entity
     */
    public Task terminateTask(Long id) {
        return setTaskStatus(id, false);
    }

    /**
     * Re-activates a previously deactivated task.
     *
     * @param id the task ID
     * @return the updated task entity
     */
    public Task activateTask(Long id) {
        return setTaskStatus(id, true);
    }
}
