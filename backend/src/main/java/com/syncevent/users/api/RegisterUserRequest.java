package com.syncevent.users.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @Size(max = 30) String documentType,
        @Size(max = 50) String documentNumber,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 30) String phone) {

    public boolean hasGuestProfile() {
        return documentType != null || documentNumber != null || firstName != null
                || lastName != null || phone != null;
    }

    public com.syncevent.users.application.GuestProfileCommand toGuestProfileCommand() {
        return new com.syncevent.users.application.GuestProfileCommand(
                documentType, documentNumber, firstName, lastName, phone);
    }
}
