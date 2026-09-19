#!/usr/bin/env bash
# ============================================
# Parkeo — Iniciar Backend
# ============================================
set -e

PARKEO_DIR="$HOME/Documentos/Parkeo"
BACKEND_DIR="$PARKEO_DIR/backend"
MVN="/home/antwny/.sdkman/candidates/maven/current/bin/mvn"

export DB_URL="jdbc:mysql://localhost:3306/parkeo_db?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true"
export DB_USERNAME="antwny"
export DB_PASSWORD="198009"
export JWT_SECRET="parkeo-dev-secret-key-min-256-bits-0123456789abcdefghijklmnopqrstuvwxyz"
export JWT_EXPIRATION="86400000"
export JWT_REFRESH_EXPIRATION="604800000"
export SPRING_PROFILES_ACTIVE="dev"

echo "→ Iniciando Parkeo Backend..."
echo "  URL: http://localhost:8080"
echo "  Swagger: http://localhost:8080/swagger-ui.html"
echo ""

cd "$BACKEND_DIR"
$MVN spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx512m"
