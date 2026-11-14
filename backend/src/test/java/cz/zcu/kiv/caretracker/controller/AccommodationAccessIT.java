package cz.zcu.kiv.caretracker.controller;

import com.jayway.jsonpath.JsonPath;
import cz.zcu.kiv.caretracker.support.AbstractIntegrationTest;
import cz.zcu.kiv.caretracker.support.AccessScenario;
import cz.zcu.kiv.caretracker.support.AccessScenario.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static cz.zcu.kiv.caretracker.support.AccessScenario.Actor.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Who may read and change municipalities and supported housing (DPS) of the scenario's department.
 * Staff of the organization reads them like the department itself; admins and the coordinator of that department change them.
 * Every matrix test runs once per {@link Actor}; allowed actors get 2xx, everyone else 403.
 */
class AccommodationAccessIT extends AbstractIntegrationTest {

    private static final Set<Actor> ORGANIZATION_STAFF = EnumSet.of(SUPERADMIN, ADMIN, COORDINATOR, COORDINATOR_OTHER_DEPT,
            CAREGIVER_OF_CLIENT, CAREGIVER, CAREGIVER_OTHER_DEPT);
    private static final Set<Actor> ACCOMMODATION_EDITORS = EnumSet.of(SUPERADMIN, ADMIN, COORDINATOR);

    private AccessScenario scenario;
    @BeforeEach
    void createScenario() {
        scenario = new AccessScenario(testData);
    }

    @ParameterizedTest
    @EnumSource(Actor.class)
    void readMunicipalities(Actor actor) throws Exception {
        perform(actor, get("/api/departments/{id}/municipalities", scenario.department.getId()))
                .andExpect(allowedOnlyFor(ORGANIZATION_STAFF, actor));
    }

    @ParameterizedTest
    @EnumSource(Actor.class)
    void addMunicipality(Actor actor) throws Exception {
        perform(actor, post("/api/departments/{id}/municipalities", scenario.department.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Bohy"))))
                .andExpect(allowedOnlyFor(ACCOMMODATION_EDITORS, actor));
    }

    @ParameterizedTest
    @EnumSource(Actor.class)
    void deleteMunicipality(Actor actor) throws Exception {
        long municipalityId = createAsAdmin("/api/departments/{id}/municipalities", Map.of("name", "Kralovice"));

        perform(actor, delete("/api/departments/{id}/municipalities/{municipalityId}", scenario.department.getId(), municipalityId))
                .andExpect(allowedOnlyFor(ACCOMMODATION_EDITORS, actor));
    }

    @ParameterizedTest
    @EnumSource(Actor.class)
    void readSupportedHousings(Actor actor) throws Exception {
        perform(actor, get("/api/departments/{id}/supported-housings", scenario.department.getId()))
                .andExpect(allowedOnlyFor(ORGANIZATION_STAFF, actor));
    }

    @ParameterizedTest
    @EnumSource(Actor.class)
    void addSupportedHousing(Actor actor) throws Exception {
        perform(actor, post("/api/departments/{id}/supported-housings", scenario.department.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(housingRequest())))
                .andExpect(allowedOnlyFor(ACCOMMODATION_EDITORS, actor));
    }

    @ParameterizedTest
    @EnumSource(Actor.class)
    void updateSupportedHousing(Actor actor) throws Exception {
        long housingId = createSupportedHousingAsAdmin();

        perform(actor, put("/api/departments/{id}/supported-housings/{housingId}", scenario.department.getId(), housingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(housingRequest())))
                .andExpect(allowedOnlyFor(ACCOMMODATION_EDITORS, actor));
    }

    @ParameterizedTest
    @EnumSource(Actor.class)
    void terminateSupportedHousing(Actor actor) throws Exception {
        long housingId = createSupportedHousingAsAdmin();

        perform(actor, put("/api/departments/{id}/supported-housings/{housingId}/terminate", scenario.department.getId(), housingId))
                .andExpect(allowedOnlyFor(ACCOMMODATION_EDITORS, actor));
    }

    private long createSupportedHousingAsAdmin() throws Exception {
        return createAsAdmin("/api/departments/{id}/supported-housings", housingRequest());
    }

    private long createAsAdmin(String path, Map<String, Object> body) throws Exception {
        String response = perform(ADMIN, post(path, scenario.department.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private ResultActions perform(Actor actor, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(loggedInAs(scenario.user(actor))));
    }

    private static ResultMatcher allowedOnlyFor(Set<Actor> allowed, Actor actor) {
        return allowed.contains(actor) ? status().is2xxSuccessful() : status().isForbidden();
    }

    private static Map<String, Object> housingRequest() {
        return Map.of("name", "DPS Kralovice", "street", "Plzeňská 400", "city", "Kralovice", "postalCode", "331 41");
    }
}
