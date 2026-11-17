package com.syncevent.participants.api;

import java.util.List;

import com.syncevent.participants.application.RegisterParticipantCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BulkParticipantRequest(
        @NotEmpty @Size(max = 500) List<@NotNull @Valid BulkParticipantItem> items) {

    public record BulkParticipantItem(
            @jakarta.validation.constraints.NotBlank @Size(max = 30) String documentType,
            @jakarta.validation.constraints.NotBlank @Size(max = 50) String documentNumber,
            @jakarta.validation.constraints.NotBlank @Size(max = 100) String firstName,
            @jakarta.validation.constraints.NotBlank @Size(max = 100) String lastName,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email @Size(max = 254) String email,
            @Size(max = 30) String phone) {

        public RegisterParticipantCommand toCommand(String createdBy) {
            return new RegisterParticipantCommand(
                    documentType,
                    documentNumber,
                    firstName,
                    lastName,
                    email,
                    phone == null ? "" : phone,
                    createdBy);
        }
    }
}
