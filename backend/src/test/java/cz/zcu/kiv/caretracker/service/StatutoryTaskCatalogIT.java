package cz.zcu.kiv.caretracker.service;

import cz.zcu.kiv.caretracker.entity.Employee;
import cz.zcu.kiv.caretracker.entity.Organization;
import cz.zcu.kiv.caretracker.entity.StatutoryTask;
import cz.zcu.kiv.caretracker.entity.Task;
import cz.zcu.kiv.caretracker.entity.User;
import cz.zcu.kiv.caretracker.enums.EmployeeRole;
import cz.zcu.kiv.caretracker.enums.UnitType;
import cz.zcu.kiv.caretracker.repository.StatutoryTaskRepository;
import cz.zcu.kiv.caretracker.repository.TaskRepository;
import cz.zcu.kiv.caretracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tasks from the statutory catalog of home care (decree 505/2006 Sb., § 6): the catalog comes with the migration,
 * an organization imports the items it chooses (named by their short name, priced at the maximum) and may link
 * its own tasks to items. A linked task may not cost more than the item's maximum and must use its unit.
 */
class StatutoryTaskCatalogIT extends AbstractIntegrationTest {

    @Autowired
    private StatutoryTaskRepository statutoryTaskRepository;

    @Autowired
    private TaskRepository taskRepository;

    private Organization organization;
    private Employee admin;
    private StatutoryTask dressing;
    private StatutoryTask largeShopping;

    @BeforeEach
    void createOrganization() {
        organization = testData.organization();
        admin = testData.employee(testData.department(organization), EmployeeRole.MANAGER);
        dressing = statutoryTaskRepository.findByCode("a3").orElseThrow();
        largeShopping = statutoryTaskRepository.findByCode("d6").orElseThrow();
    }

    @Test
    void catalogHasAllItemsOfTheDecree() {
        List<StatutoryTask> catalog = statutoryTaskRepository.findAll();

        assertThat(catalog).hasSize(34);
        assertThat(catalog).filteredOn(item -> item.getUnitType() == UnitType.HOUR).hasSize(28)
                .allSatisfy(item -> assertThat(item.getMaxUnitPrice()).isEqualTo(165));
        assertThat(dressing.getShortName()).isEqualTo("Oblékání");
        assertThat(largeShopping.getUnitType()).isEqualTo(UnitType.OCCURRENCE);
        assertThat(largeShopping.getMaxUnitPrice()).isEqualTo(180);
    }

