package com.syncevent.users.application;

public record GuestProfileCommand(
        String documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String phone) {
}