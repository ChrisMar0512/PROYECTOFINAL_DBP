# Evaluación README e Informe + Postman Collection — ParkShare CS 2031 DBP

## Instrucciones

Eres un evaluador técnico senior del curso CS 2031 Desarrollo Basado en Plataforma de UTEC Lima.
A continuación te compartiré el contenido del archivo README.md y el archivo postman_collection.json
del proyecto ParkShare. Evalúa exactamente los dos componentes que se describen abajo
siguiendo los criterios de la rúbrica oficial. Para cada punto dime el puntaje exacto,
qué está bien, qué falta y cómo corregirlo antes del domingo 25 de mayo.

## Contexto del Proyecto

ParkShare es un marketplace de estacionamiento on-demand en Lima. Conecta
propietarios de cocheras privadas con conductores en tiempo real usando
geolocalización PostGIS, reservas con timeout de 15 minutos, check-in/check-out
por QR, billetera virtual auditada y WebSockets.
Stack: Spring Boot 3, Java 21, PostgreSQL + PostGIS en Docker.

---

## PARTE A — README e Informe (0.4 puntos de la rúbrica + evaluación del informe)

### A.1 Estructura del Informe en README.md

El informe debe estar en formato Markdown dentro del README.md en la raíz del repositorio.
Debe tener entre 1000 y 2000 palabras. Verifica que contenga exactamente estas secciones:

**Portada**
- Título descriptivo del proyecto que refleje el propósito o solución
- Nombre del curso: CS 2031 Desarrollo Basado en Plataforma
- Nombre completo de cada integrante del equipo

**Índice**
- Tabla de contenidos con todas las secciones del informe y sus enlaces

**Introducción**
- Contexto: descripción del entorno en que surge el problema (estacionamiento en Lima,
  escasez de espacios, tiempo perdido buscando cochera, etc.)
- Objetivos del Proyecto: lista de objetivos específicos y medibles

**Identificación del Problema o Necesidad**
- Descripción del Problema: explicación detallada del problema de estacionamiento
  que ParkShare busca resolver
- Justificación: por qué es relevante solucionarlo (impacto en movilidad urbana,
  oportunidad económica para propietarios de cocheras, etc.)

**Descripción de la Solución**
- Funcionalidades Implementadas: lista con descripción de cada funcionalidad
  (geolocalización, reservas, QR check-in/check-out, billetera virtual, WebSockets, etc.)
  explicando cómo cada una aporta a la solución
- Tecnologías Utilizadas: Spring Boot 3, Java 21, PostgreSQL, PostGIS, Docker,
  Spring Security, JWT, Firebase, Cloudinary, etc. con versiones donde corresponda

**Modelo de Entidades**
- Diagrama entidad-relación o diagrama de clases embebido o enlazado
- Descripción de entidades principales: User, ParkingSpace, Feature, Reservation,
  Wallet, WalletTransaction, QRCode y Review con sus atributos y relaciones

**Testing y Manejo de Errores**
- Niveles de testing realizados: unitarios (Mockito), integración (@WebMvcTest),
  repositorios (@DataJpaTest), TestContainers
- Resultados: resumen de pruebas, errores encontrados y corregidos
- Manejo de Errores: descripción de las excepciones globales con @RestControllerAdvice
  y por qué deben manejarse centralizadamente

**Medidas de Seguridad Implementadas**
- Seguridad de Datos: JWT, BCrypt, variables de entorno para secrets
- Prevención de Vulnerabilidades: CSRF deshabilitado con sesión stateless,
  validación de inputs, sanitización, CORS configurado

**Eventos y Asincronía**
- Descripción de los eventos de Spring usados (UserRegisteredEvent, ReservationCompletedEvent, etc.)
- Por qué son necesarios en ParkShare
- Por qué deben ser asincrónicos (no bloquear el hilo principal, mejorar latencia)

**GitHub & Management**
- Descripción del uso de GitHub Projects o Issues: asignación de tareas,
  labels, milestones, deadlines
- Descripción del flujo de GitHub Actions implementado para el proyecto

**Conclusión**
- Logros del Proyecto: qué se resolvió y qué valor aporta ParkShare
- Aprendizajes Clave: reflexión sobre los aprendizajes más importantes del desarrollo
- Trabajo Futuro: mejoras o extensiones posibles (app móvil, pagos reales, IA para precios, etc.)

**Apéndices**
- Licencia del proyecto (MIT, Apache 2.0, etc.)
- Referencias bibliográficas o de documentación

Para cada sección indica:
- Si está presente y completa ✅
- Si está presente pero incompleta ⚠️ con qué le falta
- Si está ausente ❌

