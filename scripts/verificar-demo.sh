#!/usr/bin/env bash
# Verificación funcional de la demo FinanTech.
# Uso:
#   bash scripts/verificar-demo.sh
#   bash scripts/verificar-demo.sh --con-pruebas --seguridad
#
# Requiere: curl, python3 y los servicios Java (8080), WCF (8081) y MailHog (8025).

set -uo pipefail

BASE_JAVA="${BASE_JAVA:-http://localhost:8080}"
BASE_WCF="${BASE_WCF:-http://localhost:8081/TransaccionService.svc}"
MAILHOG="${MAILHOG:-http://localhost:8025}"
DEMO_USER="${DEMO_USER:-cliente}"
DEMO_PASSWORD="${DEMO_PASSWORD:-Clave123!}"

CON_PRUEBAS=0
SEGURIDAD=0
for arg in "$@"; do
  case "$arg" in
    --con-pruebas) CON_PRUEBAS=1 ;;
    --seguridad) SEGURIDAD=1 ;;
    -h|--help)
      sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) echo "Opción desconocida: $arg" >&2; exit 2 ;;
  esac
done

PASS=0
FAIL=0
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

ok()   { printf '  [OK]    %s\n' "$1"; PASS=$((PASS + 1)); }
ko()   { printf '  [FALLA] %s\n' "$1"; FAIL=$((FAIL + 1)); }
info() { printf '\n== %s ==\n' "$1"; }

json_get() {
  # json_get <archivo> <clave>
  python3 - "$1" "$2" <<'PY'
import json, sys
try:
    with open(sys.argv[1], encoding="utf-8") as f:
        data = json.load(f)
    print(data.get(sys.argv[2], ""))
except Exception:
    print("")
PY
}

http_code() {
  curl -s -o "$2" -w "%{http_code}" --max-time 15 "$1" 2>/dev/null || echo "000"
}

for cmd in curl python3; do
  command -v "$cmd" >/dev/null 2>&1 || { echo "Falta el comando: $cmd" >&2; exit 2; }
done

info "1. Servicios disponibles"
code=$(http_code "$BASE_JAVA/api/v1/transacciones" "$TMP/java.json")
if [ "$code" = "401" ] || [ "$code" = "403" ]; then
  ok "Java responde en $BASE_JAVA (HTTP $code sin token, como se espera)"
else
  ko "Java no responde correctamente en $BASE_JAVA (HTTP $code)"
fi

code=$(http_code "$BASE_WCF?wsdl" "$TMP/wsdl.xml")
if [ "$code" = "200" ] && grep -q "urn:finantech:transacciones:v1" "$TMP/wsdl.xml"; then
  ok "WCF publica el WSDL con namespace versionado"
else
  ko "WCF no publica el WSDL esperado (HTTP $code)"
fi

code=$(http_code "$MAILHOG/api/v2/messages" "$TMP/mailhog.json")
if [ "$code" = "200" ]; then
  ok "MailHog responde en $MAILHOG"
else
  ko "MailHog no responde (HTTP $code)"
fi

info "2. Autenticación JWT"
code=$(curl -s -o "$TMP/login.json" -w "%{http_code}" --max-time 15 \
  -X POST "$BASE_JAVA/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$DEMO_USER\",\"password\":\"$DEMO_PASSWORD\"}")
TOKEN="$(json_get "$TMP/login.json" accessToken)"
if [ "$code" = "200" ] && [ -n "$TOKEN" ]; then
  ok "Login correcto y token JWT recibido"
else
  ko "No se pudo obtener el token (HTTP $code)"
  echo "No se puede continuar sin token." >&2
  exit 1
fi

code=$(curl -s -o "$TMP/login-malo.json" -w "%{http_code}" --max-time 15 \
  -X POST "$BASE_JAVA/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"cliente","password":"incorrecta"}')
if [ "$code" = "401" ]; then
  ok "Credenciales inválidas rechazadas con 401"
else
  ko "Las credenciales inválidas no devolvieron 401 (HTTP $code)"
fi

info "3. Transacciones REST"
ID_KEY="VERIF-$(date +%s)-$RANDOM"
BODY="$(python3 - "$ID_KEY" <<'PY'
import json, sys
print(json.dumps({
    "idTransaccion": sys.argv[1],
    "cuentaOrigen": "001-1234567890",
    "cuentaDestino": "002-0987654321",
    "monto": 1500.00,
    "moneda": "PEN",
    "descripcion": "verificacion automatica",
}))
PY
)"

