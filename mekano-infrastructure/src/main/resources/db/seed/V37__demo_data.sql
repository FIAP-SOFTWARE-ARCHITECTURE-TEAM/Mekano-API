-- V37__demo_data.sql — dados de demonstração
-- Executado APENAS nos ambientes com QUARKUS_FLYWAY_LOCATIONS incluindo db/seed
-- (produção/EKS). Os testes usam apenas db/migration e não são afetados.
-- Idempotente por construção (ON CONFLICT DO NOTHING).

INSERT INTO users (uuid, name, email, password_hash, created_at) VALUES
    ('11111111-1111-4111-8111-111111111111', 'Admin Demo', 'admin@mekano.com',
     '$2y$10$B6bZnPXc2QaSMRj2KsdMGekGdZVfMMcRzKr1yjhMbTlTY7qxGoPRq', now()),
    ('22222222-2222-4222-8222-222222222222', 'Mecanico Demo', 'mecanico@mekano.com',
     '$2y$10$B6bZnPXc2QaSMRj2KsdMGekGdZVfMMcRzKr1yjhMbTlTY7qxGoPRq', now())
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (uuid, user_uuid, role) VALUES
    ('aaaa1111-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', 'admin'),
    ('aaaa2222-0000-4000-8000-000000000002', '22222222-2222-4222-8222-222222222222', 'mecanico')
ON CONFLICT (user_uuid, role) DO NOTHING;

INSERT INTO clientes (
    uuid, nome, cpf, email, telefone,
    endereco_logradouro, endereco_numero, endereco_bairro,
    endereco_cidade, endereco_uf, endereco_cep, created_at
) VALUES
    ('33333333-3333-4333-8333-333333333333', 'Cliente Demo', '52998224725',
     'cliente.demo@mekano.com', '11999990000',
     'Rua Demo', '100', 'Centro', 'São Paulo', 'SP', '01001000', now())
ON CONFLICT (cpf) DO NOTHING;

INSERT INTO veiculos (uuid, cliente_uuid, placa, marca, modelo, ano, created_at) VALUES
    ('44444444-4444-4444-8444-444444444444', '33333333-3333-4333-8333-333333333333',
     'ABC1D23', 'Volkswagen', 'Gol', 2020, now())
ON CONFLICT (placa) DO NOTHING;

INSERT INTO pecas (uuid, codigo, descricao, valor_unitario, saldo, estoque_minimo, created_at) VALUES
    ('55555555-5555-4555-8555-555555555555', 'PNEU-16', 'Pneu 16 polegadas', 350.00, 10, 2, now()),
    ('66666666-6666-4666-8666-666666666666', 'FILTRO-AR', 'Filtro de ar esportivo', 80.00, 5, 2, now())
ON CONFLICT DO NOTHING;

INSERT INTO servicos (uuid, nome, descricao, valor, created_at) VALUES
    ('77777777-7777-4777-8777-777777777777', 'TROCA_OLEO', 'Troca de oleo e filtro', 150.00, now())
ON CONFLICT DO NOTHING;
