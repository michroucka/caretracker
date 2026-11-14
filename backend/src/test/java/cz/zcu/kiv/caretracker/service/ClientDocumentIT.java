package cz.zcu.kiv.caretracker.service;

import com.jayway.jsonpath.JsonPath;
import cz.zcu.kiv.caretracker.entity.Client;
import cz.zcu.kiv.caretracker.entity.ClientDocument;
import cz.zcu.kiv.caretracker.entity.Department;
import cz.zcu.kiv.caretracker.entity.Employee;
import cz.zcu.kiv.caretracker.enums.ClientDocumentType;
import cz.zcu.kiv.caretracker.enums.EmployeeRole;
import cz.zcu.kiv.caretracker.repository.ClientDocumentRepository;
import cz.zcu.kiv.caretracker.repository.ClientRepository;
import cz.zcu.kiv.caretracker.support.AbstractIntegrationTest;
import cz.zcu.kiv.caretracker.support.TestData;
import cz.zcu.kiv.caretracker.support.TestImages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Client documents: PDF, JPEG or PNG files of a type (contract, intake assessment, key consent, other), checked by
 * their content, listed without their data and downloaded with the original file name. A terminated client's
 * documents stay readable but can no longer change. Who may do what is covered by ClientDocumentAccessIT.
 */
class ClientDocumentIT extends AbstractIntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 13, 10, 0);

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientDocumentRepository clientDocumentRepository;

    private Employee coordinator;
    private Client client;

    @BeforeEach
    void setUp() {
        clock.fixAt(NOW);
        Department department = testData.department(testData.organization());
        coordinator = testData.employee(department, EmployeeRole.COORDINATOR);
        client = testData.client(testData.employee(department, EmployeeRole.CAREGIVER));
    }

    @Test
    void uploadedPdfIsDownloadedWithSameContentAndName() throws Exception {
        String response = upload(new MockMultipartFile("file", "Smlouva Nováková.pdf", "application/pdf", TestData.PDF), "CONTRACT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CONTRACT"))
                .andExpect(jsonPath("$.fileName").value("Smlouva Nováková.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.size").value(TestData.PDF.length))
                .andExpect(jsonPath("$.uploadedAt").value("2026-11-13T10:00:00"))
                .andExpect(jsonPath("$.uploadedByName").value(coordinator.getFirstName() + " " + coordinator.getLastName()))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(response, "$.id");

        perform(get("/api/clients/{id}/documents/{documentId}", client.getId(), id.longValue()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"Smlouva Novakova.pdf\"; filename*=UTF-8''Smlouva%20Nov%C3%A1kov%C3%A1.pdf"))
                .andExpect(content().bytes(TestData.PDF));
    }

    @Test
    void imageTypeIsTakenFromItsContent() throws Exception {
        upload(new MockMultipartFile("file", "souhlas.png", "application/octet-stream", TestImages.encode(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png")), "KEY_CONSENT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentType").value("image/png"));
    }

    @Test
    void textPretendingToBePdfIsRejected() throws Exception {
        upload(new MockMultipartFile("file", "smlouva.pdf", "application/pdf", "jen text".getBytes(StandardCharsets.UTF_8)), "CONTRACT")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nahrát lze jen PDF, JPEG nebo PNG"));

        assertThat(clientDocumentRepository.count()).isZero();
    }

    @Test
    void emptyFileIsRejected() throws Exception {
        upload(new MockMultipartFile("file", "smlouva.pdf", "application/pdf", new byte[0]), "CONTRACT")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Soubor je prázdný"));
    }

    @Test
    void missingTypeIsRejected() throws Exception {
        perform(multipart("/api/clients/{id}/documents", client.getId())
                .file(new MockMultipartFile("file", "smlouva.pdf", "application/pdf", TestData.PDF)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Prosím vyberte typ dokumentu"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVOICE", "contract"})
    void unknownTypeIsRejected(String type) throws Exception {
        upload(new MockMultipartFile("file", "smlouva.pdf", "application/pdf", TestData.PDF), type)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Neznámý typ dokumentu"));
    }

    @Test
    void listIsNewestFirstAndHasNoFileData() throws Exception {
        ClientDocument older = testData.clientDocument(client, ClientDocumentType.CONTRACT, NOW.minusDays(2));
        ClientDocument newer = testData.clientDocument(client, ClientDocumentType.OTHER, NOW.minusDays(1));
        testData.clientDocument(testData.client(coordinator), ClientDocumentType.OTHER, NOW);

        perform(get("/api/clients/{id}/documents", client.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(newer.getId()))
                .andExpect(jsonPath("$[1].id").value(older.getId()))
                .andExpect(jsonPath("$[0].data").doesNotExist());
    }

    @Test
    void documentOfAnotherClientIsNotFound() throws Exception {
        Client otherClient = testData.client(coordinator);
        ClientDocument foreign = testData.clientDocument(otherClient);

        perform(get("/api/clients/{id}/documents/{documentId}", client.getId(), foreign.getId()))
                .andExpect(status().isNotFound());
        perform(delete("/api/clients/{id}/documents/{documentId}", client.getId(), foreign.getId()))
                .andExpect(status().isNotFound());

        assertThat(clientDocumentRepository.existsById(foreign.getId())).isTrue();
    }

    @Test
    void deletedDocumentIsGone() throws Exception {
        ClientDocument document = testData.clientDocument(client);

        perform(delete("/api/clients/{id}/documents/{documentId}", client.getId(), document.getId()))
                .andExpect(status().isNoContent());

        assertThat(clientDocumentRepository.existsById(document.getId())).isFalse();
    }

    @Test
    void terminatedClientsDocumentsAreReadOnly() throws Exception {
        ClientDocument document = testData.clientDocument(client);
        client.setActive(false);
        clientRepository.save(client);

        perform(get("/api/clients/{id}/documents", client.getId())).andExpect(status().isOk());
        perform(get("/api/clients/{id}/documents/{documentId}", client.getId(), document.getId())).andExpect(status().isOk());
        upload(new MockMultipartFile("file", "smlouva.pdf", "application/pdf", TestData.PDF), "CONTRACT")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Klient je ukončený, dokumenty už nelze měnit"));
        perform(delete("/api/clients/{id}/documents/{documentId}", client.getId(), document.getId()))
                .andExpect(status().isBadRequest());

        assertThat(clientDocumentRepository.existsById(document.getId())).isTrue();
    }

    @Test
    void documentUpToTenMegabytesIsAcceptedAndBiggerRefused() throws Exception {
        upload(new MockMultipartFile("file", "sken.pdf", "application/pdf", pdfOfSize(10 * 1024 * 1024)), "CONTRACT")
                .andExpect(status().isOk());

        upload(new MockMultipartFile("file", "sken.pdf", "application/pdf", pdfOfSize(10 * 1024 * 1024 + 1)), "CONTRACT")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Soubor je příliš velký. Maximální velikost je 10 MB"));
    }

    private static byte[] pdfOfSize(int size) {
        byte[] data = new byte[size];
        System.arraycopy(TestData.PDF, 0, data, 0, TestData.PDF.length);
        return data;
    }

    private ResultActions upload(MockMultipartFile file, String type) throws Exception {
        return perform(multipart("/api/clients/{id}/documents", client.getId()).file(file).param("type", type));
    }

    private ResultActions perform(AbstractMockHttpServletRequestBuilder<?> request) throws Exception {
        return mockMvc.perform(request.with(loggedInAs(coordinator)));
    }
}
