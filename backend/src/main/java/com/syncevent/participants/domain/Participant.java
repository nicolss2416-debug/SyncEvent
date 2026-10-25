package com.syncevent.participants.domain;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

public final class Participant {

    private final UUID id;
    private final String documentType;
    private final String documentNumber;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final String createdBy;
    private final LocalDateTime createdAt;
    private final String updatedBy;
    private final LocalDateTime updatedAt;

    private Participant(
            UUID id,
            String documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            String createdBy,
            LocalDateTime createdAt,
            String updatedBy,
            LocalDateTime updatedAt) {
        this.id = id;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public static Participant register(
            String documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            String createdBy) {
        return new Participant(
                UUID.randomUUID(),
                normalize(documentType).toUpperCase(Locale.ROOT),
                normalize(documentNumber).toUpperCase(Locale.ROOT),
                normalizeName(firstName),
                normalizeName(lastName),
                normalize(email),
                normalize(phone),
                requireText(createdBy, "createdBy"),
                LocalDateTime.now(),
                null,
                null);
    }

    public static Participant reconstitute(
            UUID id,
            String documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            String createdBy,
            LocalDateTime createdAt,
            String updatedBy,
            LocalDateTime updatedAt) {
        return new Participant(
                id,
                documentType,
                documentNumber,
                firstName,
                lastName,
                email,
                phone,
                createdBy,
                createdAt,
                updatedBy,
                updatedAt);
    }

    public Participant update(
            String documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            String updatedBy) {
        return new Participant(
                id,
                normalize(documentType).toUpperCase(Locale.ROOT),
                normalize(documentNumber).toUpperCase(Locale.ROOT),
                normalizeName(firstName),
                normalizeName(lastName),
                normalize(email),
                normalize(phone),
                createdBy,
                createdAt,
                requireText(updatedBy, "updatedBy"),
                LocalDateTime.now());
    }

    private static String normalizeName(String value) {
        String normalized = normalize(value).toLowerCase(Locale.forLanguageTag("es"));
        StringBuilder result = new StringBuilder(normalized.length());
        boolean capitalizeNext = true;
        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            if (Character.isWhitespace(character)) {
                result.append(' ');
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext
                        ? Character.toUpperCase(character)
                        : character);
                capitalizeNext = character == '-';
            }
        }
        return result.toString();
    }

    private static String normalize(String value) {
        return requireText(value, "value").trim().replaceAll("\\s+", " ");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
