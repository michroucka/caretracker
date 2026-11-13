package cz.zcu.kiv.caretracker.controller;

import com.jayway.jsonpath.JsonPath;
import cz.zcu.kiv.caretracker.entity.Client;
import cz.zcu.kiv.caretracker.entity.Department;
import cz.zcu.kiv.caretracker.entity.Employee;
import cz.zcu.kiv.caretracker.entity.Organization;
import cz.zcu.kiv.caretracker.enums.EmployeeRole;
import cz.zcu.kiv.caretracker.repository.ClientRepository;
import cz.zcu.kiv.caretracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

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
 * The personal number is the two-digit department number followed by a four-digit sequence within the department
 * (XXYYYY). It goes into the 10-digit payment variable symbol after year and month (YYMM), so it never changes
 * and is never given to another client of the organization, not even after the client was terminated.
 */
class ClientPersonalNumberIT extends AbstractIntegrationTest {

    @Autowired
    private ClientRepository clientRepository;

    private Organization organization;
    private Department firstDepartment;
    private Department secondDepartment;
    private Employee coordinator;
    private Employee caregiver;

    @BeforeEach
    void setUp() {
        organization = testData.organization();
        firstDepartment = testData.department(organization);
        secondDepartment = testData.department(organization);
        coordinator = testData.employee(firstDepartment, EmployeeRole.COORDINATOR);
        caregiver = testData.employee(firstDepartment, EmployeeRole.CAREGIVER);
    }

    @Test
    void clientsWithoutNumberGetDepartmentNumberAndSequence() throws Exception {
        assertThat(firstDepartment.getDepartmentNumber()).isEqualTo(1);

        assertThat(createdPersonalNumber(clientRequest(null))).isEqualTo("010001");
        assertThat(createdPersonalNumber(clientRequest(null))).isEqualTo("010002");
    }

    @Test
    void numberOfTerminatedClientIsNotGivenAgain() throws Exception {
        Client terminated = testData.client(caregiver, "010007");
        terminated.setActive(false);
        clientRepository.save(terminated);

        assertThat(createdPersonalNumber(clientRequest(null))).isEqualTo("010008");
    }

    @Test
    void movedClientKeepsNumberAndItsSequenceStaysTaken() throws Exception {
        Client moved = testData.client(caregiver, "010005");
        Employee otherCaregiver = testData.employee(secondDepartment, EmployeeRole.CAREGIVER);
        Map<String, Object> move = clientRequest(null);
        move.put("departmentId", secondDepartment.getId());
        move.put("caregiverId", otherCaregiver.getId());

        mockMvc.perform(put("/api/clients/" + moved.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(move))
                        .with(loggedInAs(testData.employee(firstDepartment, EmployeeRole.MANAGER))))
                .andExpect(status().is2xxSuccessful());

        assertThat(clientRepository.findById(moved.getId()).orElseThrow().getPersonalNumber()).isEqualTo("010005");
        assertThat(createdPersonalNumber(clientRequest(null))).isEqualTo("010006");
    }

    @Test
    void nextPersonalNumberIsTheOneThatWouldBeGiven() throws Exception {
        testData.client(caregiver, "010003");

        mockMvc.perform(get("/api/departments/{id}/next-personal-number", firstDepartment.getId()).with(loggedInAs(coordinator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalNumber").value("010004"));

        assertThat(createdPersonalNumber(clientRequest(null))).isEqualTo("010004");
    }

    @Test
    void coordinatorCannotAskForNumberOfAnotherDepartment() throws Exception {
        mockMvc.perform(get("/api/departments/{id}/next-personal-number", secondDepartment.getId()).with(loggedInAs(coordinator)))
                .andExpect(status().isForbidden());
    }

    @Test
    void caregiverCannotAskForNextNumber() throws Exception {
        mockMvc.perform(get("/api/departments/{id}/next-personal-number", firstDepartment.getId()).with(loggedInAs(caregiver)))
                .andExpect(status().isForbidden());
    }

    @Test
    void departmentWithoutFreeNumberIsReported() throws Exception {
        testData.client(caregiver, "019999");

        create(clientRequest(null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ve středisku došla osobní čísla"));
    }

    @Test
    void manualNumberIsKept() throws Exception {
        assertThat(createdPersonalNumber(clientRequest("020099"))).isEqualTo("020099");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567", "01a001"})
    void numberWithoutSixDigitsIsRejectedOnCreate(String personalNumber) throws Exception {
        create(clientRequest(personalNumber))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Osobní číslo musí mít 6 číslic"));
    }

    @Test
    void numberWithoutSixDigitsIsRejectedOnUpdate() throws Exception {
        Long clientId = testData.client(caregiver).getId();

        mockMvc.perform(put("/api/clients/" + clientId).contentType(MediaType.APPLICATION_JSON)
                        .content(json(clientRequest("12345")))
                        .with(loggedInAs(coordinator)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Osobní číslo musí mít 6 číslic"));
    }

    @Test
    void numberOfTerminatedClientCannotBeEnteredManually() throws Exception {
        Client terminated = testData.client(caregiver, "010042");
        terminated.setActive(false);
        clientRepository.save(terminated);

        create(clientRequest("010042"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Osobní číslo je už obsazené"));
    }

    @Test
    void clientKeepsOwnNumberOnUpdate() throws Exception {
        Client client = testData.client(caregiver, "010010");

        mockMvc.perform(put("/api/clients/" + client.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(clientRequest("010010")))
                        .with(loggedInAs(coordinator)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void sameNumberIsAllowedInAnotherOrganization() throws Exception {
        Department foreignDepartment = testData.department(testData.organization());
        testData.client(testData.employee(foreignDepartment, EmployeeRole.CAREGIVER), "010001");

        assertThat(createdPersonalNumber(clientRequest(null))).isEqualTo("010001");
    }

    private String createdPersonalNumber(Map<String, Object> request) throws Exception {
        String response = create(request).andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(response, "$.id");
        return clientRepository.findById(id.longValue()).orElseThrow().getPersonalNumber();
    }

    private ResultActions create(Map<String, Object> request) throws Exception {
        return mockMvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON)
                .content(json(request))
                .with(loggedInAs(coordinator)));
    }

    private Map<String, Object> clientRequest(String personalNumber) {
        Map<String, Object> request = new HashMap<>(Map.ofEntries(
                Map.entry("firstName", "Marie"),
                Map.entry("lastName", "Nová"),
                Map.entry("gender", "FEMALE"),
                Map.entry("dateOfBirth", "1948-03-12"),
                Map.entry("street", "Americká 12"),
                Map.entry("city", "Plzeň"),
                Map.entry("postalCode", "301 00"),
                Map.entry("legallyCompetent", true),
                Map.entry("benefits", "THREE"),
                Map.entry("departmentId", firstDepartment.getId()),
                Map.entry("caregiverId", caregiver.getId()),
                Map.entry("taskIds", List.of())));
        request.put("personalNumber", personalNumber);
        return request;
    }
}
