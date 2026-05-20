# Evaluación ParkShare - Rúbrica CS 2031 DBP

## Instrucciones

Eres un evaluador técnico senior del curso CS 2031 Desarrollo Basado en
Plataforma de UTEC Lima. Te voy a compartir el código completo de mi proyecto
ParkShare. Analiza cada criterio de la rúbrica oficial, dime el puntaje exacto
que obtendría en cada sección, qué está bien, qué falta y cómo corregirlo.
Al final dame el puntaje total estimado sobre 20 puntos y una lista priorizada
de correcciones urgentes antes del domingo 25 de mayo.

## Contexto del Proyecto

ParkShare es un marketplace de estacionamiento on-demand en Lima. Conecta
propietarios de cocheras privadas con conductores en tiempo real usando
geolocalización PostGIS, reservas con timeout de 15 minutos, check-in/check-out
por QR, billetera virtual auditada y WebSockets.
Stack: Spring Boot 3, Java 21, PostgreSQL + PostGIS en Docker.

---

## CRITERIO 1 — Entidades y Modelo de Datos (3 puntos)

### 1.1 Diseño de Entidades (1.5 puntos)

Verifica que existan más de 6 entidades correctamente definidas. Las esperadas
son: User, ParkingSpace, Feature, Reservation, Wallet, WalletTransaction,
QRCode y Review. Para cada entidad verifica:

- Tiene @Entity y @Table correctamente anotadas
- Cada campo tiene @Column con tipo de dato adecuado
- Campos obligatorios tienen @NotNull o equivalente
- Campos monetarios tienen precision y scale definidos en @Column
- Enums tienen @Enumerated(EnumType.STRING)
- Timestamps usan @CreationTimestamp o @UpdateTimestamp
- No hay atributos redundantes entre entidades
- La lógica de negocio se refleja completamente en el modelo

Puntaje: más de 6 entidades correctas = 1.5, cinco o seis = 1.2,
cuatro = 0.9, tres = 0.6, dos = 0.3, menos de dos = 0.0.

### 1.2 Relaciones entre Entidades (1.0 punto)

Verifica que existan y estén correctamente configuradas TODAS estas relaciones:

- @OneToOne entre Wallet y User
- @OneToOne entre QRCode y Reservation
- @OneToOne entre Review y Reservation
- @ManyToOne de ParkingSpace hacia User como host
- @ManyToOne de Reservation hacia User como driver
- @ManyToOne de Reservation hacia ParkingSpace
- @ManyToOne de WalletTransaction hacia Wallet
- @ManyToOne de WalletTransaction hacia Reservation nullable
- @ManyToOne de Review hacia User como reviewer
- @ManyToOne de Review hacia User como reviewee
- @ManyToMany entre ParkingSpace y Feature con tabla parking_space_features
- @ManyToMany entre User y ParkingSpace para favoritos con tabla
  user_favorite_spaces

Para cada relación verifica:
- Cascade type apropiado según el caso de uso
- Fetch type optimizado: LAZY donde no se necesite carga inmediata,
  EAGER solo donde sea estrictamente necesario
- Las relaciones bidireccionales tienen mappedBy correcto

Reporta cada relación mal configurada o con fetch EAGER innecesario.

Puntaje: todas correctas con cascade y fetch optimizados = 1.0,
mayoría correctas = 0.8, básicas implementadas = 0.6,
múltiples errores = 0.2, sin relaciones = 0.0.

### 1.3 Constraints y Validaciones (0.5 puntos)

Verifica que existan estos constraints a nivel de base de datos y aplicación:

A nivel de base de datos:
- @UniqueConstraint en Review sobre reservation_id y reviewer_id
- @Index en columna status de Reservation
- @Index en columna expires_at de Reservation
- @Index en wallet_id de WalletTransaction
- @Index en created_at de WalletTransaction
- @Unique en email de User
- @Unique en code de QRCode
- unique=true en la relación @OneToOne de Wallet con User

A nivel de aplicación en DTOs:
- @NotBlank en todos los campos obligatorios de request DTOs
- @Email en el campo email del RegisterRequest
- @Positive en el monto del TopUpRequest
- @NotNull en parkingSpaceId del CreateReservationRequest
- @Min(1) y @Max(5) en rating del CreateReviewRequest
- @Valid en los parámetros de los controllers que reciben request DTOs

