-- V32: Seed default admin user
-- Creates the initial admin user with email admin@mekano.com.br
-- Password: Mekano@2024 (bcrypt hashed)
-- Role: admin

INSERT INTO users (uuid, name, email, password_hash, created_at, is_active)
SELECT gen_random_uuid(), 'Admin', 'admin@mekano.com.br',
       '$2a$10$6PbT6JSoH.Idnp7wfb4KY.4fFYeEZP8XzfxRtakClW5eyPUsVRNBu',
       NOW(), TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@mekano.com.br');

INSERT INTO user_roles (uuid, user_uuid, role, created_at, is_active)
SELECT gen_random_uuid(), u.uuid, 'admin', NOW(), TRUE
FROM users u
WHERE u.email = 'admin@mekano.com.br'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_uuid = u.uuid AND ur.role = 'admin');
