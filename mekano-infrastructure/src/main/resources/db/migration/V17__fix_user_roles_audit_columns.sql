ALTER TABLE user_roles
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE user_roles
    ADD COLUMN IF NOT EXISTS updated_by UUID;

ALTER TABLE user_roles
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

ALTER TABLE user_roles
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE user_roles
    ALTER COLUMN created_by DROP NOT NULL;

ALTER TABLE user_roles
    ADD COLUMN IF NOT EXISTS uuid UUID;

UPDATE user_roles
SET uuid = gen_random_uuid()
WHERE uuid IS NULL;

ALTER TABLE user_roles
    ALTER COLUMN uuid SET NOT NULL;