-- View de autenticação consumida pelo Mekano-Serverless.
-- Mantém o contrato desacoplado das tabelas internas do Mekano-API.

CREATE OR REPLACE VIEW auth_customer_view AS
SELECT
    c.uuid                                        AS customer_id,
    regexp_replace(c.cpf, '\D', '', 'g')         AS cpf,
    c.nome                                        AS display_name,
    CASE
        WHEN c.is_active = TRUE
         AND c.deleted_at IS NULL
        THEN 'ACTIVE'
        ELSE 'INACTIVE'
    END                                           AS status,
    ARRAY['cliente']::TEXT[]                      AS roles
FROM clientes c;