Puntaje: todos los constraints presentes = 0.5,
algunos en entidades principales = 0.3, sin constraints = 0.0.

---

## CRITERIO 2 — DTOs y Mapeo (2 puntos)

### 2.1 Definición de DTOs (1.2 puntos)

Cuenta todos los DTOs del proyecto. Los esperados son:
RegisterRequest, LoginRequest, AuthResponse, ErrorResponse,
CreateParkingSpaceRequest, UpdateParkingSpaceRequest, ParkingSpaceResponse,
HostDashboardResponse, ReservationSummary, CreateReservationRequest,
ReservationResponse, ParkingSpaceInfo, DriverInfo, ParkingUpdateEvent,
TopUpRequest, WalletResponse, WalletTransactionResponse, EarningsSummaryResponse,
QRResponse, CheckInRequest, CheckOutRequest, CheckInResponse, CheckOutResponse,
CreateReviewRequest, ReviewResponse, ParkingSpaceReviewsResponse.

Verifica que:
- Cada DTO tenga una responsabilidad única y clara
- Los DTOs de request estén separados de los de response
- Ningún DTO mezcle datos de entidades distintas sin justificación
- Los DTOs de request tengan anotaciones de validación

Puntaje: más de 10 DTOs bien organizados = 1.2, ocho a diez = 1.0,
seis a siete = 0.8, cuatro a cinco = 0.6, tres o menos = 0.3,
sin DTOs = 0.0.

### 2.2 Mapeo Entidad-DTO (0.8 puntos)

Verifica que el mapeo sea consistente en todo el proyecto:

- Ningún controller retorna una entidad JPA directamente
- ParkingSpaceResponse extrae latitude y longitude como doubles
  del Point de PostGIS, nunca expone el objeto Point crudo
- AuthResponse no incluye el campo password
- ReservationResponse incluye objetos anidados ParkingSpaceInfo y
  DriverInfo sin exponer password
- WalletTransactionResponse incluye typeLabel en español y amountDisplay
  con signo positivo o negativo
- CheckOutResponse incluye durationMinutes, totalCharged y remainingBalance
- ErrorResponse incluye timestamp, status, error, message y path

Reporta cada lugar donde se exponga una entidad directamente o
haya fuga de datos sensibles.

Puntaje: mapeo consistente en todos los endpoints sin fugas = 0.8,
mayoría correcta = 0.6, múltiples problemas = 0.2, sin mapeo = 0.0.

---

## CRITERIO 3 — Arquitectura y Patrones de Diseño (2 puntos)

### 3.1 Separación de Capas (0.8 puntos)

Verifica la arquitectura Controller → Service → Repository:

- Ningún Controller accede directamente a un Repository
- Ningún Controller tiene lógica de negocio, solo llama al Service
  y retorna ResponseEntity
- Los Services tienen toda la lógica de negocio
- Los Repositories solo tienen queries, sin lógica de negocio
- No hay uso de entityManager directamente en Controllers
- Los Controllers usan ResponseEntity con el status HTTP correcto

Indica el archivo y método exacto de cada violación encontrada.

Puntaje: arquitectura limpia sin violaciones = 0.8,
violaciones menores = 0.6, violaciones notables = 0.4,
sin separación = 0.0.

### 3.2 Principio de Responsabilidad Única SRP (0.6 puntos)

Verifica que cada Service tenga una sola responsabilidad:

- UserService o AuthService: solo autenticación y gestión de usuarios
- ParkingSpaceService: solo gestión de cocheras
- ReservationService: solo gestión de reservas
- WalletService: solo operaciones de billetera
- CheckInOutService: solo flujo de entrada y salida
- ReviewService: solo gestión de reseñas
- FirebaseNotificationService: solo notificaciones push
- CloudinaryService: solo gestión de imágenes
- ReservationScheduler: solo tareas programadas

También verifica que ningún método supere aproximadamente 30 líneas
y que los nombres sean descriptivos de su única responsabilidad.

### 3.3 Inyección de Dependencias (0.6 puntos)

Verifica que:

- Todas las dependencias se inyectan por constructor usando
  @RequiredArgsConstructor de Lombok o constructor explícito
- No hay @Autowired en campos, solo en constructores si acaso
- No hay uso de new para instanciar componentes de Spring
- No hay acoplamiento directo entre módulos que debería ser
  mediado por interfaces

