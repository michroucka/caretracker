package cz.zcu.kiv.caretracker.mapper;

import cz.zcu.kiv.caretracker.dto.task.StatutoryTaskDTO;
import cz.zcu.kiv.caretracker.dto.task.TaskDTO;
import cz.zcu.kiv.caretracker.dto.task.TaskRequestDTO;
import cz.zcu.kiv.caretracker.entity.Organization;
import cz.zcu.kiv.caretracker.entity.StatutoryTask;
import cz.zcu.kiv.caretracker.entity.Task;
import cz.zcu.kiv.caretracker.enums.UnitType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TaskMapper {
    public TaskDTO toDTO(Task task) {
        if (task == null) {
            return null;
        }

        TaskDTO dto = new TaskDTO();

        dto.setId(task.getId());
        dto.setName(task.getName());
        dto.setUnitPrice(task.getUnitPrice());
        dto.setUnitType(task.getUnitType().name());
        dto.setDoubleMeeting(task.getDoubleMeeting());
        dto.setActive(task.getActive());
        StatutoryTask statutoryTask = task.getStatutoryTask();
        if (statutoryTask != null) {
            dto.setStatutoryTaskId(statutoryTask.getId());
            dto.setStatutoryTaskCode(statutoryTask.getCode());
            dto.setStatutoryTaskName(statutoryTask.getName());
            dto.setStatutoryMaxUnitPrice(statutoryTask.getMaxUnitPrice());
        }

        return dto;
    }

    public void requestToTask(Task task, TaskRequestDTO dto, Organization organization, StatutoryTask statutoryTask) {
        task.setName(dto.getName());
        task.setUnitPrice(dto.getUnitPrice());
        task.setUnitType(UnitType.valueOf(dto.getUnitType()));
        task.setDoubleMeeting(dto.getDoubleMeeting());
        task.setStatutoryTask(statutoryTask);

        task.setOrganization(organization);
    }

    /** New task of the organization billing the statutory item, named by its short name and priced at the maximum. */
    public Task fromStatutoryTask(StatutoryTask statutoryTask, Organization organization) {
        Task task = new Task();
        task.setName(statutoryTask.getShortName());
        task.setUnitPrice(statutoryTask.getMaxUnitPrice());
        task.setUnitType(statutoryTask.getUnitType());
        task.setDoubleMeeting(false);
        task.setStatutoryTask(statutoryTask);
        task.setOrganization(organization);
        return task;
    }

    public StatutoryTaskDTO toStatutoryDTO(StatutoryTask statutoryTask, Long importedTaskId) {
        return new StatutoryTaskDTO(statutoryTask.getId(), statutoryTask.getCode(), statutoryTask.getName(),
                statutoryTask.getShortName(), statutoryTask.getUnitType().name(), statutoryTask.getMaxUnitPrice(), importedTaskId);
    }

    public List<TaskDTO> toDTOList(List<Task> tasks) {
        if (tasks == null) {
            return null;
        }

        return tasks.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
