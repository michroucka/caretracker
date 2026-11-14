package cz.zcu.kiv.caretracker.service;

import cz.zcu.kiv.caretracker.dto.client.ClientDocumentDTO;
import cz.zcu.kiv.caretracker.entity.Client;
import cz.zcu.kiv.caretracker.entity.ClientDocument;
import cz.zcu.kiv.caretracker.entity.Employee;
import cz.zcu.kiv.caretracker.enums.ClientDocumentType;
import cz.zcu.kiv.caretracker.exception.ResourceNotFoundException;
import cz.zcu.kiv.caretracker.exception.ValidationException;
import cz.zcu.kiv.caretracker.repository.ClientDocumentRepository;
import cz.zcu.kiv.caretracker.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Documents of a client. Which roles may use them is set on the endpoints (caregivers may not);
 * here the user must additionally have access to the client's department.
 */
@Service
public class ClientDocumentService extends BaseRoleFilteringService<ClientDocument, ClientDocumentDTO> {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final ClientRepository clientRepository;
    private final ClientDocumentRepository clientDocumentRepository;
    private final Clock clock;

    public ClientDocumentService(ClientRepository clientRepository, ClientDocumentRepository clientDocumentRepository, Clock clock) {
        this.clientRepository = clientRepository;
        this.clientDocumentRepository = clientDocumentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ClientDocumentDTO> getDocuments(Long clientId) {
        validateClientAccess(clientId);
        return clientDocumentRepository.findSummariesByClientId(clientId);
    }

    @Transactional(readOnly = true)
    public ClientDocument getDocument(Long clientId, Long documentId) {
        validateClientAccess(clientId);
        return findDocument(clientId, documentId);
    }

    @Transactional
    public ClientDocumentDTO uploadDocument(Long clientId, String type, MultipartFile file) throws IOException {
        Client client = validateClientAccess(clientId);
        requireActiveClient(client);
        ClientDocumentType documentType = parseType(type);
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Soubor je prázdný");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("Soubor je příliš velký. Maximální velikost je 10 MB");
        }
        byte[] data = file.getBytes();

        ClientDocument document = new ClientDocument();
        document.setClient(client);
        document.setType(documentType);
        document.setFileName(fileName(file.getOriginalFilename()));
        document.setContentType(detectContentType(data));
        document.setSize((long) data.length);
        document.setData(data);
        document.setUploadedAt(LocalDateTime.now(clock));
        document.setUploadedBy(getCurrentUser().getEmployee());
        ClientDocument saved = clientDocumentRepository.save(document);

        return new ClientDocumentDTO(saved.getId(), saved.getType(), saved.getFileName(), saved.getContentType(),
                saved.getSize(), saved.getUploadedAt(), fullName(saved.getUploadedBy()));
    }

    @Transactional
    public void deleteDocument(Long clientId, Long documentId) {
        Client client = validateClientAccess(clientId);
        requireActiveClient(client);
        clientDocumentRepository.delete(findDocument(clientId, documentId));
    }

    private Client validateClientAccess(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Klient nebyl nalezen"));
        validateDepartmentAccess(client, c -> c.getOrganization().getId(), c -> c.getDepartment().getId());
        return client;
    }

    private ClientDocument findDocument(Long clientId, Long documentId) {
        return clientDocumentRepository.findByIdAndClientId(documentId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Dokument nebyl nalezen"));
    }

    private static void requireActiveClient(Client client) {
        if (!Boolean.TRUE.equals(client.getActive())) {
            throw new ValidationException("Klient je ukončený, dokumenty už nelze měnit");
        }
    }

    private static ClientDocumentType parseType(String type) {
        if (type == null || type.isBlank()) {
            throw new ValidationException("Prosím vyberte typ dokumentu");
        }
        return Arrays.stream(ClientDocumentType.values())
                .filter(value -> value.name().equals(type))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Neznámý typ dokumentu"));
    }

    /** Detects the type from the file signature, the client-supplied Content-Type is trivially forgeable. */
    private static String detectContentType(byte[] data) {
        if (startsWith(data, PDF_SIGNATURE)) {
            return "application/pdf";
        }
        if (startsWith(data, PNG_SIGNATURE)) {
            return "image/png";
        }
        if (startsWith(data, JPEG_SIGNATURE)) {
            return "image/jpeg";
        }
        throw new ValidationException("Nahrát lze jen PDF, JPEG nebo PNG");
    }

    private static boolean startsWith(byte[] data, byte[] signature) {
        return data.length >= signature.length
                && Arrays.equals(data, 0, signature.length, signature, 0, signature.length);
    }

    /** Browsers may send a full path (old Internet Explorer), only the last segment is kept. */
    private static String fileName(String originalFileName) {
        String name = originalFileName == null ? "" : originalFileName.substring(
                Math.max(originalFileName.lastIndexOf('/'), originalFileName.lastIndexOf('\\')) + 1).trim();
        if (name.isEmpty()) {
            return "dokument";
        }
        return name.length() > MAX_FILE_NAME_LENGTH ? name.substring(0, MAX_FILE_NAME_LENGTH) : name;
    }

    private static String fullName(Employee employee) {
        return employee == null ? null : employee.getFirstName() + " " + employee.getLastName();
    }
}
