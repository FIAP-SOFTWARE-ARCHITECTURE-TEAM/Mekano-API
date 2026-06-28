package com.fiap.mekano.infrastructure.entity;

import com.fiap.mekano.domain.model.Role;
import jakarta.persistence.*;

import java.util.UUID;


@Entity
@Table(name = "user_roles")
public class UserRoleEntity extends BaseEntity {

	@Column(name = "uuid", nullable = false, unique = true)
	public UUID uuid;
	
    @Column(name = "user_uuid", nullable = false)
    public UUID userUuid;

    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    public Role role;   
    
}
