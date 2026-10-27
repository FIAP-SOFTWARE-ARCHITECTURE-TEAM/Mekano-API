-- Corrige o CPF do cliente de demonstração criado pelo seed V37.
--
-- O CPF 11122233344 não passa na validação de CPF da API. Como o endpoint
-- GET /api/v1/clientes valida todos os registros retornados, o cliente
-- inválido fazia a listagem inteira responder 400 e impedia o gerador de
-- tráfego de demonstração de encontrar o cliente OBS.
--
-- O CPF abaixo é válido e diferente do CPF usado pelo cliente OBS
-- (11144477735) para não colidir com o seed de observabilidade.
UPDATE clientes
SET cpf = '52998224725'
WHERE uuid = '33333333-3333-4333-8333-333333333333'
  AND cpf = '11122233344';
