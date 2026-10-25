# AGENTS.md - Proyecto FinanTech S.A.

> Contexto para cualquier agente de IA o desarrollador que trabaje en este repositorio.
> Este archivo se generó a partir del enunciado del caso práctico (curso Servicios Web; copia local no distribuida en el repositorio).

## 1. Contexto del proyecto

Trabajo final del curso **Servicios Web (PDSD-643)**.

Empresa ficticia: **FinanTech S.A.**, servicios financieros.

Situación actual:

- Core en **Java** (70 % de la lógica de negocio), servidores **Linux**.
- Componentes migrados a **.NET** (30 %), servidores Windows.
- 85 % de los usuarios usan clientes **Windows**.
- 50 000 operaciones diarias; 1 000 alertas diarias por transacciones fallidas.
- Latencia actual de hasta **500 ms** por solicitud.
- Migración gradual .NET-Java con problemas de interoperabilidad.

Objetivo del trabajo:

- Diseñar una solución de interoperabilidad .NET-Java.
- Reducir la latencia a **menos de 200 ms** por transacción.
- Permitir migración progresiva sin interrumpir operaciones.
- Automatizar notificaciones por correo.
- Usar **WCF** en .NET, **JSON** para intercambio, **JWT** y **SSL/TLS**.
- Soportar un incremento del **40 %** en 6 meses.

## 2. Entregables

1. **Documento de propuesta** (formato oficial `.docx`, exportable a PDF/Word; copia local no versionada).
2. **Demo funcional** (servicio Java + servicio WCF + cliente .NET + notificaciones).
3. **Diagramas** de arquitectura, secuencia, migración y despliegue.
4. **Evidencias** de pruebas (resultados de latencia, logs, WSDL, JSON/XML).
5. **Defensa** del proyecto (documento local no versionado).

## 3. Alcance decidido

- Se entrega **propuesta + demo funcional**.
- Plazo: **4 días**.
- En el PC del instituto solo se documenta y planifica; el desarrollo se hará en el PC personal.
- El PC del instituto tiene .NET SDK y Java 8 (sin JDK). **No se puede compilar Java aquí**.
- La demo se ejecutará en localhost. La latencia real de red se documenta como SLO y se mide con JMeter; no se deben inventar resultados.

## 4. Stack tecnológico

| Capa | Tecnología | Versión / nota |
|---|---|---|
| Servicio core | Java + Spring Boot | JDK 17, Spring Boot 3.5.x, Maven |
| Seguridad Java | Spring Security + jjwt | JWT HS256 en demo; RS256 recomendado en producción |
| Base de datos demo | H2 en memoria | PostgreSQL 16 opcional con Docker |
| Servicio .NET | WCF | Ruta A: .NET Framework 4.8 + Visual Studio. Ruta B: CoreWCF sobre .NET 8 |
| Cliente .NET | Consola .NET 8 | `HttpClient` + `System.ServiceModel.Http` |
| Intercambio | JSON | camelCase, UTF-8, ISO-8601 UTC |
| Correo | MailHog (demo) / SMTP real | JavaMailSender, asíncrono |
| Pruebas | Postman, SoapUI, JMeter, WCF Test Client | Evidencias en `docs/evidencias/` |
| Diagramas | Graphviz / Mermaid | Exportar a PNG para el documento |

## 5. Estructura del repositorio publicado

```text
PROYECTO_FINAL/
├─ AGENTS.md
├─ README.md
├─ .github/
│  └─ dependabot.yml
├─ docs/
│  ├─ ejemplos/              JSON de ejemplo
│  └─ evidencias/            JSON, XML, CSV, PNG y fuentes Graphviz
├─ java-service/             Spring Boot 3.5 + JDK 17
├─ dotnet/
│  ├─ FinanTech.WcfService.CoreWcf/ (ruta B, .NET 8)
│  └─ FinanTech.Client/
├─ scripts/
│  ├─ verificar-demo.sh
│  └─ prueba-latencia.ps1
└─ docker-compose.yml        (MailHog + PostgreSQL opcional)
```

> La documentación extensa (plan, arquitectura, guía, documento final y defensa) se conserva en el equipo, pero **no se publica** en GitHub.

## 6. Decisiones técnicas obligatorias

- **WCF interoperable:** usar `basicHttpBinding` (SOAP 1.1) para clientes Java. No usar `wsHttpBinding` ni `netTcpBinding` para interoperabilidad.
- **JSON:** DTO plano para transacciones; metadatos en cabeceras HTTP (`Authorization`, `Idempotency-Key`, `X-Correlation-Id`). Versión en la URL (`/api/v1`).
- **Contratos WCF:** `ServiceContract` y `DataContract` con namespace explícito `urn:finantech:transacciones:v1`; solo tipos interoperables; errores con `FaultContract`, no excepciones .NET.
- **Seguridad:** TLS 1.2/1.3 + JWT. En demo se puede usar HTTP local, pero el documento debe explicar TLS y la configuración de producción.
- **Latencia:** medir p50/p95/p99; optimizar conexión, serialización, base de datos y correo asíncrono. No afirmar <200 ms sin evidencia.
- **Escalabilidad:** servicios sin estado, balanceo, caché, colas, circuit breaker y autoescalado. La carga real (50 000/día) es baja; el foco es latencia, disponibilidad y migración.
- **Migración:** patrón Strangler Fig, API Gateway, anti-corruption layer, feature flags y canary releases.

