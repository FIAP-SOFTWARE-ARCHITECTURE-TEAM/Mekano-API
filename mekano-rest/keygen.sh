#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

PRIVATE_DIR="${HOME}/.mekano/secrets"
PRIVATE_KEY="${PRIVATE_DIR}/privatekey.pem"

PUBLIC_KEY="${PROJECT_ROOT}/src/main/resources/publicKey.pem"

mkdir -p "${PRIVATE_DIR}"
mkdir -p "${PROJECT_ROOT}/src/main/resources"

echo "Gerando chave privada Ed25519 em:"
echo "${PRIVATE_KEY}"

openssl genpkey \
  -algorithm Ed25519 \
  -out "${PRIVATE_KEY}"

echo "Gerando chave pública Ed25519 em:"
echo "${PUBLIC_KEY}"

openssl pkey \
  -in "${PRIVATE_KEY}" \
  -pubout \
  -out "${PUBLIC_KEY}"

echo
echo "Validando chave pública..."
openssl pkey \
  -pubin \
  -in "${PUBLIC_KEY}" \
  -text \
  -noout

echo
echo "Chaves geradas com sucesso."
echo "Private key: ${PRIVATE_KEY}"
echo "Public key : ${PUBLIC_KEY}"