#!/usr/bin/env bash
# ============================================
# Parkeo — Script de configuración inicial
# ============================================
set -e

PARKEO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB_USER="antwny"
DB_PASS="198009"
DB_NAME="parkeo_db"
BACKEND_DIR="$PARKEO_DIR/backend"
MVN="/home/antwny/.sdkman/candidates/maven/current/bin/mvn"

echo "╔══════════════════════════════════════╗"
echo "║         PARKEO — Setup               ║"
echo "╚══════════════════════════════════════╝"
echo ""

# ─── 1. Verificar herramientas ───────────
echo "→ Verificando herramientas..."
java -version 2>&1 | head -1
mysql --version
git --version
$MVN --version | head -1

# ─── 2. Crear base de datos ──────────────
echo ""
echo "→ Configurando base de datos..."
mysql -u $DB_USER -p$DB_PASS -e "CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null
echo "  ✓ Base de datos '$DB_NAME' verificada"

# ─── 3. Ejecutar schema ──────────────────
if [ -f "$PARKEO_DIR/database/schema.sql" ]; then
    echo "→ Aplicando schema..."
    mysql -u $DB_USER -p$DB_PASS $DB_NAME < "$PARKEO_DIR/database/schema.sql"
    echo "  ✓ Schema aplicado"
else
    echo "  ✗ schema.sql no encontrado"
fi

# ─── 4. Cargar datos iniciales ───────────
if [ -f "$PARKEO_DIR/database/data.sql" ]; then
    echo "→ Cargando datos de prueba..."
    mysql -u $DB_USER -p$DB_PASS $DB_NAME < "$PARKEO_DIR/database/data.sql"
    echo "  ✓ Datos cargados"
else
    echo "  ✗ data.sql no encontrado"
fi

# ─── 5. Compilar backend ─────────────────
if [ -f "$BACKEND_DIR/pom.xml" ]; then
    echo ""
    echo "→ Compilando backend..."
    cd $BACKEND_DIR
    $MVN clean package -DskipTests -q
    echo "  ✓ Backend compilado"
else
    echo "  ✗ pom.xml no encontrado en $BACKEND_DIR"
fi

echo ""
echo "╔══════════════════════════════════════╗"
echo "║         Setup completado ✓           ║"
echo "╚══════════════════════════════════════╝"
echo ""
echo "Para iniciar el backend:"
echo "  cd $BACKEND_DIR && $MVN spring-boot:run"
echo ""
echo "Swagger UI disponible en:"
echo "  http://localhost:8080/swagger-ui.html"