## 7. Convenciones

- Documentación en **español**; código con nombres de dominio en español (`Transaccion`, `monto`, `cuentaOrigen`).
- Clases C# y Java en `PascalCase`; métodos Java en `camelCase`; tablas/columnas en `snake_case`.
- JSON en `camelCase`, UTF-8, fechas ISO-8601 UTC (`2026-10-22T15:30:00Z`).
- Montos con `BigDecimal` en Java y `decimal` en .NET; en producción considerar string o unidades mínimas (céntimos).
- IDs de transacción como string (`TX-2026-000001` o UUID).
- No usar `DataSet`, `Dictionary`, `object`, tipos anónimos ni polimorfismo en contratos WCF.
- No registrar tokens, contraseñas, números de cuenta completos ni datos personales en logs.
- Secretos por variables de entorno; nunca en el repositorio.

## 8. Comandos previstos

```powershell
# Java
mvn -f java-service/pom.xml spring-boot:run
mvn -f java-service/pom.xml test

# WCF ruta A (.NET Framework): abrir la solución en Visual Studio y ejecutar con IIS Express
# WCF ruta B (CoreWCF)
dotnet run --project dotnet/FinanTech.WcfService.CoreWcf

# Cliente .NET
dotnet run --project dotnet/FinanTech.Client

# MailHog
docker run -d --name mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog

# Verificación funcional completa
bash scripts/verificar-demo.sh --con-pruebas --seguridad

# Prueba de latencia
powershell -ExecutionPolicy Bypass -File scripts/prueba-latencia.ps1 -Iterations 200 -BaseUrl http://localhost:8080
```

## 9. Flujo de trabajo

1. Revisar la documentación local (plan, arquitectura y guía) antes de cambiar el código.
2. Implementar primero el servicio Java y validarlo con Postman/curl.
3. Implementar el servicio WCF y validar el WSDL con SoapUI/WCF Test Client.
4. Implementar el cliente .NET y medir latencia.
5. Integrar notificaciones asíncronas y evidencias.
6. Completar el documento final y la defensa.
7. Si el tiempo aprieta, aplicar la **ruta mínima viable** del plan local.
8. Antes de cerrar, ejecutar `bash scripts/verificar-demo.sh --con-pruebas --seguridad`.

## 10. Reglas de seguridad

- No commitear `.env`, claves JWT, certificados `.pfx`, contraseñas ni cadenas de conexión reales.
- Usar `JWT_SECRET`, `MAIL_PASSWORD` y `DB_PASSWORD` como variables de entorno.
- En producción: JWT RS256 con JWKS, rotación de claves, TLS 1.3, mTLS interno, rate limiting y auditoría.
- La demo puede usar HTTP y HS256, pero debe quedar documentada la diferencia con producción.

## 11. Definition of Done

- [ ] El servicio Java autentica con JWT y expone POST/GET de transacciones.
- [ ] El servicio WCF publica WSDL y responde una operación SOAP.
- [ ] El cliente .NET consume Java (JSON) y WCF (SOAP) correctamente.
- [ ] Una transacción fallida genera un correo visible en MailHog.
- [ ] Existe una prueba de latencia con p50/p95/p99 y capturas.
- [ ] El documento final responde las 5 preguntas guía y tiene diagramas.
- [ ] El ZIP de entrega incluye código, documento PDF y evidencias.
- [ ] La demo se puede explicar en 5 minutos sin leer el código.

## 12. Advertencias técnicas

- **WCF clásico no corre en .NET Core/.NET 8.** Si no hay .NET Framework 4.8, usar **CoreWCF** (ruta B). No mezclar ambas rutas en la misma demo.
- **Java 8 runtime no compila.** Instalar **JDK 17** y configurar `JAVA_HOME`.
- **Menos de 200 ms no se garantiza solo con código.** Es un SLO que depende de red, infraestructura y medición. Documentar el baseline y las optimizaciones.
- **No inventar resultados de pruebas.** Usar placeholders `[COMPLETAR]` hasta tener la evidencia.
- **El PDF pide una propuesta**, no un sistema de producción. La demo es un complemento para demostrar que la propuesta es viable.

## 13. Documentación local (no distribuida)

- `docs/01_PLAN_4_DIAS.md`: cronograma y checklist.
- `docs/02_ARQUITECTURA.md`: diseño técnico, diagramas y respuestas a las preguntas guía.
- `docs/03_GUIA_IMPLEMENTACION.md`: paso a paso con código.
- `docs/04_DOCUMENTO_FINAL.md`: borrador del entregable formal.
- `docs/05_DEFENSA.md`: guion de exposición y preguntas probables.
- `docs/06_GUIA_LLENADO_FORMATO.md`: guía para completar el formato oficial.
- `PDSD-643_TRABAJOFINAL.md` / `.pdf`: enunciado original.

> Estos archivos se conservan en el equipo, pero están excluidos del repositorio público. Los únicos Markdown publicados son `README.md` y `AGENTS.md`.
