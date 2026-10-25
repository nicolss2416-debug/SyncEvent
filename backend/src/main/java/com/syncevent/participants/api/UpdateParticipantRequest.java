package com.syncevent.participants.api;

import com.syncevent.participants.application.UpdateParticipantCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateParticipantRequest(
        @NotBlank @Size(max = 30) String documentType,
        @NotBlank @Size(max = 50) String documentNumber,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 30) String phone) {

    public UpdateParticipantCommand toCommand(String updatedBy) {
        return new UpdateParticipantCommand(
                documentType,
                documentNumber,
                firstName,
                lastName,
                email,
                phone,
                updatedBy);
    }
}
