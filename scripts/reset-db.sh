#!/usr/bin/env bash
# ============================================
# Parkeo — Reiniciar base de datos
# ============================================
set -e

PARKEO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB_USER="antwny"
DB_PASS="198009"
DB_NAME="parkeo_db"

echo "⚠️  Esto eliminará y recreará toda la base de datos '$DB_NAME'"
read -p "¿Confirmar? (escribe 'SI' para continuar): " CONFIRM

if [ "$CONFIRM" != "SI" ]; then
    echo "Operación cancelada."
    exit 0
fi

echo "→ Eliminando base de datos..."
mysql -u $DB_USER -p$DB_PASS -e "DROP DATABASE IF EXISTS $DB_NAME;"
echo "→ Creando base de datos..."
mysql -u $DB_USER -p$DB_PASS -e "CREATE DATABASE $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
echo "→ Aplicando schema..."
mysql -u $DB_USER -p$DB_PASS $DB_NAME < "$PARKEO_DIR/database/schema.sql"
echo "→ Cargando datos de prueba..."
mysql -u $DB_USER -p$DB_PASS $DB_NAME < "$PARKEO_DIR/database/data.sql"
echo ""
echo "✓ Base de datos reiniciada correctamente"
mysql -u $DB_USER -p$DB_PASS $DB_NAME -e "SHOW TABLES;"
