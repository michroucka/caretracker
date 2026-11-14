package cz.zcu.kiv.caretracker.controller;

import cz.zcu.kiv.caretracker.entity.Client;
import cz.zcu.kiv.caretracker.entity.Department;
import cz.zcu.kiv.caretracker.entity.Employee;
import cz.zcu.kiv.caretracker.enums.EmployeeRole;
import cz.zcu.kiv.caretracker.repository.ClientRepository;
import cz.zcu.kiv.caretracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import com.jayway.jsonpath.JsonPath;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Creating a client with incomplete or invalid data through the real service and mapper:
 * the request is rejected with 400, nothing is stored and the message does not name Java classes.
 */
class ClientCreateValidationIT extends AbstractIntegrationTest {

    private static final String GUARDIAN_REQUIRED = "U nesvéprávného klienta vyplňte jméno a kontakt opatrovníka";

    @Autowired
    private ClientRepository clientRepository;

    private Employee coordinator;
    private Employee caregiver;

    @BeforeEach
    void setUp() {
        Department department = testData.department(testData.organization());
        coordinator = testData.employee(department, EmployeeRole.COORDINATOR);
        caregiver = testData.employee(department, EmployeeRole.CAREGIVER);
    }

    @Test
    void unknownGenderIsRejected() throws Exception {
        createClient(validRequestWith("gender", "ALIEN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Neplatná hodnota v požadavku"));

        assertThat(clientRepository.findAll()).isEmpty();
    }

    @Test
    void unknownBenefitLevelIsRejected() throws Exception {
        createClient(validRequestWith("benefits", "TEN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Neplatná hodnota v požadavku"));

        assertThat(clientRepository.findAll()).isEmpty();
    }

    @Test
    void missingGenderIsRejected() throws Exception {
        createClient(validRequestWith("gender", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Prosím vyberte pohlaví"));
    }

    @Test
    void missingTaskIdsMeanNoTasks() throws Exception {
        createClient(validRequestWith("taskIds", null))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void missingDepartmentIsRejected() throws Exception {
        createClient(validRequestWith("departmentId", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Prosím vyberte středisko"));
    }

    @Test
    void incompetentClientWithoutGuardianNameIsRejected() throws Exception {
        Map<String, Object> request = validRequestWith("legallyCompetent", false);
        request.put("guardianContact", "+420 777 111 222");

        createClient(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(GUARDIAN_REQUIRED));
    }

    @Test
    void incompetentClientWithoutGuardianContactIsRejected() throws Exception {
        Map<String, Object> request = validRequestWith("legallyCompetent", false);
        request.put("guardianName", "Jana Nová");
        request.put("guardianContact", "  ");

        createClient(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(GUARDIAN_REQUIRED));
    }

    @Test
    void updatingOldIncompetentClientWithoutGuardianIsRejected() throws Exception {
        Client oldClient = testData.client(caregiver);
        oldClient.setLegallyCompetent(false);
        clientRepository.save(oldClient);

        mockMvc.perform(put("/api/clients/{id}", oldClient.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequestWith("legallyCompetent", false)))
                        .with(loggedInAs(coordinator)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(GUARDIAN_REQUIRED));
    }

    @Test
    void definiteContractWithoutEndIsRejected() throws Exception {
        Map<String, Object> request = validRequestWith("contractIndefinite", false);
        request.put("contractFrom", "2026-01-01");

        createClient(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("U smlouvy na dobu určitou vyplňte datum konce"));
    }

    @Test
    void contractEndingBeforeItStartsIsRejected() throws Exception {
        Map<String, Object> request = validRequestWith("contractFrom", "2026-05-01");
        request.put("contractTo", "2026-04-30");

        createClient(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Konec smlouvy nesmí být před jejím začátkem"));
    }

    @Test
    void careEndingBeforeItStartsIsRejected() throws Exception {
        Map<String, Object> request = validRequestWith("careFrom", "2026-05-01");
        request.put("careTo", "2026-04-30");

        createClient(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Konec péče nesmí být před jejím začátkem"));
    }

    @Test
    void additionalDetailsAreStoredAndReturned() throws Exception {
        Map<String, Object> request = validRequestWith("legallyCompetent", false);
        request.put("titleBefore", "Ing.");
        request.put("titleAfter", "CSc.");
        request.put("phone2", "+420 777 000 111");
        request.put("guardianName", "Jana Nová");
        request.put("guardianContact", "jana@example.com, +420 777 111 222");
        request.put("careFrom", "2026-01-01");
        request.put("careTo", "2026-12-31");
        request.put("contractFrom", "2026-01-01");
        request.put("contractIndefinite", false);
        request.put("contractTo", "2026-06-30");
        request.put("noCharge", true);
        request.put("intakeAssessmentDate", "2025-12-15");

        String response = createClient(request).andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(get("/api/clients/{id}", id).with(loggedInAs(coordinator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titleBefore").value("Ing."))
                .andExpect(jsonPath("$.titleAfter").value("CSc."))
                .andExpect(jsonPath("$.phone2").value("+420 777 000 111"))
                .andExpect(jsonPath("$.guardianName").value("Jana Nová"))
                .andExpect(jsonPath("$.guardianContact").value("jana@example.com, +420 777 111 222"))
                .andExpect(jsonPath("$.careFrom").value("2026-01-01"))
                .andExpect(jsonPath("$.careTo").value("2026-12-31"))
                .andExpect(jsonPath("$.contractFrom").value("2026-01-01"))
                .andExpect(jsonPath("$.contractTo").value("2026-06-30"))
                .andExpect(jsonPath("$.contractIndefinite").value(false))
                .andExpect(jsonPath("$.noCharge").value(true))
                .andExpect(jsonPath("$.intakeAssessmentDate").value("2025-12-15"));
    }

    @Test
    void requestWithoutAdditionalDetailsUsesDefaults() throws Exception {
        String response = createClient(validRequestWith("firstName", "Marie")).andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(get("/api/clients/{id}", id).with(loggedInAs(coordinator)))
                .andExpect(jsonPath("$.contractIndefinite").value(true))
                .andExpect(jsonPath("$.noCharge").value(false))
                .andExpect(jsonPath("$.guardianName").doesNotExist());
    }

    private ResultActions createClient(Map<String, Object> request) throws Exception {
        return mockMvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON)
                .content(json(request))
                .with(loggedInAs(coordinator)));
    }

    private Map<String, Object> validRequestWith(String field, Object value) {
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
                Map.entry("departmentId", caregiver.getDepartment().getId()),
                Map.entry("caregiverId", caregiver.getId()),
                Map.entry("taskIds", List.of())));
        request.put(field, value);
        return request;
    }
}
