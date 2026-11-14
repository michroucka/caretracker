package cz.zcu.kiv.caretracker.controller;

import cz.zcu.kiv.caretracker.dto.task.StatutoryTaskDTO;
import cz.zcu.kiv.caretracker.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only catalog of home care activities set by decree 505/2006 Sb. */
@RestController
@RequestMapping("/api/statutory-tasks")
public class StatutoryTaskController {

    private final TaskService taskService;

    public StatutoryTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'COORDINATOR', 'CAREGIVER')")
    public ResponseEntity<List<StatutoryTaskDTO>> getStatutoryTasks(@RequestParam(required = false) Long organizationId) {
        return ResponseEntity.ok(taskService.getStatutoryTasks(organizationId));
    }
}
