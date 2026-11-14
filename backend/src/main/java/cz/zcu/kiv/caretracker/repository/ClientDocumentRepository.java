package cz.zcu.kiv.caretracker.repository;

import cz.zcu.kiv.caretracker.dto.client.ClientDocumentDTO;
import cz.zcu.kiv.caretracker.entity.ClientDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientDocumentRepository extends JpaRepository<ClientDocument, Long> {

    /** The documents of a client newest first, without loading their file data. */
    @Query("""
            SELECT new cz.zcu.kiv.caretracker.dto.client.ClientDocumentDTO(
                d.id, d.type, d.fileName, d.contentType, d.size, d.uploadedAt,
                CASE WHEN e.id IS NULL THEN NULL ELSE CONCAT(e.firstName, ' ', e.lastName) END)
            FROM ClientDocument d LEFT JOIN d.uploadedBy e
            WHERE d.client.id = :clientId
            ORDER BY d.uploadedAt DESC, d.id DESC
            """)
    List<ClientDocumentDTO> findSummariesByClientId(@Param("clientId") Long clientId);

    Optional<ClientDocument> findByIdAndClientId(Long id, Long clientId);
}