Reporta cada caso de @Autowired en campo o instanciación con new.

Puntaje: inyección por constructor en todo el proyecto = 0.6,
buena aplicación con oportunidades de mejora = 0.5,
básica con acoplamiento alto = 0.4, inconsistente = 0.2,
sin inyección apropiada = 0.0.

---

## CRITERIO 4 — Testing (4 puntos)

### 4.1 Testing de Repositorios (1.0 punto)

Verifica que existan tests con @DataJpaTest para los repositorios.
Los repositorios esperados son: UserRepository, ParkingSpaceRepository,
ReservationRepository, WalletRepository, WalletTransactionRepository,
QRCodeRepository y ReviewRepository.

Para cada test de repositorio verifica:
- Usa @DataJpaTest correctamente
- Prueba operaciones CRUD básicas
- Prueba las queries personalizadas como ST_DWithin,
  findExpiredPendingReservations y el AVG de ratings
- Prueba edge cases como búsqueda de entidad inexistente
- Los métodos de test usan nomenclatura BDD: shouldXxxWhenYyy
  por ejemplo shouldFindAvailableSpacesWhenWithinRadius

Puntaje: tests completos para todos los repositorios con BDD = 1.0,
80% o más con buena cobertura o completos sin BDD = 0.8,
60 a 70% con casos básicos = 0.6, cobertura limitada = 0.4,
muy básicos = 0.2, sin tests = 0.0.

### 4.2 Testing de Servicios (1.0 punto)

Verifica que existan tests unitarios con Mockito para los services.
Los services esperados son: AuthService o UserService, ParkingSpaceService,
ReservationService, WalletService, CheckInOutService y ReviewService.

Para cada test de service verifica:
- Usa @ExtendWith(MockitoExtension.class)
- Mockea correctamente todas las dependencias con @Mock
- Prueba la lógica de negocio principal
- Prueba el manejo de excepciones: por ejemplo que lanza
  IllegalStateException cuando la cochera no está disponible,
  que lanza IllegalStateException cuando el saldo es insuficiente
- Prueba casos edge como QR expirado o reserva ya finalizada
- Nomenclatura BDD: shouldXxxWhenYyy

Puntaje: tests completos para todos los services con mocks y BDD = 1.0,
mayoría con buena cobertura o completos sin BDD = 0.8,
principales con mocks básicos = 0.6, cobertura limitada = 0.4,
básicos sin mocks = 0.2, sin tests = 0.0.

### 4.3 Testing de Controladores (1.2 puntos)

Verifica que existan tests de integración con MockMvc. Los controllers
esperados son: AuthController, ParkingSpaceController, ReservationController,
WalletController, CheckInOutController y ReviewController.

Para cada test de controller verifica:
- Usa @WebMvcTest o @SpringBootTest con MockMvc
- Verifica el status HTTP correcto para cada endpoint
- Verifica el body del response con jsonPath
- Prueba casos de error: request inválido retorna 400,
  no autenticado retorna 401, sin permisos retorna 403,
  no encontrado retorna 404
- Mockea el service con @MockBean
- Nomenclatura BDD: shouldXxxWhenYyy

Puntaje: tests completos para más de 5 controllers con BDD = 1.2,
cuatro a cinco controllers con buena cobertura o completos sin BDD = 1.0,
tres controllers = 0.8, dos controllers = 0.6,
uno = 0.3, sin tests = 0.0.

### 4.4 TestContainers (0.8 puntos)

Verifica que exista configuración de TestContainers con
PostgreSQL para tests de integración real. Verifica:

- Dependencia de TestContainers en el pom.xml:
  org.testcontainers postgresql y junit-jupiter
- Existe al menos una clase de test con @Testcontainers y
  @Container con PostgreSQLContainer
- La configuración del contenedor incluye la extensión PostGIS
  para que las queries espaciales funcionen en tests
- Se usan en tests de repositorio o service de integración
- Nomenclatura BDD en los métodos de test

Puntaje: TestContainers en múltiples tests con PostGIS y BDD = 0.8,
en algunos tests con configuración correcta = 0.6,
en al menos un test básico o completo sin BDD = 0.4,
intento con problemas = 0.2, sin TestContainers = 0.0.

---

## CRITERIO 5 — Manejo de Excepciones (2 puntos)

### 5.1 Excepciones Personalizadas (0.8 puntos)

