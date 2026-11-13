package cz.zcu.kiv.caretracker.controller;

import cz.zcu.kiv.caretracker.entity.Client;
import cz.zcu.kiv.caretracker.entity.ClientDocument;
import cz.zcu.kiv.caretracker.support.AbstractIntegrationTest;
import cz.zcu.kiv.caretracker.support.AccessScenario;
import cz.zcu.kiv.caretracker.support.AccessScenario.Actor;
import cz.zcu.kiv.caretracker.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import java.util.EnumSet;
import java.util.Set;

import static cz.zcu.kiv.caretracker.support.AccessScenario.Actor.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Client documents (contract, intake assessment, key consent) hold sensitive data the caregivers do not need:
 * only admins and the coordinator of the client's department may read, upload or delete them.
 */
class ClientDocumentAccessIT extends AbstractIntegrationTest {

    private static final Set<Actor> DOCUMENT_MANAGERS = EnumSet.of(SUPERADMIN, ADMIN, COORDINATOR);

    private AccessScenario scenario;
    private Client client;
    private ClientDocument document;

    @BeforeEach
    void createScenario() {
        scenario = new AccessScenario(testData);
        client = scenario.client;
        document = testData.clientDocument(client);
    }

    @ParameterizedTest
    @EnumSource(Actor.class)
    void listDocuments(Actor actor) throws Exception {
        perform(actor, get("/api/clients/{id}/documents", client.getId()))
                .andExpect(allowedOnlyFor(actor));
    }

    @ParameterizedTest
    @EnumSource(Actor.class)
    void downloadDocument(Actor actor) throws Exception {
        perform(actor, get("/api/clients/{id}/documents/{documentId}", client.getId(), document.getId()))
                .andExpect(allowedOnlyFor(actor));
    }

    @ParameterizedTest
    @EnumSource(Actor.class)
    void uploadDocument(Actor actor) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "smlouva.pdf", "application/pdf", TestData.PDF);

        perform(actor, multipart("/api/clients/{id}/documents", client.getId()).file(file).param("type", "CONTRACT"))
                .andExpect(allowedOnlyFor(actor));
    }

    @ParameterizedTest
    @EnumSource(Actor.class)
    void deleteDocument(Actor actor) throws Exception {
        perform(actor, delete("/api/clients/{id}/documents/{documentId}", client.getId(), document.getId()))
                .andExpect(allowedOnlyFor(actor));
    }

    private ResultActions perform(Actor actor, AbstractMockHttpServletRequestBuilder<?> request) throws Exception {
        return mockMvc.perform(request.with(loggedInAs(scenario.user(actor))));
    }

    private static ResultMatcher allowedOnlyFor(Actor actor) {
        return DOCUMENT_MANAGERS.contains(actor) ? status().is2xxSuccessful() : status().isForbidden();
    }
}