code=$(curl -s -o "$TMP/post.json" -w "%{http_code}" --max-time 15 \
  -X POST "$BASE_JAVA/api/v1/transacciones" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $ID_KEY" \
  -d "$BODY")
TX_ID="$(json_get "$TMP/post.json" idTransaccion)"
ESTADO="$(json_get "$TMP/post.json" estado)"
if [ "$code" = "201" ] && [ -n "$TX_ID" ] && [ "$ESTADO" = "APROBADA" ]; then
  ok "POST /transacciones -> 201 ($TX_ID, $ESTADO)"
else
  ko "POST /transacciones falló (HTTP $code, estado '$ESTADO')"
fi

if [ -n "$TX_ID" ]; then
  code=$(curl -s -o "$TMP/get.json" -w "%{http_code}" --max-time 15 \
    "$BASE_JAVA/api/v1/transacciones/$TX_ID" \
    -H "Authorization: Bearer $TOKEN")
  if [ "$code" = "200" ] && grep -q "$TX_ID" "$TMP/get.json"; then
    ok "GET /transacciones/$TX_ID -> 200"
  else
    ko "GET de la transacción falló (HTTP $code)"
  fi
fi

code=$(curl -s -o "$TMP/post-idem.json" -w "%{http_code}" --max-time 15 \
  -X POST "$BASE_JAVA/api/v1/transacciones" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $ID_KEY" \
  -d "$BODY")
TX_ID2="$(json_get "$TMP/post-idem.json" idTransaccion)"
if [ "$code" = "201" ] && [ "$TX_ID" = "$TX_ID2" ]; then
  ok "Idempotencia: la misma clave devuelve el mismo idTransaccion"
else
  ko "La idempotencia no devolvió el mismo idTransaccion"
fi

code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 15 \
  "$BASE_JAVA/api/v1/transacciones/$TX_ID")
if [ "$code" = "401" ]; then
  ok "GET sin token rechazado con 401"
else
  ko "GET sin token no devolvió 401 (HTTP $code)"
fi

info "4. WCF / SOAP"
cat > "$TMP/soap-ok.xml" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
  <s:Body>
    <ProcesarTransaccion xmlns="urn:finantech:transacciones:v1">
      <request>
        <cuentaDestino>002-0987654321</cuentaDestino>
        <cuentaOrigen>001-1234567890</cuentaOrigen>
        <descripcion>verificacion SOAP</descripcion>
        <idTransaccion>TX-VERIF-SOAP</idTransaccion>
        <moneda>PEN</moneda>
        <monto>250.50</monto>
      </request>
    </ProcesarTransaccion>
  </s:Body>
</s:Envelope>
XML

code=$(curl -s -o "$TMP/soap-resp.xml" -w "%{http_code}" --max-time 15 \
  -X POST "$BASE_WCF" \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H 'SOAPAction: "urn:finantech:transacciones:v1/ITransaccionesService/ProcesarTransaccion"' \
  --data-binary @"$TMP/soap-ok.xml")
if [ "$code" = "200" ] && grep -q "ProcesarTransaccionResult" "$TMP/soap-resp.xml" && grep -q "APROBADA" "$TMP/soap-resp.xml"; then
  ok "SOAP ProcesarTransaccion -> 200 y estado APROBADA"
else
  ko "La operación SOAP correcta falló (HTTP $code)"
fi

sed 's/TX-VERIF-SOAP/TX-VERIF-FAULT/; s#<monto>250.50</monto>#<monto>0</monto>#' \
  "$TMP/soap-ok.xml" > "$TMP/soap-fault.xml"
code=$(curl -s -o "$TMP/soap-fault-resp.xml" -w "%{http_code}" --max-time 15 \
  -X POST "$BASE_WCF" \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H 'SOAPAction: "urn:finantech:transacciones:v1/ITransaccionesService/ProcesarTransaccion"' \
  --data-binary @"$TMP/soap-fault.xml")
if [ "$code" = "500" ] && grep -q "ErrorFinanciero" "$TMP/soap-fault-resp.xml" && grep -q "VAL-001" "$TMP/soap-fault-resp.xml"; then
  ok "SOAP Fault tipado -> ErrorFinanciero / VAL-001"
else
  ko "El SOAP Fault tipado no devolvió ErrorFinanciero/VAL-001 (HTTP $code)"
fi

info "5. Correo asíncrono"
curl -s -o /dev/null --max-time 15 \
  -X POST "$BASE_JAVA/api/v1/transacciones" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: ALERTA-$ID_KEY" \
  -d "{\"idTransaccion\":\"TX-VERIF-ALERTA\",\"cuentaOrigen\":\"001-1234567890\",\"cuentaDestino\":\"002-0987654321\",\"monto\":0,\"moneda\":\"PEN\",\"descripcion\":\"alerta de verificacion\"}"
sleep 2
code=$(curl -s -o "$TMP/mailhog2.json" -w "%{http_code}" --max-time 15 \
  "$MAILHOG/api/v2/messages?limit=50")
if [ "$code" = "200" ] && grep -q "Transaccion confirmada" "$TMP/mailhog2.json"; then
  ok "Correo de confirmación presente en MailHog"
else
  ko "No se encontró el correo de confirmación en MailHog"
fi
if [ "$code" = "200" ] && grep -q "ALERTA FINANTECH" "$TMP/mailhog2.json"; then
  ok "Correo de alerta presente en MailHog"
else
  ko "No se encontró el correo de alerta en MailHog"
fi

if [ "$CON_PRUEBAS" = "1" ]; then
  info "6. Pruebas y compilación"

  MVN_CMD="${MVN_BIN:-}"
  if [ -z "$MVN_CMD" ] && command -v mvn >/dev/null 2>&1; then
    MVN_CMD="$(command -v mvn)"
  fi
  if [ -z "$MVN_CMD" ] && [ -n "${MAVEN_HOME:-}" ] && [ -x "$MAVEN_HOME/bin/mvn" ]; then
    MVN_CMD="$MAVEN_HOME/bin/mvn"
  fi

  if [ -n "$MVN_CMD" ]; then
    if (cd "$(dirname "$0")/.." && "$MVN_CMD" -B -q -f java-service/pom.xml test > "$TMP/mvn.log" 2>&1); then
      ok "mvn test (4 pruebas) superado"
    else
      ko "mvn test falló; revisa $TMP/mvn.log"
      tail -20 "$TMP/mvn.log"
    fi
  else
    echo "  [OMITIDO] mvn no está en PATH (define MVN_BIN=/ruta/al/mvn)"
  fi

  DOTNET_CMD="${DOTNET_BIN:-}"
  if [ -z "$DOTNET_CMD" ] && command -v dotnet >/dev/null 2>&1; then
    DOTNET_CMD="$(command -v dotnet)"
  fi
  if [ -z "$DOTNET_CMD" ] && [ -n "${DOTNET_ROOT:-}" ] && [ -x "$DOTNET_ROOT/dotnet" ]; then
    DOTNET_CMD="$DOTNET_ROOT/dotnet"
  fi

  if [ -n "$DOTNET_CMD" ]; then
    if (cd "$(dirname "$0")/.." && "$DOTNET_CMD" build dotnet/FinanTech.Client/FinanTech.Client.csproj -v q --nologo > "$TMP/dotnet.log" 2>&1); then
      ok "dotnet build del cliente y WCF superado"
    else
      ko "dotnet build falló; revisa $TMP/dotnet.log"
    fi
  else
    echo "  [OMITIDO] dotnet no está en PATH (define DOTNET_BIN=/ruta/al/dotnet)"
  fi
fi

if [ "$SEGURIDAD" = "1" ]; then
  info "7. Escaneo de dependencias"
  DOTNET_SEC="${DOTNET_BIN:-}"
  if [ -z "$DOTNET_SEC" ] && command -v dotnet >/dev/null 2>&1; then
    DOTNET_SEC="$(command -v dotnet)"
  fi
  if [ -z "$DOTNET_SEC" ] && [ -n "${DOTNET_ROOT:-}" ] && [ -x "$DOTNET_ROOT/dotnet" ]; then
    DOTNET_SEC="$DOTNET_ROOT/dotnet"
  fi

  if [ -n "$DOTNET_SEC" ]; then
    salida="$(cd "$(dirname "$0")/.." && "$DOTNET_SEC" list dotnet/FinanTech.WcfService.CoreWcf/FinanTech.WcfService.CoreWcf.csproj package --vulnerable --include-transitive 2>&1)"
    if echo "$salida" | grep -qi "no tiene paquetes vulnerables"; then
      ok "NuGet: sin paquetes vulnerables en WCF"
    else
      ko "NuGet: revisar posibles paquetes vulnerables en WCF"
    fi
    salida="$(cd "$(dirname "$0")/.." && "$DOTNET_SEC" list dotnet/FinanTech.Client/FinanTech.Client.csproj package --vulnerable --include-transitive 2>&1)"
    if echo "$salida" | grep -qi "no tiene paquetes vulnerables"; then
      ok "NuGet: sin paquetes vulnerables en el cliente"
    else
      ko "NuGet: revisar posibles paquetes vulnerables en el cliente"
    fi
  else
    echo "  [OMITIDO] dotnet no está en PATH (define DOTNET_BIN=/ruta/al/dotnet)"
  fi
  echo "  [INFO] Java: revisa la sección Seguridad del README (2 avisos de Spring no aplicables)."
fi

info "Resumen"
printf '  Comprobaciones OK: %d\n' "$PASS"
printf '  Comprobaciones fallidas: %d\n' "$FAIL"
if [ "$FAIL" -gt 0 ]; then
  echo "  RESULTADO: REVISAR"
  exit 1
fi
echo "  RESULTADO: SISTEMA OK"
