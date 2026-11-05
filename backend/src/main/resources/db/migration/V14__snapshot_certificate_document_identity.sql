ALTER TABLE certificate_documents
    ADD COLUMN participant_document_type VARCHAR(40),
    ADD COLUMN participant_document_number VARCHAR(100);

UPDATE certificate_documents certificate
SET participant_document_type = participant.document_type,
    participant_document_number = participant.document_number
FROM participants participant
WHERE participant.id = certificate.participant_id;

UPDATE certificate_documents
SET participant_document_type = COALESCE(participant_document_type, ''),
    participant_document_number = COALESCE(participant_document_number, '');

ALTER TABLE certificate_documents
    ALTER COLUMN participant_document_type SET NOT NULL,
    ALTER COLUMN participant_document_number SET NOT NULL;
