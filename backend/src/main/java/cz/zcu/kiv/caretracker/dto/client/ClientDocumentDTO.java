package cz.zcu.kiv.caretracker.dto.client;

import cz.zcu.kiv.caretracker.enums.ClientDocumentType;

import java.time.LocalDateTime;

/** Client document without its file data. */
public record ClientDocumentDTO(
        Long id,
        ClientDocumentType type,
        String fileName,
        String contentType,
        Long size,
        LocalDateTime uploadedAt,
        String uploadedByName
) {
}