### A.2 Criterio de Rúbrica 10.1 — README Técnico (0.4 puntos)

Además del informe narrativo, verifica que el README incluya estos elementos técnicos:

- Título descriptivo del proyecto
- Descripción clara del problema que resuelve
- Tecnologías utilizadas con versiones
- Instrucciones paso a paso para correr el proyecto localmente con Docker
- Variables de entorno requeridas documentadas (lista de todas las variables
  del application.properties que deben configurarse)
- Lista de todos los endpoints con método HTTP, ruta y descripción breve
- Diagrama entidad-relación o diagrama de clases
- Diagrama de arquitectura del sistema
- Nombres de todos los integrantes del equipo
- Link al deployment si existe

Puntaje:
- 0.4 pto: README completo con todos los elementos anteriores
- 0.3 pto: README con la mayoría de elementos, falta 1-2 secciones
- 0.2 pto: README básico con información mínima
- 0.0 pto: README ausente o muy deficiente

---

## PARTE B — Postman Collection (entregable complementario)

### B.1 Estructura y Ubicación

Verifica que:
- El archivo se llama exactamente `postman_collection.json` (o similar) y está en la raíz del repositorio
- Es un archivo JSON válido que puede importarse en Postman
- La colección tiene un nombre descriptivo relacionado al proyecto

### B.2 Cobertura de Endpoints

Verifica que la colección incluya requests para TODOS los endpoints del proyecto.
Los endpoints esperados para ParkShare son:

**Auth**
- POST /api/auth/register — registro de usuario
- POST /api/auth/login — inicio de sesión

**Parking Spaces**
- GET /api/parking-spaces — listar cocheras disponibles (con filtros de ubicación)
- POST /api/parking-spaces — crear cochera (requiere rol HOST)
- GET /api/parking-spaces/{id} — detalle de cochera
- PUT /api/parking-spaces/{id} — actualizar cochera
- DELETE /api/parking-spaces/{id} — eliminar cochera
- POST /api/parking-spaces/{id}/favorites — agregar a favoritos
- DELETE /api/parking-spaces/{id}/favorites — quitar de favoritos

**Reservations**
- POST /api/reservations — crear reserva
- GET /api/reservations — listar reservas del usuario
- GET /api/reservations/{id} — detalle de reserva
- DELETE /api/reservations/{id} — cancelar reserva

**Check-in / Check-out**
- POST /api/checkin — hacer check-in con QR
- POST /api/checkout — hacer check-out con QR

**Wallet**
- GET /api/wallet — ver saldo y datos de la billetera
- POST /api/wallet/topup — recargar saldo
- GET /api/wallet/transactions — historial de transacciones

**Reviews**
- POST /api/reviews — crear reseña
- GET /api/parking-spaces/{id}/reviews — ver reseñas de una cochera

Para cada endpoint verifica que el request tenga:
- Método HTTP correcto
- URL con variable de entorno para la base (ej: {{baseUrl}}/api/...)
- Headers necesarios (Authorization: Bearer {{token}} donde corresponda)
- Body de ejemplo con datos realistas para los POST y PUT
- Descripción explicando para qué sirve el endpoint

Reporta cada endpoint faltante o mal configurado.

### B.3 Variables de Entorno

Verifica que la colección o el environment incluya:
- `baseUrl` con el valor de la URL base (local o del deployment)
- `token` o `authToken` para el JWT que se usa en los endpoints protegidos
- Cualquier otra variable necesaria como `parkingSpaceId`, `reservationId`, etc.

Verifica que la autorización esté configurada a nivel de colección o carpeta
para no repetirla en cada request individual (Bearer Token usando {{token}}).

### B.4 Ejemplos de Funcionamiento

Verifica que los requests más importantes tengan:
- Ejemplo de request body con datos válidos y realistas
- Descripción del response esperado (status code y estructura del body)
- Al menos los endpoints de register, login, crear cochera, crear reserva,
  check-in y check-out deben tener ejemplos claros

---

## Formato de Respuesta Requerido

Para cada sección (A.1, A.2, B.1, B.2, B.3, B.4) dame:
1. Estado general: completo / parcialmente completo / ausente
2. Lista de lo que está correcto ✅
3. Lista de lo que falta o está incompleto ⚠️ con indicación exacta de qué agregar
4. Lista de lo que está ausente y es crítico ❌ con cómo añadirlo

Al final dame:
- Tabla resumen con estado por sección
- Top 5 de correcciones más urgentes para el README y la Postman Collection
  ordenadas por impacto antes del domingo 25 de mayo
- Estimación de si el README alcanza las 1000-2000 palabras requeridas
