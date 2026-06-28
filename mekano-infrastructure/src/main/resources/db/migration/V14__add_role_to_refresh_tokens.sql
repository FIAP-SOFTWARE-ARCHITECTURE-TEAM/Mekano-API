
ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS role VARCHAR(50);

UPDATE refresh_tokens
SET role = 'cliente'
WHERE role IS NULL;

ALTER TABLE refresh_tokens
    ALTER COLUMN role SET NOT NULL;