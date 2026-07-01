-- AUTH-04 / D-46
-- Migração idempotente para inserir role 'cliente' em user_roles
-- para usuários legados SOMENTE se a tabela users ainda possuir coluna role.
--
-- Se users.role não existir, a migration não faz nada.
-- Isso evita erro em ambientes onde roles já foram migradas para user_roles.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'users'
          AND column_name = 'role'
    ) THEN
        EXECUTE '
            INSERT INTO user_roles (
                uuid,
                user_uuid,
                role,
                is_active,
                created_at
            )
            SELECT
                gen_random_uuid(),
                u.uuid,
                ''cliente'',
                true,
                CURRENT_TIMESTAMP
            FROM users u
            WHERE u.role = ''cliente''
              AND u.uuid IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM user_roles ur
                  WHERE ur.user_uuid = u.uuid
                    AND ur.role = ''cliente''
                    AND ur.is_active = true
              )
        ';
    END IF;
END $$;