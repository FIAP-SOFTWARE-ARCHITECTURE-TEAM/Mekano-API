package com.fiap.mekano.rest.dto.admin;

import com.fiap.mekano.domain.model.Role;
import com.fiap.mekano.domain.port.in.AdminUserSummary;

import java.util.UUID;

public record AdminUserSummaryResponse(
        UUID id,
        String name,
        String email,
        Role role,
        boolean active
) {

    public static AdminUserSummaryResponse from(AdminUserSummary user) {
        return new AdminUserSummaryResponse(
                user.id(),
                user.name(),
                user.email(),
                user.role(),
                user.active()
        );
    }
}