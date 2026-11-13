package cz.zcu.kiv.caretracker.service;

import com.jayway.jsonpath.JsonPath;
import cz.zcu.kiv.caretracker.entity.Client;
import cz.zcu.kiv.caretracker.entity.Department;
import cz.zcu.kiv.caretracker.entity.Employee;
import cz.zcu.kiv.caretracker.entity.Organization;
import cz.zcu.kiv.caretracker.entity.User;
import cz.zcu.kiv.caretracker.enums.EmployeeRole;
import cz.zcu.kiv.caretracker.enums.UserRole;
import cz.zcu.kiv.caretracker.repository.ClientRepository;
import cz.zcu.kiv.caretracker.repository.DepartmentRepository;
import cz.zcu.kiv.caretracker.repository.EmployeeRepository;
import cz.zcu.kiv.caretracker.repository.UserRepository;
import cz.zcu.kiv.caretracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Department lifecycle: assigning a coordinator raises their role (employee and account) and replacing them
 * lowers it back, termination is blocked while the department still has active clients or employees
 * and releases the coordinator.
 */
class DepartmentLifecycleIT extends AbstractIntegrationTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    private Organization organization;
    private Department department;
    private Employee admin;

    @BeforeEach
    void createOrganization() {
        organization = testData.organization();
        department = testData.department(organization);
        admin = testData.employee(testData.department(organization), EmployeeRole.MANAGER);
    }

    @Test
    void newDepartmentBelongsToAdminsOrganizationAndPromotesItsCoordinator() throws Exception {
        Employee caregiver = testData.employee(department, EmployeeRole.CAREGIVER);

        long id = createdId(perform(post("/api/departments"), newDepartmentRequest(caregiver, 90)).andExpect(status().isOk()));

        Department saved = departmentRepository.findById(id).orElseThrow();
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getOrganization().getId()).isEqualTo(organization.getId());
        assertThat(saved.getCoordinator().getId()).isEqualTo(caregiver.getId());
        assertRoles(caregiver, EmployeeRole.COORDINATOR, UserRole.COORDINATOR);
    }

    @Test
    void replacedCoordinatorFallsBackToCaregiver() throws Exception {
        Employee previous = testData.employee(department, EmployeeRole.CAREGIVER);
        Employee next = testData.employee(department, EmployeeRole.CAREGIVER);
        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, previous)).andExpect(status().isOk());

        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, next)).andExpect(status().isOk());

        assertRoles(previous, EmployeeRole.CAREGIVER, UserRole.CAREGIVER);
        assertRoles(next, EmployeeRole.COORDINATOR, UserRole.COORDINATOR);
    }

    @Test
    void removedCoordinatorFallsBackToCaregiver() throws Exception {
        Employee coordinator = testData.employee(department, EmployeeRole.CAREGIVER);
        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, coordinator)).andExpect(status().isOk());

        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, null)).andExpect(status().isOk());

        assertThat(departmentRepository.findById(department.getId()).orElseThrow().getCoordinator()).isNull();
        assertRoles(coordinator, EmployeeRole.CAREGIVER, UserRole.CAREGIVER);
    }

    @Test
    void keepingTheSameCoordinatorChangesNoRole() throws Exception {
        Employee coordinator = testData.employee(department, EmployeeRole.CAREGIVER);
        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, coordinator)).andExpect(status().isOk());

        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, coordinator)).andExpect(status().isOk());

        assertRoles(coordinator, EmployeeRole.COORDINATOR, UserRole.COORDINATOR);
    }

    @Test
    void managerCoordinatingDepartmentKeepsManagerRole() throws Exception {
        Employee manager = testData.employee(department, EmployeeRole.MANAGER);

        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, manager)).andExpect(status().isOk());
        assertRoles(manager, EmployeeRole.MANAGER, UserRole.ADMIN);

        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, null)).andExpect(status().isOk());
        assertRoles(manager, EmployeeRole.MANAGER, UserRole.ADMIN);
    }

    @Test
    void adminAccountSurvivesCoordinatorChanges() throws Exception {
        Employee caregiverWithAdminAccount = testData.employee(department, EmployeeRole.CAREGIVER);
        User account = caregiverWithAdminAccount.getUser();
        account.setRole(UserRole.ADMIN);
        userRepository.save(account);

        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, caregiverWithAdminAccount)).andExpect(status().isOk());
        assertRoles(caregiverWithAdminAccount, EmployeeRole.COORDINATOR, UserRole.ADMIN);

        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, null)).andExpect(status().isOk());
        assertRoles(caregiverWithAdminAccount, EmployeeRole.CAREGIVER, UserRole.ADMIN);
    }

    @Test
    void activeClientBlocksTermination() throws Exception {
        Employee caregiver = testData.employee(department, EmployeeRole.CAREGIVER);
        testData.client(caregiver);
        caregiver.setActive(false);
        employeeRepository.save(caregiver);

        perform(put("/api/departments/{id}/terminate", department.getId())).andExpect(status().isBadRequest());

        assertThat(departmentRepository.findById(department.getId()).orElseThrow().getActive()).isTrue();
    }

    @Test
    void activeEmployeeBlocksTermination() throws Exception {
        Employee coordinator = testData.employee(department, EmployeeRole.CAREGIVER);
        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, coordinator)).andExpect(status().isOk());

        perform(put("/api/departments/{id}/terminate", department.getId())).andExpect(status().isBadRequest());

        Department saved = departmentRepository.findById(department.getId()).orElseThrow();
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getCoordinator().getId()).isEqualTo(coordinator.getId());
        assertRoles(coordinator, EmployeeRole.COORDINATOR, UserRole.COORDINATOR);
    }

    @Test
    void departmentWithOnlyInactivePeopleIsTerminatedAndReleasesCoordinator() throws Exception {
        Employee formerCaregiver = testData.employee(department, EmployeeRole.CAREGIVER);
        Client formerClient = testData.client(formerCaregiver);
        formerClient.setActive(false);
        clientRepository.save(formerClient);
        formerCaregiver.setActive(false);
        employeeRepository.save(formerCaregiver);
        Employee coordinatorFromElsewhere = testData.employee(testData.department(organization), EmployeeRole.CAREGIVER);
        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, coordinatorFromElsewhere)).andExpect(status().isOk());

        perform(put("/api/departments/{id}/terminate", department.getId())).andExpect(status().isOk());

        Department saved = departmentRepository.findById(department.getId()).orElseThrow();
        assertThat(saved.getActive()).isFalse();
        assertThat(saved.getCoordinator()).isNull();
        assertRoles(coordinatorFromElsewhere, EmployeeRole.CAREGIVER, UserRole.CAREGIVER);
    }

    @Test
    void activationRestoresDepartmentWithoutCoordinator() throws Exception {
        perform(put("/api/departments/{id}/terminate", department.getId())).andExpect(status().isOk());

        perform(put("/api/departments/{id}/activate", department.getId())).andExpect(status().isOk());

        Department saved = departmentRepository.findById(department.getId()).orElseThrow();
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getCoordinator()).isNull();
    }

    @Test
    void coordinatorOfAnotherDepartmentCannotBeAssigned() throws Exception {
        Department otherDepartment = testData.department(organization);
        Employee coordinator = testData.employee(otherDepartment, EmployeeRole.CAREGIVER);
        perform(put("/api/departments/{id}", otherDepartment.getId()), departmentRequest(otherDepartment, coordinator)).andExpect(status().isOk());

        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, coordinator)).andExpect(status().isBadRequest());

        assertThat(departmentRepository.findById(department.getId()).orElseThrow().getCoordinator()).isNull();
    }

    @Test
    void employeeStillCoordinatingAnotherDepartmentStaysCoordinator() throws Exception {
        Department otherDepartment = testData.department(organization);
        Employee coordinator = testData.employee(otherDepartment, EmployeeRole.CAREGIVER);
        perform(put("/api/departments/{id}", otherDepartment.getId()), departmentRequest(otherDepartment, coordinator)).andExpect(status().isOk());
        department.setCoordinator(coordinator);
        departmentRepository.save(department);

        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, null)).andExpect(status().isOk());

        assertRoles(coordinator, EmployeeRole.COORDINATOR, UserRole.COORDINATOR);
    }

    @Test
    void departmentNumberIsRequired() throws Exception {
        perform(post("/api/departments"), newDepartmentRequest(null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Číslo střediska je povinné"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100})
    void departmentNumberOutsideRangeIsRejected(int departmentNumber) throws Exception {
        perform(post("/api/departments"), newDepartmentRequest(null, departmentNumber))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Číslo střediska musí být mezi 1 a 99"));
    }

    @Test
    void departmentNumberMustBeUniqueWithinOrganization() throws Exception {
        perform(post("/api/departments"), newDepartmentRequest(null, department.getDepartmentNumber()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Středisko s tímto číslem už v organizaci existuje"));
    }

    @Test
    void departmentKeepsItsOwnNumberOnUpdate() throws Exception {
        perform(put("/api/departments/{id}", department.getId()), departmentRequest(department, null)).andExpect(status().isOk());

        assertThat(departmentRepository.findById(department.getId()).orElseThrow().getDepartmentNumber())
                .isEqualTo(department.getDepartmentNumber());
    }

    @Test
    void sameDepartmentNumberIsAllowedInAnotherOrganization() throws Exception {
        Organization otherOrganization = testData.organization();
        testData.department(otherOrganization);
        testData.department(otherOrganization);
        Department foreignDepartment = testData.department(otherOrganization);

        perform(post("/api/departments"), newDepartmentRequest(null, foreignDepartment.getDepartmentNumber()))
                .andExpect(status().isOk());
    }

    private void assertRoles(Employee employee, EmployeeRole employeeRole, UserRole accountRole) {
        Employee saved = employeeRepository.findById(employee.getId()).orElseThrow();
        assertThat(saved.getRole()).isEqualTo(employeeRole);
        assertThat(saved.getUser().getRole()).isEqualTo(accountRole);
    }

    private long createdId(ResultActions result) throws Exception {
        Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(loggedInAs(admin)));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, Map<String, Object> body) throws Exception {
        return perform(request.contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private static Map<String, Object> departmentRequest(Department department, Employee coordinator) {
        return newDepartmentRequest(coordinator, department.getDepartmentNumber());
    }

    private static Map<String, Object> newDepartmentRequest(Employee coordinator, Integer departmentNumber) {
        Map<String, Object> request = new HashMap<>();
        request.put("street", "Náměstí 1");
        request.put("city", "Plzeň");
        request.put("postalCode", "301 00");
        request.put("departmentNumber", departmentNumber);
        request.put("coordinatorId", coordinator != null ? coordinator.getId() : null);
        return request;
    }
}
