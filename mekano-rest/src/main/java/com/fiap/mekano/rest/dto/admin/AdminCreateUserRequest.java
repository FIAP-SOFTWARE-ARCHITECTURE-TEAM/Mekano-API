package com.fiap.mekano.rest.dto.admin;

import com.fiap.mekano.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminCreateUserRequest(

        @NotBlank
        String name,

        @Email
        @NotBlank
        String email,

        @NotNull
        Role role
) {
}