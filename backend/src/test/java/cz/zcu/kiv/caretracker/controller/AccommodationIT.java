package cz.zcu.kiv.caretracker.controller;

import com.jayway.jsonpath.JsonPath;
import cz.zcu.kiv.caretracker.entity.Department;
import cz.zcu.kiv.caretracker.entity.Employee;
import cz.zcu.kiv.caretracker.entity.Organization;
import cz.zcu.kiv.caretracker.enums.EmployeeRole;
import cz.zcu.kiv.caretracker.service.GeocodingService;
import cz.zcu.kiv.caretracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accommodation of a client: a client in supported housing (DPS) takes the address of the house,
 * other kinds of accommodation keep the address typed in the form. Municipalities of a department
 * are only suggestions for the city, a client may live outside them.
 */
class AccommodationIT extends AbstractIntegrationTest {

    private Employee admin;
    private Employee coordinator;
    private Employee caregiver;
    private Department department;
    private Department otherDepartment;

    @BeforeEach
    void setUp() {
        Organization organization = testData.organization();
        department = testData.department(organization);
        otherDepartment = testData.department(organization);
        admin = testData.employee(department, EmployeeRole.MANAGER);
        coordinator = testData.employee(department, EmployeeRole.COORDINATOR);
        caregiver = testData.employee(department, EmployeeRole.CAREGIVER);
    }

    // ========== Municipalities ==========