    @Test
    void importCreatesOnlyChosenItemsWithShortNameMaximumAndLink() throws Exception {
        importItems(admin.getUser(), List.of(dressing.getId(), largeShopping.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        List<Task> tasks = taskRepository.findByOrganizationId(organization.getId());
        assertThat(tasks).extracting(Task::getName).containsExactlyInAnyOrder("Oblékání", "Velký nákup");
        Task imported = tasks.stream().filter(task -> task.getName().equals("Oblékání")).findFirst().orElseThrow();
        assertThat(imported.getUnitPrice()).isEqualTo(165);
        assertThat(imported.getUnitType()).isEqualTo(UnitType.HOUR);
        assertThat(imported.getStatutoryTask().getId()).isEqualTo(dressing.getId());
        assertThat(imported.getActive()).isTrue();
    }

    @Test
    void secondImportCreatesNothing() throws Exception {
        importItems(admin.getUser(), List.of(dressing.getId())).andExpect(status().isOk());

        importItems(admin.getUser(), List.of(dressing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        assertThat(taskRepository.findByOrganizationId(organization.getId())).hasSize(1);
    }

    @Test
    void manuallyLinkedTaskIsSkippedByImport() throws Exception {
        Task own = linkedTask("Pomoc s oblékáním", 120);

        importItems(admin.getUser(), List.of(dressing.getId())).andExpect(status().isOk());

        assertThat(taskRepository.findByOrganizationId(organization.getId())).extracting(Task::getId).containsExactly(own.getId());
    }

    @Test
    void emptySelectionIsRejected() throws Exception {
        importItems(admin.getUser(), List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vyberte alespoň jeden úkon"));
    }

    @Test
    void superadminImportsIntoChosenOrganization() throws Exception {
        perform(testData.superadmin(), post("/api/tasks/import-statutory").param("organizationId", organization.getId().toString()),
                Map.of("statutoryTaskIds", List.of(dressing.getId())))
                .andExpect(status().isOk());

        assertThat(taskRepository.findByOrganizationId(organization.getId())).hasSize(1);
    }

    @Test
    void superadminMustChooseOrganizationForImport() throws Exception {
        importItems(testData.superadmin(), List.of(dressing.getId())).andExpect(status().isBadRequest());
    }

    @Test
    void listMarksImportedAndLinkedItems() throws Exception {
        Task own = linkedTask("Pomoc s oblékáním", 120);
        Task foreign = testData.task(testData.organization());
        foreign.setStatutoryTask(largeShopping);
        taskRepository.save(foreign);

        perform(admin.getUser(), get("/api/statutory-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(34))
                .andExpect(jsonPath("$[?(@.code == 'a3')].importedTaskId").value(own.getId().intValue()))
                .andExpect(jsonPath("$[?(@.code == 'a3')].shortName").value("Oblékání"))
                .andExpect(jsonPath("$[?(@.code == 'd6')].importedTaskId").value((Object) null));
    }

    @Test
    void renamedTaskKeepsOfficialName() throws Exception {
        Task own = linkedTask("Oblékání u paní Novákové", 120);

        perform(admin.getUser(), get("/api/tasks/{id}", own.getId()))
                .andExpect(jsonPath("$.name").value("Oblékání u paní Novákové"))
                .andExpect(jsonPath("$.statutoryTaskId").value(dressing.getId()))
                .andExpect(jsonPath("$.statutoryTaskCode").value("a3"))
                .andExpect(jsonPath("$.statutoryTaskName").value(dressing.getName()))
                .andExpect(jsonPath("$.statutoryMaxUnitPrice").value(165));
    }

    @Test
    void existingTaskCanBeLinkedToItem() throws Exception {
        Task task = testData.task(organization, UnitType.HOUR, 150);

        perform(admin.getUser(), put("/api/tasks/{id}", task.getId()), taskRequest(150, "HOUR", dressing.getId()))
                .andExpect(status().isOk());

        assertThat(taskRepository.findById(task.getId()).orElseThrow().getStatutoryTask().getId()).isEqualTo(dressing.getId());
    }

    @Test
    void priceAboveMaximumIsRejected() throws Exception {
        perform(admin.getUser(), post("/api/tasks"), taskRequest(166, "HOUR", dressing.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cena převyšuje maximální úhradu podle vyhlášky (165 Kč)"));

        assertThat(taskRepository.count()).isZero();
    }

    @Test
    void priceAtMaximumIsAccepted() throws Exception {
        perform(admin.getUser(), post("/api/tasks"), taskRequest(165, "HOUR", dressing.getId())).andExpect(status().isOk());
    }

    @Test
    void unitMustMatchItem() throws Exception {
        perform(admin.getUser(), post("/api/tasks"), taskRequest(100, "OCCURRENCE", dressing.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Jednotka úkonu neodpovídá položce vyhlášky"));
    }

    @Test
    void taskWithoutLinkIsNotLimited() throws Exception {
        perform(admin.getUser(), post("/api/tasks"), taskRequest(500, "HOUR", null)).andExpect(status().isOk());
    }

    @Test
    void unlinkingRemovesLink() throws Exception {
        Task own = linkedTask("Pomoc s oblékáním", 120);

        perform(admin.getUser(), put("/api/tasks/{id}", own.getId()), taskRequest(120, "HOUR", null)).andExpect(status().isOk());

        assertThat(taskRepository.findById(own.getId()).orElseThrow().getStatutoryTask()).isNull();
    }

    private Task linkedTask(String name, int unitPrice) {
        Task task = testData.task(organization, UnitType.HOUR, unitPrice);
        task.setName(name);
        task.setStatutoryTask(dressing);
        return taskRepository.save(task);
    }

    private ResultActions importItems(User account, List<Long> statutoryTaskIds) throws Exception {
        return perform(account, post("/api/tasks/import-statutory"), Map.of("statutoryTaskIds", statutoryTaskIds));
    }

    private ResultActions perform(User account, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(loggedInAs(account)));
    }

    private ResultActions perform(User account, MockHttpServletRequestBuilder request, Map<String, Object> body) throws Exception {
        return perform(account, request.contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private static Map<String, Object> taskRequest(int unitPrice, String unitType, Long statutoryTaskId) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "Pomoc s oblékáním");
        request.put("unitPrice", unitPrice);
        request.put("unitType", unitType);
        request.put("doubleMeeting", false);
        request.put("statutoryTaskId", statutoryTaskId);
        return request;
    }
}
