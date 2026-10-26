ALTER TABLE user_accounts
    ADD COLUMN participant_id UUID;

ALTER TABLE user_accounts
    ADD CONSTRAINT fk_user_accounts_participant
        FOREIGN KEY (participant_id) REFERENCES participants(id);

ALTER TABLE user_accounts
    ADD CONSTRAINT uk_user_accounts_participant UNIQUE (participant_id);
