-- Completa o endereço do cliente de demonstração criado pelo seed V37.
--
-- O VO Endereco exige logradouro, cidade, UF e CEP. O seed antigo deixava
-- esses campos nulos; como GET /api/v1/clientes serializa todos os clientes,
-- a listagem inteira respondia 400 ("Logradouro é obrigatório") e o gerador
-- de tráfego não conseguia localizar o cliente OBS.
UPDATE clientes
SET endereco_logradouro = 'Rua Demo',
    endereco_numero     = '100',
    endereco_bairro     = 'Centro',
    endereco_cidade     = 'São Paulo',
    endereco_uf         = 'SP',
    endereco_cep        = '01001000'
WHERE uuid = '33333333-3333-4333-8333-333333333333';
