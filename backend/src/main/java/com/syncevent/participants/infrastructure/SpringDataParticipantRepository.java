package com.syncevent.participants.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataParticipantRepository
        extends JpaRepository<ParticipantEntity, UUID> {

    Optional<ParticipantEntity> findByDocumentTypeAndDocumentNumber(
            String documentType, String documentNumber);

    @Query(
            value = """
                    SELECT p FROM ParticipantEntity p
                    WHERE LOWER(p.documentNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                       OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                       OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
                       OR LOWER(CONCAT(CONCAT(p.firstName, ' '), p.lastName))
                            LIKE LOWER(CONCAT('%', :query, '%'))
                       OR LOWER(p.email) LIKE LOWER(CONCAT('%', :query, '%'))
                    """,
            countQuery = """
                    SELECT COUNT(p) FROM ParticipantEntity p
                    WHERE LOWER(p.documentNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                       OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                       OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
                       OR LOWER(CONCAT(CONCAT(p.firstName, ' '), p.lastName))
                            LIKE LOWER(CONCAT('%', :query, '%'))
                       OR LOWER(p.email) LIKE LOWER(CONCAT('%', :query, '%'))
                    """)
    Page<ParticipantEntity> search(@Param("query") String query, Pageable pageable);
}
