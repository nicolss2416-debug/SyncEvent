package com.syncevent.users.api;

import com.syncevent.users.application.GuestProfileCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestProfileRequest(
        @NotBlank @Size(max = 30) String documentType,
        @NotBlank @Size(max = 50) String documentNumber,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 30) String phone) {

    public GuestProfileCommand toCommand() {
        return new GuestProfileCommand(documentType, documentNumber, firstName, lastName, phone);
    }
}