    @Test
    void municipalitiesOfDepartmentAreListedByName() throws Exception {
        createMunicipality(department, "Kralovice");
        createMunicipality(department, "Bohy");
        createMunicipality(otherDepartment, "Kožlany");

        mockMvc.perform(get("/api/departments/{id}/municipalities", department.getId()).with(loggedInAs(coordinator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Bohy", "Kralovice")));
    }

    @Test
    void municipalityNeedsName() throws Exception {
        mockMvc.perform(post("/api/departments/{id}/municipalities", department.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "  ")))
                        .with(loggedInAs(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Zadejte název obce"));
    }

    @Test
    void municipalityIsUniqueInDepartmentIgnoringCase() throws Exception {
        createMunicipality(department, "Kralovice");

        mockMvc.perform(post("/api/departments/{id}/municipalities", department.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "kralovice ")))
                        .with(loggedInAs(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Obec už ve středisku existuje"));
    }

    @Test
    void sameMunicipalityMayBelongToAnotherDepartment() throws Exception {
        createMunicipality(department, "Kralovice");

        mockMvc.perform(post("/api/departments/{id}/municipalities", otherDepartment.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Kralovice")))
                        .with(loggedInAs(admin)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void deletedMunicipalityDisappearsFromList() throws Exception {
        long kralovice = createMunicipality(department, "Kralovice");
        createMunicipality(department, "Bohy");

        mockMvc.perform(delete("/api/departments/{id}/municipalities/{municipalityId}", department.getId(), kralovice)
                        .with(loggedInAs(admin)))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/departments/{id}/municipalities", department.getId()).with(loggedInAs(coordinator)))
                .andExpect(jsonPath("$[*].name", contains("Bohy")));
    }

    @Test
    void municipalityOfAnotherDepartmentCannotBeDeletedThroughThisOne() throws Exception {
        long kozlany = createMunicipality(otherDepartment, "Kožlany");

        mockMvc.perform(delete("/api/departments/{id}/municipalities/{municipalityId}", department.getId(), kozlany)
                        .with(loggedInAs(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientMayLiveInMunicipalityOutsideDepartment() throws Exception {
        createMunicipality(department, "Kralovice");
        Map<String, Object> request = clientRequest("FAMILY_HOUSE");
        request.put("city", "Plasy");
        request.put("travelDistanceKm", 12);
        long clientId = createClient(request);

        mockMvc.perform(get("/api/clients/{id}", clientId).with(loggedInAs(coordinator)))
                .andExpect(jsonPath("$.city").value("Plasy"))
                .andExpect(jsonPath("$.travelDistanceKm").value(12));
    }

    // ========== Supported housing (DPS) ==========

    @Test
    void supportedHousingNeedsNameAndAddress() throws Exception {
        mockMvc.perform(post("/api/departments/{id}/supported-housings", department.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "DPS Kralovice", "street", "", "city", "Kralovice", "postalCode", "33141")))
                        .with(loggedInAs(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Prosím zadejte ulici a číslo popisné"));
    }

    @Test
    void supportedHousingsOfDepartmentAreListed() throws Exception {
        createSupportedHousing(department, "DPS Kralovice");
        createSupportedHousing(otherDepartment, "DPS Kožlany");

        mockMvc.perform(get("/api/departments/{id}/supported-housings", department.getId()).with(loggedInAs(coordinator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("DPS Kralovice")))
                .andExpect(jsonPath("$[0].street").value("Plzeňská 400"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void clientInSupportedHousingTakesAddressOfHouse() throws Exception {
        long housing = createSupportedHousing(department, "DPS Kralovice");
        Map<String, Object> request = clientRequest("SUPPORTED_HOUSING");
        request.put("supportedHousingId", housing);
        long clientId = createClient(request);

        mockMvc.perform(get("/api/clients/{id}", clientId).with(loggedInAs(coordinator)))
                .andExpect(jsonPath("$.accommodationType").value("SUPPORTED_HOUSING"))
                .andExpect(jsonPath("$.supportedHousing.id").value(housing))
                .andExpect(jsonPath("$.supportedHousing.name").value("DPS Kralovice"))
                .andExpect(jsonPath("$.street").value("Plzeňská 400"))
                .andExpect(jsonPath("$.city").value("Kralovice"))
                .andExpect(jsonPath("$.postalCode").value("331 41"));
    }

    @Test
    void supportedHousingIsRequiredForThatKindOfAccommodation() throws Exception {
        mockMvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content(json(clientRequest("SUPPORTED_HOUSING")))
                        .with(loggedInAs(coordinator)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vyberte dům s pečovatelskou službou"));
    }

    @Test
    void supportedHousingMustBelongToClientsDepartment() throws Exception {
        long otherHousing = createSupportedHousing(otherDepartment, "DPS Kožlany");
        Map<String, Object> request = clientRequest("SUPPORTED_HOUSING");
        request.put("supportedHousingId", otherHousing);

        mockMvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content(json(request))
                        .with(loggedInAs(coordinator)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dům s pečovatelskou službou nepatří do střediska klienta"));
    }

    @Test
    void terminatedSupportedHousingCannotBeChosen() throws Exception {
        long housing = createSupportedHousing(department, "DPS Kralovice");
        mockMvc.perform(put("/api/departments/{id}/supported-housings/{housingId}/terminate", department.getId(), housing)
                        .with(loggedInAs(admin)))
                .andExpect(status().is2xxSuccessful());
        Map<String, Object> request = clientRequest("SUPPORTED_HOUSING");
        request.put("supportedHousingId", housing);

        mockMvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content(json(request))
                        .with(loggedInAs(coordinator)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dům s pečovatelskou službou je ukončený"));
    }

    @Test
    void otherKindOfAccommodationKeepsTypedAddressAndDropsHousing() throws Exception {
        long housing = createSupportedHousing(department, "DPS Kralovice");
        Map<String, Object> request = clientRequest("RENTED_FLAT");
        request.put("supportedHousingId", housing);
        long clientId = createClient(request);

        mockMvc.perform(get("/api/clients/{id}", clientId).with(loggedInAs(coordinator)))
                .andExpect(jsonPath("$.accommodationType").value("RENTED_FLAT"))
                .andExpect(jsonPath("$.supportedHousing").doesNotExist())
                .andExpect(jsonPath("$.street").value("Americká 12"));
    }

    @Test
    void changedAddressOfHouseMovesItsClients() throws Exception {
        long housing = createSupportedHousing(department, "DPS Kralovice");
        Map<String, Object> request = clientRequest("SUPPORTED_HOUSING");
        request.put("supportedHousingId", housing);
        long clientId = createClient(request);

        mockMvc.perform(put("/api/departments/{id}/supported-housings/{housingId}", department.getId(), housing)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(housingRequest("DPS Kralovice", "Žatecká 12")))
                        .with(loggedInAs(admin)))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/clients/{id}", clientId).with(loggedInAs(coordinator)))
                .andExpect(jsonPath("$.street").value("Žatecká 12"));
    }

    @Test
    void changedAddressOfHouseMovesCoordinatesOfItsClients() throws Exception {
        long housing = createSupportedHousing(department, "DPS Kralovice");
        Map<String, Object> request = clientRequest("SUPPORTED_HOUSING");
        request.put("supportedHousingId", housing);
        long clientId = createClient(request);
        when(geocodingService.geocode("Žatecká 12", "Kralovice", "331 41"))
                .thenReturn(Optional.of(new GeocodingService.GeocodeResult(50.1, 13.5)));

        mockMvc.perform(put("/api/departments/{id}/supported-housings/{housingId}", department.getId(), housing)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(housingRequest("DPS Kralovice", "Žatecká 12")))
                        .with(loggedInAs(admin)))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/clients/{id}", clientId).with(loggedInAs(coordinator)))
                .andExpect(jsonPath("$.latitude").value(50.1))
                .andExpect(jsonPath("$.longitude").value(13.5));
    }

    @Test
    void housePostalCodeInWrongFormatIsRejected() throws Exception {
        mockMvc.perform(post("/api/departments/{id}/supported-housings", department.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "DPS", "street", "Ulice 1", "city", "Kralovice", "postalCode", "12345")))
                        .with(loggedInAs(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("PSČ ve tvaru 123 45"));
    }

    @Test
    void clientWithoutAccommodationTypeCanStillBeSaved() throws Exception {
        long clientId = testData.client(caregiver).getId();

        mockMvc.perform(put("/api/clients/{id}", clientId).contentType(MediaType.APPLICATION_JSON)
                        .content(json(clientRequest(null)))
                        .with(loggedInAs(coordinator)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.accommodationType").doesNotExist());
    }

    // ========== Access to the flat ==========

    @Test
    void accessDetailsAreSavedAndShownInDetail() throws Exception {
        Map<String, Object> request = clientRequest("OWN_FLAT");
        request.put("hasElevator", false);
        request.put("floorOrRoom", "3. patro");
        request.put("accessMethod", "KEYS");
        request.put("lockCode", "4711");
        request.put("accessNote", "Klíče u sousedky v 2. patře");
        long clientId = createClient(request);

        mockMvc.perform(get("/api/clients/{id}", clientId).with(loggedInAs(caregiver)))
                .andExpect(jsonPath("$.hasElevator").value(false))
                .andExpect(jsonPath("$.floorOrRoom").value("3. patro"))
                .andExpect(jsonPath("$.accessMethod").value("KEYS"))
                .andExpect(jsonPath("$.lockCode").value("4711"))
                .andExpect(jsonPath("$.accessNote").value("Klíče u sousedky v 2. patře"));
    }

    @Test
    void lockCodeIsNotInClientList() throws Exception {
        Map<String, Object> request = clientRequest("OWN_FLAT");
        request.put("accessMethod", "KEYS");
        request.put("lockCode", "4711");
        createClient(request);

        mockMvc.perform(get("/api/clients").with(loggedInAs(coordinator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]", not(hasKey("lockCode"))));
    }

    @Test
    void travelDistanceCannotBeNegative() throws Exception {
        Map<String, Object> request = clientRequest("FAMILY_HOUSE");
        request.put("travelDistanceKm", -1);

        mockMvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content(json(request))
                        .with(loggedInAs(coordinator)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dojezdová vzdálenost nesmí být záporná"));
    }

    private long createMunicipality(Department target, String name) throws Exception {
        String response = mockMvc.perform(post("/api/departments/{id}/municipalities", target.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name)))
                        .with(loggedInAs(admin)))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private long createSupportedHousing(Department target, String name) throws Exception {
        String response = mockMvc.perform(post("/api/departments/{id}/supported-housings", target.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(housingRequest(name, "Plzeňská 400")))
                        .with(loggedInAs(admin)))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private long createClient(Map<String, Object> request) throws Exception {
        String response = mockMvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content(json(request))
                        .with(loggedInAs(coordinator)))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private static Map<String, Object> housingRequest(String name, String street) {
        return Map.of("name", name, "street", street, "city", "Kralovice", "postalCode", "331 41");
    }

    private Map<String, Object> clientRequest(String accommodationType) {
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
                Map.entry("departmentId", department.getId()),
                Map.entry("caregiverId", caregiver.getId()),
                Map.entry("taskIds", List.of())));
        request.put("accommodationType", accommodationType);
        return request;
    }
}