Verifica que existan más de 7 excepciones personalizadas organizadas
por categoría. Las esperadas son:

- ResourceNotFoundException o EntityNotFoundException para 404
- DuplicateResourceException para cuando algo ya existe
- InvalidOperationException para operaciones no permitidas
  como reservar una cochera ocupada
- InsufficientBalanceException para saldo insuficiente
- QRCodeExpiredException para QR vencido
- QRCodeAlreadyUsedException para QR ya utilizado
- UnauthorizedOperationException para acceso no permitido
- ReservationExpiredException para reservas vencidas
- SpaceNotAvailableException para cochera no disponible

Verifica que cada una extienda RuntimeException o una excepción
base del proyecto, y que tengan un mensaje descriptivo en el constructor.

Puntaje: más de 7 excepciones organizadas por categoría = 0.8,
cinco a siete = 0.6, tres a cuatro = 0.4, dos = 0.2,
solo genéricas = 0.0.

### 5.2 Global Exception Handler (1.2 puntos)

Verifica que el GlobalExceptionHandler con @RestControllerAdvice maneje:

- Cada excepción personalizada con su status HTTP correcto
- MethodArgumentNotValidException retorna 400 con lista de errores
- HttpMessageNotReadableException retorna 400
- AccessDeniedException retorna 403
- AuthenticationException retorna 401
- Exception genérica retorna 500

Verifica que el ErrorResponse incluya los campos:
timestamp, status, error, message y path.
Verifica que el path se obtenga del HttpServletRequest.

Puntaje: maneja todas las excepciones con formato consistente
y status codes correctos incluyendo path = 1.2,
mayoría con respuestas consistentes = 1.0,
algunas inconsistencias = 0.5, sin handler global = 0.0.

---

## CRITERIO 6 — Seguridad y Autenticación (4 puntos)

### 6.1 Configuración de Spring Security (1.0 punto)

Verifica que:

