#!/bin/bash
# Backup do banco de dados Mekano (PostgreSQL)
# Uso: ./scripts/backup-db.sh [diretorio_destino]

set -euo pipefail

DEST_DIR="${1:-./backups}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${DEST_DIR}/mekano_${TIMESTAMP}.sql"
CONTAINER_NAME="mekano-postgres"
DB_NAME="mekano"
DB_USER="${POSTGRES_USER:-mekano}"

mkdir -p "$DEST_DIR"

echo "Backup iniciado: ${BACKUP_FILE}"

docker exec "$CONTAINER_NAME" \
  pg_dump -U "$DB_USER" -d "$DB_NAME" --no-owner --no-privileges \
  > "$BACKUP_FILE"

# Comprimir
gzip "$BACKUP_FILE"
echo "Backup salvo: ${BACKUP_FILE}.gz"

# Manter apenas os últimos 7 backups
BACKUP_COUNT=$(ls -1 "${DEST_DIR}"/mekano_*.sql.gz 2>/dev/null | wc -l)
if [ "$BACKUP_COUNT" -gt 7 ]; then
  ls -1t "${DEST_DIR}"/mekano_*.sql.gz | tail -n +8 | xargs rm -f
  echo "Backups antigos removidos (mantidos: 7)"
fi

echo "Backup concluído com sucesso."