- SecurityConfig tiene @Configuration y @EnableWebSecurity
- CSRF está deshabilitado
- Sesión configurada como STATELESS
- CORS está configurado permitiendo el origen del frontend
- /api/auth/** y /ws/** están permitidos sin autenticación
- Todo lo demás requiere autenticación
- El SecurityContext se usa en los services para obtener
  el usuario autenticado con SecurityContextHolder

Puntaje: bien configurado con CORS y SecurityContext funcional = 1.0,
parcialmente funcional = 0.6, básico con errores = 0.3,
sin Spring Security = 0.0.

### 6.2 Sistema JWT (1.5 puntos)

Verifica la implementación completa de JWT:

- JwtService genera tokens con el email y roles en los claims
- JwtAuthFilter extiende OncePerRequestFilter y extrae el
  token del header Authorization con prefijo Bearer
- El filter valida el token y setea el SecurityContextHolder
- UserDetailsService personalizado carga el usuario por email
- La secret key viene de variables de entorno via @Value,
  no hardcodeada en el código
- La expiración del token se valida correctamente
- El AuthResponse incluye el token JWT en el login y registro

Verifica si están implementados refresh tokens. Si no están,
repórtalo como punto de mejora ya que suma para el puntaje máximo.

Puntaje: implementación completa con refresh tokens = 1.5,
JWT funcional con generación validación y filter = 1.2,
implementación parcial = 0.6, muy básica = 0.3, sin JWT = 0.0.

### 6.3 Roles y Autorización (1.0 punto)

Verifica el sistema de roles DRIVER y HOST:

- Los roles están almacenados en la base de datos en la entidad User
- Los roles están incluidos en los claims del JWT
- Existen verificaciones de rol en los endpoints sensibles
  usando @PreAuthorize o verificación manual en el service
- Solo HOST puede crear cocheras
- Solo DRIVER puede hacer reservas
- Solo HOST puede ver el dashboard de ingresos
- Solo el dueño de la reserva puede cancelarla
- Solo los participantes de una reserva pueden dejar reseña

Verifica si existe @EnableMethodSecurity para usar @PreAuthorize.

Puntaje: sistema completo con @PreAuthorize y verificaciones
en services = 1.0, roles básicos en algunos endpoints = 0.6,
sistema muy básico = 0.4, intento con problemas = 0.2,
sin roles = 0.0.

### 6.4 Registro y Login (0.5 puntos)

Verifica que:

- El endpoint POST /api/auth/register valida email único
  lanzando excepción si ya existe
- El password se encripta con BCryptPasswordEncoder antes de guardar
- El endpoint POST /api/auth/login valida credenciales correctamente
- Ambos endpoints retornan el JWT en la respuesta
- El registro inicializa la wallet del usuario nuevo
- El login actualiza el fcmToken si viene en el request

Puntaje: completamente funcional con todas las validaciones = 0.5,
funcional con validaciones básicas = 0.3, con problemas = 0.0.

---

## CRITERIO 7 — API REST y Controllers (2 puntos)

### 7.1 Diseño RESTful (0.8 puntos)

Verifica las convenciones REST en todos los endpoints:

- Las URIs usan recursos en plural: /api/parking-spaces,
  /api/reservations, /api/reviews
- Se usa versionado de API con /api/v1/ en las rutas.
  Si no está presente repórtalo como mejora importante
- Los verbos HTTP son correctos: GET para consultar,
  POST para crear, PUT para actualizar completo,
  PATCH para actualizar parcial, DELETE para eliminar
- Las URIs son descriptivas y no incluyen verbos:
  no debe existir /api/createReservation sino POST /api/reservations
- Los recursos anidados usan rutas apropiadas como
  /api/parking-spaces/{id}/favorites

Puntaje: convenciones REST completas con versionado = 0.8,
mayoría correcta con errores menores = 0.6,
básico con violaciones = 0.4, múltiples problemas = 0.2,
sin convenciones = 0.0.

### 7.2 Códigos de Estado HTTP (0.7 puntos)

Verifica el uso correcto de status codes en cada controller:

- POST que crea recurso retorna 201 Created con el recurso creado
- GET que consulta retorna 200 OK
- DELETE retorna 204 No Content
- Recurso no encontrado retorna 404 Not Found
- Validación fallida retorna 400 Bad Request
- No autenticado retorna 401 Unauthorized
- Sin permisos retorna 403 Forbidden
- Conflicto como email duplicado retorna 409 Conflict
- Error interno retorna 500

Reporta cada endpoint que use el status code incorrecto.

### 7.3 Estructura de Controladores (0.5 puntos)

Verifica que los controllers sean delgados:

- Cada controller tiene @RestController y @RequestMapping
- Se usan @PathVariable, @RequestParam, @RequestBody y
  @RequestPart correctamente según el caso
- @Valid está presente en los parámetros que reciben DTOs
- Los métodos retornan ResponseEntity con el tipo correcto
- No hay lógica de negocio en ningún controller
- Cada método del controller hace exactamente una llamada
  al service y retorna el resultado

---

## CRITERIO 8 — Eventos y Asincronía (2 puntos)

### 8.1 Implementación de Eventos (1.0 punto)

Verifica si existen eventos de Spring usando ApplicationEvent
y @EventListener o @TransactionalEventListener. Los casos de uso
donde se esperan eventos son:

- Al completar una reserva (check-out) se podría publicar un
  ReservationCompletedEvent para disparar el cobro al wallet
- Al registrar un usuario se podría publicar un UserRegisteredEvent
  para enviar email de bienvenida
- Al expirar una reserva se podría publicar un ReservationExpiredEvent

Verifica si existen clases que extiendan ApplicationEvent,
publishers con ApplicationEventPublisher y listeners con @EventListener.

Si no existen eventos de Spring y en cambio toda la lógica
está encadenada directamente en los services, repórtalo como
una brecha importante que resta puntos en este criterio.

Puntaje: eventos en más de 2 casos de uso con publishers
y listeners desacoplados = 1.0, en 2 casos = 0.8,
en 1 caso además del correo = 0.6, solo correo = 0.3,
sin eventos = 0.0.

### 8.2 Procesamiento Asíncrono (0.5 puntos)

Verifica que exista @EnableAsync en la clase principal o
en una clase de configuración, y que los métodos asíncronos
tengan @Async. Los casos esperados son:

- El método sendNotification de FirebaseNotificationService
  debe ser @Async para no bloquear el hilo principal
- Si existe un EmailService debe ser @Async
- Verifica si existe un ThreadPoolTaskExecutor configurado
  con el número de threads apropiado

Puntaje: @Async en múltiples services con ThreadPoolTaskExecutor = 0.5,
@Async básico para notificaciones = 0.3, sin asincronía = 0.0.

### 8.3 Servicio de Correo Electrónico (0.5 puntos)

Verifica si existe un servicio de email con JavaMailSender
o una librería como Resend. Verifica:

- Dependencia de email en el pom.xml
- Configuración de SMTP en application.properties
- Envío de email al registrar un usuario nuevo
- Envío de email al completar una reserva con el resumen del cobro
- Uso de plantillas HTML con Thymeleaf u otro motor de plantillas
- El servicio es @Async para no bloquear el flujo principal
- Maneja excepciones de envío sin propagarlas

Si no existe servicio de email repórtalo como brecha importante
que representa 0.5 puntos perdidos.

---

## CRITERIO 9 — Deployment (2 puntos)

Verifica si existe evidencia de deployment en el proyecto:

- Si hay un archivo Dockerfile en la raíz del proyecto
- Si el docker-compose.yml incluye el servicio de la aplicación
  además de la base de datos
- Si existen variables de entorno configuradas para producción
  separadas de desarrollo
- Si hay un archivo de configuración para Railway, Render,
  Heroku u otra plataforma de deployment
- Si el README incluye un link a la aplicación desplegada

Si no hay evidencia de deployment repórtalo como 2 puntos en riesgo.

Puntaje: desplegado en AWS con ECS y RDS = 2.0,
desplegado en Railway, Render o Heroku con BD en la nube = 1.0,
deployment parcialmente funcional = 0.5, sin deployment = 0.0.

---

## CRITERIO 10 — GitHub y Documentación (1 punto)

### 10.1 README y Documentación (0.4 puntos)

Verifica que el README del proyecto incluya:

- Título descriptivo del proyecto
- Descripción del problema que resuelve
- Tecnologías utilizadas con versiones
- Instrucciones para correr el proyecto localmente con Docker
- Variables de entorno requeridas documentadas
- Lista de todos los endpoints con método, ruta y descripción
- Diagrama entidad-relación o diagrama de clases
- Diagrama de arquitectura del sistema
- Nombres de todos los integrantes del equipo
- Link al deployment si existe

### 10.2 Control de Versiones (0.4 puntos)

Verifica en el historial de Git:

- Los commits son frecuentes y tienen mensajes descriptivos
  en inglés o español consistente
- Existen ramas separadas por feature o por integrante
  siguiendo algo parecido a GitFlow
- No hay archivos sensibles commiteados: .env,
  firebase-service-account.json, API keys hardcodeadas en código
- El .gitignore incluye los archivos sensibles del proyecto
- Hay evidencia de pull requests o merges entre ramas

### 10.3 Gestión de Proyecto (0.2 puntos)

Verifica si existen GitHub Issues o GitHub Projects con:

- Issues creados por funcionalidad o módulo
- Labels organizando los issues por tipo o prioridad
- Milestones para organizar entregas
- Issues asignados a integrantes específicos

---

## CRITERIO BONUS — Elementos Adicionales

Verifica la presencia de estos elementos bonus que pueden
compensar puntos perdidos en otras áreas:

- Documentación Swagger/OpenAPI: existe dependencia
  springdoc-openapi y endpoint /swagger-ui.html funcional
- Logging estructurado: uso de @Slf4j con log.info,
  log.error y log.warn en puntos clave del sistema
- Paginación: los endpoints de listado como historial de
  reservas y transacciones usan Pageable y retornan Page
- Docker Compose completo: el docker-compose.yml incluye
  tanto la base de datos como el servicio de la aplicación
- GitHub Actions: existe archivo en .github/workflows
  con pipeline de CI que corre los tests automáticamente
- Cobertura de tests mayor al 80%: si hay reporte de
  cobertura con JaCoCo u otro plugin

---

## Formato de Respuesta Requerido

Organiza tu respuesta exactamente así:

Para cada criterio dame:
1. Puntaje obtenido sobre el puntaje máximo
2. Lista de lo que está correcto con marca de verificación
3. Lista de lo que falta o está incompleto con advertencia
   indicando exactamente qué archivo y qué método
4. Lista de errores críticos que deben corregirse antes
   de la entrega indicando cómo corregirlos

Al final dame:
- Tabla resumen con puntaje por criterio y puntaje máximo
- Puntaje total estimado sobre 20 puntos
- Top 5 de correcciones más urgentes ordenadas por impacto
  en el puntaje final
- Lista de elementos bonus presentes y ausentes