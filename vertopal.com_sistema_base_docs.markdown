---
identifier: "urn:uuid:18785f3b-571a-4c8f-adde-5bfeae56afd8"
language: es-MX
title: Sistema base --- Especificación de backend
---

[]{#title_page.xhtml}

[]{#nav.xhtml}

```{=html}
<nav epub:type="landmarks" id="landmarks" hidden="hidden">
```
1.  [Title Page](text/title_page.xhtml){.titlepage}
2.  [Table of Contents](#nav.xhtml#toc){.toc}

```{=html}
</nav>
```
[]{#ch001.xhtml}

::: {#ch001.xhtml#propósito-y-alcance .section .level1}
# Propósito y alcance

Este documento define el comportamiento esperado de los módulos
reutilizables del backend y los contratos HTTP que deberán respetar sus
consumidores. La integración interna del frontend se documentará en una
etapa posterior.

Las palabras **debe**, **no debe**, **debería** y **puede** expresan,
respectivamente, una obligación, una prohibición, una recomendación y
una posibilidad de implementación.
:::

[]{#ch002.xhtml}

::: {#ch002.xhtml#convenciones-compartidas .section .level1}
# Convenciones compartidas

::: {#ch002.xhtml#autenticación-y-autorización .section .level2}
## Autenticación y autorización

-   **Autenticación:** demuestra quién es el usuario.
-   **Autorización:** determina qué operaciones puede ejecutar.
-   Un usuario puede tener múltiples roles.
-   Los roles agrupan permisos; los permisos efectivos son la autoridad
    real.
-   El contrato público usa exclusivamente el término `permissions`. No
    se utilizará `privileges` como sinónimo.
-   El frontend puede usar los permisos para adaptar la interfaz, pero
    el backend debe autorizar cada operación protegida.
:::

::: {#ch002.xhtml#sesión .section .level2}
## Sesión

La sesión utiliza dos credenciales:

-   **Access token:** JWT de vida corta, enviado mediante
    `Authorization: Bearer <token>`.
-   **Refresh token:** valor opaco, aleatorio y de larga duración. Se
    entrega exclusivamente mediante una cookie protegida y se almacena
    como hash en el servidor.

El refresh token no aparece en cuerpos JSON ni puede ser leído por
JavaScript. La cookie debe configurarse como mínimo con:

``` http
Set-Cookie: refresh_token=<valor>; HttpOnly; Secure; SameSite=Lax; Path=/api/auth; Max-Age=<segundos>
```

-   `HttpOnly` impide que JavaScript lea el valor de la cookie.
-   `Secure` obliga a enviarla únicamente mediante HTTPS.
-   `SameSite` reduce el envío de la cookie desde sitios externos.
-   `Path` limita los endpoints a los que se adjunta.
-   En producción nunca se debe desactivar `Secure`.

Si la aplicación requiere solicitudes cross-site y utiliza
`SameSite=None`, deberá usar `Secure`, una política CORS con orígenes
explícitos y protección CSRF mediante un token verificable. Aun con
`SameSite=Lax`, las operaciones que dependan de cookies deben validar
`Origin` o `Referer` cuando corresponda. `HttpOnly` reduce el robo del
token mediante XSS, pero no elimina la necesidad de prevenir XSS y CSRF.
:::

::: {#ch002.xhtml#respuesta-exitosa .section .level2}
## Respuesta exitosa

::: {#ch002.xhtml#cb2 .sourceCode}
``` {.sourceCode .json}
{
  "success": true,
  "message": "Operación completada",
  "data": {}
}
```
:::
:::

::: {#ch002.xhtml#respuesta-de-error .section .level2}
## Respuesta de error

::: {#ch002.xhtml#cb3 .sourceCode}
``` {.sourceCode .json}
{
  "success": false,
  "message": "La solicitud contiene datos inválidos",
  "code": "VALIDATION_ERROR",
  "fields": {
    "email": ["El formato del correo es inválido"]
  },
  "requestId": "req_8a7b6c5d4e"
}
```
:::

`requestId` permite correlacionar la respuesta con los logs técnicos y
de auditoría. Los detalles internos y stack traces nunca se devuelven al
cliente.
:::

::: {#ch002.xhtml#códigos-http-y-códigos-de-dominio .section .level2}
## Códigos HTTP y códigos de dominio

  ---------------------------------------------------------------------------
                          HTTP Código de dominio        Uso
  ---------------------------- ------------------------ ---------------------
                           400 `VALIDATION_ERROR`       Campos ausentes,
                                                        formato incorrecto o
                                                        petición mal formada.

                           400 `INVALID_OTP`            OTP incorrecto. La
                                                        respuesta no revela
                                                        si la cuenta existe.

                           401 `UNAUTHORIZED`           Credenciales
                                                        incorrectas o access
                                                        token ausente,
                                                        inválido o expirado.

                           401 `TOKEN_EXPIRED`          Credencial de sesión
                                                        expirada.

                           403 `FORBIDDEN`              Usuario autenticado
                                                        sin el permiso
                                                        requerido o cuenta
                                                        suspendida.

                           404 `NOT_FOUND`              Recurso inexistente
                                                        cuando revelar su
                                                        ausencia sea seguro.
                                                        No se usa para
                                                        enumerar cuentas.

                           409 `CONFLICT`               Conflicto de estado
                                                        en operaciones donde
                                                        sea seguro revelarlo.
                                                        No se usa en el
                                                        registro público para
                                                        indicar que un correo
                                                        ya existe.

                           410 `RESET_TOKEN_EXPIRED`    Token opaco de
                                                        recuperación
                                                        consumido o expirado.
                                                        Puede normalizarse
                                                        como 400 si se
                                                        prefiere no
                                                        distinguir los
                                                        estados.

                           422 `UNPROCESSABLE_ENTITY`   Datos sintácticamente
                                                        válidos que no
                                                        cumplen una regla de
                                                        negocio.

                           429 `RATE_LIMITED`           Se superó el límite
                                                        de solicitudes o
                                                        intentos. Debe
                                                        incluir `Retry-After`
                                                        cuando sea posible.

                           500 `INTERNAL_ERROR`         Fallo interno con
                                                        mensaje opaco y
                                                        `requestId`.
  ---------------------------------------------------------------------------

`NETWORK_ERROR`, `TIMEOUT` y `UNKNOWN` son errores normalizados
localmente por un cliente HTTP; no forman parte del contrato de
respuestas del backend.
:::
:::

[]{#ch003.xhtml}

::: {#ch003.xhtml#módulo-de-autenticación .section .level1}
# Módulo de autenticación

::: {#ch003.xhtml#endpoints .section .level2}
## Endpoints

  --------------------------------------------------------------------------------------------------------------
  Método            Endpoint                      Entrada                                      Resultado
                                                                                               principal
  ----------------- ----------------------------- -------------------------------------------- -----------------
  POST              `/auth/login`                 `{ email, password }`                        Usuario, access
                                                                                               token y
                                                                                               expiración;
                                                                                               refresh token en
                                                                                               cookie.

  POST              `/auth/register`              `{ name, email, password, acceptedTerms }`   Respuesta opaca
                                                                                               sin tokens.

  POST              `/auth/forgot-password`       `{ email }`                                  Respuesta opaca.

  POST              `/auth/verify-otp`            `{ email, otp }`                             Token opaco de
                                                                                               recuperación.

  POST              `/auth/reset-password`        `{ token, password }`                        Confirmación del
                                                                                               cambio.

  POST              `/auth/resend-verification`   `{ email }`                                  Respuesta opaca.

  POST              `/auth/verify-email`          `{ token }`                                  Correo
                                                                                               verificado.

  POST              `/auth/refresh`               Sin body; refresh token en cookie            Usuario, access
                                                                                               token nuevo y
                                                                                               cookie rotada.

  POST              `/auth/logout`                Sin body; refresh token en cookie            Revocación de la
                                                                                               sesión actual y
                                                                                               cookie expirada.

  POST              `/auth/logout-all`            Access token                                 Revocación de
                                                                                               todas las
                                                                                               sesiones del
                                                                                               usuario.

  GET               `/auth/me`                    Access token                                 Usuario y
                                                                                               permisos
                                                                                               actuales.
  --------------------------------------------------------------------------------------------------------------
:::

::: {#ch003.xhtml#contrato-de-usuario-autenticado .section .level2}
## Contrato de usuario autenticado

::: {#ch003.xhtml#cb1 .sourceCode}
``` {.sourceCode .json}
{
  "id": "usr_123",
  "email": "usuario@ejemplo.com",
  "name": "Usuario",
  "roles": ["admin"],
  "permissions": ["users.read", "settings.manage"],
  "isVerified": true
}
```
:::
:::

::: {#ch003.xhtml#registro-público .section .level2}
## Registro público

El registro debe crear una identidad con privilegio mínimo e iniciar la
verificación del correo sin permitir enumeración de cuentas.

1.  Validar `name`, correo normalizado, contraseña y
    `acceptedTerms === true`.
2.  Aplicar rate limiting por IP y por identificador normalizado.
3.  Si el correo no existe, crear el usuario sin verificar, asignar el
    rol predeterminado y registrar `termsAcceptedAt` y la versión de
    términos aceptada.
4.  Crear un token de verificación criptográficamente aleatorio. Guardar
    únicamente su hash, asociado al usuario, propósito, expiración y
    estado de consumo.
5.  Enviar la notificación mediante una cola persistente.
6.  Si el correo ya existe, no crear otra cuenta ni revelar el
    conflicto. Puede enviarse una alerta al propietario, respetando
    límites contra abuso.
7.  En ambos caminos devolver la misma respuesta y un tiempo de
    procesamiento razonablemente uniforme.

``` http
HTTP/1.1 200 OK
```

::: {#ch003.xhtml#cb3 .sourceCode}
``` {.sourceCode .json}
{
  "success": true,
  "message": "Si la dirección puede utilizarse, recibirás instrucciones para verificar la cuenta."
}
```
:::

El endpoint público no devuelve `409`, un usuario ni tokens de sesión.
:::

::: {#ch003.xhtml#política-de-contraseñas .section .level2}
## Política de contraseñas

-   Admitir contraseñas largas y frases de contraseña; no truncarlas
    silenciosamente.
-   Permitir todos los caracteres, incluidos espacios y Unicode.
-   No imponer obligatoriamente combinaciones de mayúsculas, números o
    símbolos.
-   Establecer una longitud mínima de acuerdo con el riesgo de la
    aplicación y permitir al menos 64 caracteres.
-   Comprobar contraseñas comunes o comprometidas cuando exista un
    servicio apropiado.
-   Permitir pegar y usar administradores de contraseñas.
-   Almacenar contraseñas con Argon2id y parámetros calibrados para la
    infraestructura. Bcrypt solo se usará cuando Argon2id no esté
    disponible o por compatibilidad heredada.
-   Los parámetros se revisarán periódicamente; no se fijará un costo
    universal sin pruebas de rendimiento.
:::

::: {#ch003.xhtml#inicio-de-sesión .section .level2}
## Inicio de sesión

1.  Validar y normalizar el correo.
2.  Aplicar límites por IP, cuenta y dimensión global. Evitar bloqueos
    permanentes que permitan denegación de servicio.
3.  Responder con el mismo mensaje ante correo inexistente y contraseña
    incorrecta.
4.  Comparar la contraseña con el hash mediante la función segura de la
    librería elegida.
5.  Comprobar que la cuenta esté verificada y activa.
6.  Emitir un access token de vida corta con `sub`, `jti`, `iat`, `exp`
    y, si se adopta esta estrategia, una versión de sesión o permisos.
7.  Generar un refresh token con un CSPRNG. Guardar solo su hash y los
    metadatos de sesión.
8.  Enviar el refresh token mediante `Set-Cookie` y devolver el access
    token en JSON.

::: {#ch003.xhtml#cb4 .sourceCode}
``` {.sourceCode .json}
{
  "success": true,
  "message": "Inicio de sesión exitoso",
  "data": {
    "user": {
      "id": "usr_789",
      "email": "usuario@ejemplo.com",
      "name": "Usuario",
      "roles": ["admin"],
      "permissions": ["users.read", "settings.manage"],
      "isVerified": true
    },
    "accessToken": "<jwt>",
    "expiresIn": 900
  }
}
```
:::
:::

::: {#ch003.xhtml#rotación-del-refresh-token .section .level2}
## Rotación del refresh token

Cada sesión pertenece a una familia de refresh tokens. La base de datos
deberá conservar información suficiente para detectar reutilización:

``` text
id
user_id
family_id
token_hash
expires_at
used_at
revoked_at
replaced_by
created_at
last_used_at
ip_address
user_agent
```

Flujo obligatorio:

1.  El navegador envía automáticamente la cookie a `POST /auth/refresh`.
2.  El backend deriva el hash del token recibido y busca el registro
    mediante una consulta segura.
3.  Verifica expiración, revocación, pertenencia a la familia y estado
    de la cuenta.
4.  Dentro de una transacción con bloqueo adecuado, marca el token como
    utilizado, crea el reemplazo y enlaza `replaced_by`.
5.  Devuelve un access token nuevo y rota la cookie con un refresh token
    nuevo.
6.  Si se presenta un token ya utilizado o revocado, se considera
    posible reutilización. El backend revoca toda la familia, registra
    el evento de seguridad y obliga a iniciar sesión de nuevo.
7.  Dos solicitudes concurrentes no deben producir dos refresh tokens
    activos. La operación debe ser atómica y el sistema debe definir una
    tolerancia mínima únicamente si la arquitectura la necesita.
8.  Los tokens expirados pueden eliminarse después de un periodo de
    conservación que permita investigación y detección de reutilización;
    no deben borrarse antes de perder esa capacidad.

El valor original del refresh token nunca se almacena ni se registra.
:::

::: {#ch003.xhtml#cierre-de-sesión .section .level2}
## Cierre de sesión

`POST /auth/logout` no recibe body. El navegador envía la cookie
`HttpOnly` automáticamente.

1.  El backend obtiene el refresh token de la cookie y calcula su hash.
2.  Revoca la sesión actual y registra el evento `LOGOUT` de manera
    idempotente.
3.  Expira la cookie con los mismos atributos `Path`, `Domain`, `Secure`
    y `SameSite` usados al crearla.
4.  Devuelve `200 OK` aunque la cookie falte, el token haya expirado o
    la sesión ya esté revocada.
5.  El access token puede seguir siendo válido durante su breve vida
    restante. Para sistemas de mayor riesgo puede utilizarse una
    denylist por `jti` o una versión de sesión comprobada por el
    backend.

``` http
Set-Cookie: refresh_token=; HttpOnly; Secure; SameSite=Lax; Path=/api/auth; Max-Age=0
```

::: {#ch003.xhtml#cb7 .sourceCode}
``` {.sourceCode .json}
{
  "success": true,
  "message": "Sesión cerrada correctamente"
}
```
:::

`POST /auth/logout-all` revoca todas las familias del usuario y se
reserva para "cerrar todas las sesiones".
:::

::: {#ch003.xhtml#recuperación-de-contraseña .section .level2}
## Recuperación de contraseña

::: {#ch003.xhtml#fase-1-solicitud-del-otp .section .level3}
### Fase 1: solicitud del OTP

1.  Validar y normalizar el correo.
2.  Aplicar rate limiting por IP, cuenta y límite global.
3.  Seguir un flujo y tiempo de respuesta razonablemente uniformes,
    exista o no la cuenta. No depender de un `sleep` fijo como defensa
    principal.
4.  Para una cuenta válida, generar el OTP con un CSPRNG.
5.  Guardar solo un hash o HMAC del OTP con `user_id`, propósito,
    intentos, expiración y fecha de creación.
6.  Invalidar códigos anteriores en una operación atómica.
7.  Enviar el correo mediante una cola persistente.
8.  Responder siempre con un mensaje opaco.

::: {#ch003.xhtml#cb8 .sourceCode}
``` {.sourceCode .json}
{
  "success": true,
  "message": "Si el correo está registrado, recibirás un código de recuperación."
}
```
:::
:::

::: {#ch003.xhtml#fase-2-verificación-del-otp .section .level3}
### Fase 2: verificación del OTP

1.  Validar que el OTP tenga el formato esperado.
2.  Localizar el desafío mediante el usuario y el propósito, sin revelar
    si la cuenta existe.
3.  Comprobar expiración y contador de intentos.
4.  Derivar el hash o HMAC del OTP recibido y compararlo de manera
    segura.
5.  Incrementar el contador de forma atómica cuando falle; invalidar el
    desafío al alcanzar el límite.
6.  Al acertar, consumir el OTP inmediatamente.
7.  Generar un token de recuperación **opaco**, aleatorio, de un solo
    uso y con vigencia corta. Guardar solamente su hash con `user_id`,
    `purpose=password_reset`, `expires_at` y `used_at`.
8.  Entregar el token opaco en la respuesta. No es un JWT y no contiene
    claims.
:::

::: {#ch003.xhtml#fase-3-cambio-de-contraseña .section .level3}
### Fase 3: cambio de contraseña

1.  Recibir `{ token, password }` y localizar el registro mediante el
    hash del token.
2.  Verificar propósito, expiración y que `used_at` sea nulo.
3.  Validar la contraseña conforme a la política vigente.
4.  Dentro de una transacción, guardar el nuevo hash de contraseña,
    marcar el token como utilizado, actualizar `password_updated_at` y
    revocar todas las sesiones del usuario.
5.  Registrar `PASSWORD_CHANGED` sin almacenar contraseñas ni tokens.
6.  Enviar una notificación informativa. No enviar la contraseña ni
    iniciar sesión automáticamente.
:::
:::

::: {#ch003.xhtml#verificación-del-correo .section .level2}
## Verificación del correo

El token de verificación será opaco, aleatorio, de un solo uso y
almacenado únicamente como hash. Al verificarse deberá marcarse como
consumido y actualizarse el usuario dentro de una misma transacción. La
respuesta ante token inválido, usado o expirado no debe revelar
información adicional.
:::

::: {#ch003.xhtml#estado-actual-authme .section .level2}
## Estado actual: `/auth/me`

`GET /auth/me` valida el access token y consulta el estado actual del
usuario, sus roles y permisos. Devuelve la fuente de verdad utilizada
para sincronizar al consumidor:

::: {#ch003.xhtml#cb9 .sourceCode}
``` {.sourceCode .json}
{
  "success": true,
  "message": "Sesión recuperada",
  "data": {
    "user": {
      "id": "usr_789",
      "email": "usuario@ejemplo.com",
      "name": "Usuario",
      "roles": ["admin"],
      "permissions": ["users.read", "settings.manage"],
      "isVerified": true
    }
  }
}
```
:::

`/auth/me` no sustituye la autorización del backend. Que una interfaz o
un access token contenga un permiso no autoriza por sí solo una
operación.
:::
:::

[]{#ch004.xhtml}

::: {#ch004.xhtml#gestión-de-usuarios-y-rbac .section .level1}
# Gestión de usuarios y RBAC

::: {#ch004.xhtml#modelo-relacional .section .level2}
## Modelo relacional

-   `users`
-   `roles`
-   `permissions`
-   `user_roles`
-   `role_permissions`

Los nombres de permisos siguen la forma `recurso.acción`, por ejemplo
`users.read`, `users.update`, `roles.assign` y `audit.read`.

El backend debe comprobar el permiso requerido en cada caso de uso. Para
evitar que un access token conserve autoridad retirada durante demasiado
tiempo se utilizarán access tokens breves y, para cambios críticos,
revocación de sesión o una versión de autorización consultada por el
middleware.
:::
:::

[]{#ch005.xhtml}

::: {#ch005.xhtml#módulo-de-auditoría .section .level1}
# Módulo de auditoría

La auditoría registra eventos de negocio, seguridad y cumplimiento. Es
distinta de los logs técnicos y se almacena de forma persistente en
PostgreSQL.

::: {#ch005.xhtml#eventos-mínimos .section .level2}
## Eventos mínimos

-   `CREATE`, `UPDATE`, `DELETE`
-   `LOGIN_SUCCEEDED`, `LOGIN_FAILED`, `LOGOUT`
-   `PASSWORD_CHANGED`
-   `ROLE_CHANGED`, `PERMISSION_CHANGED`
-   `REFRESH_TOKEN_REUSE_DETECTED`
-   `ACCOUNT_SUSPENDED`, `ACCOUNT_REACTIVATED`
:::

::: {#ch005.xhtml#estructura .section .level2}
## Estructura

::: {#ch005.xhtml#cb1 .sourceCode}
``` {.sourceCode .json}
{
  "id": "audit_123",
  "requestId": "req_8a7b6c5d4e",
  "actorId": "usr_789",
  "action": "ROLE_CHANGED",
  "entityType": "USER",
  "entityId": "usr_456",
  "before": { "roles": ["viewer"] },
  "after": { "roles": ["editor"] },
  "ipAddress": "192.0.2.10",
  "userAgent": "Mozilla/5.0...",
  "createdAt": "2026-09-16T12:15:00Z"
}
```
:::

No se registran contraseñas, access tokens, refresh tokens, cookies,
secretos, claves API ni códigos OTP. La sanitización debe cubrir nombres
equivalentes y encabezados sensibles como `Authorization`, `Cookie` y
`Set-Cookie`.
:::

::: {#ch005.xhtml#integridad-y-entrega .section .level2}
## Integridad y entrega

-   Los registros son inmutables desde la API ordinaria.
-   Las operaciones sensibles y su evento de auditoría deben coordinarse
    mediante un patrón transactional outbox cuando no se pueda aceptar
    la pérdida del evento.
-   Un consumidor asíncrono procesa el outbox de forma idempotente y
    confirma su entrega.
-   Se definen índices, retención, archivado y acceso a datos
    personales.
-   Los fallos del subsistema de auditoría deben generar alertas
    técnicas; no deben desaparecer silenciosamente.
:::

::: {#ch005.xhtml#consulta .section .level2}
## Consulta

``` http
GET /admin/audit-logs?action=ROLE_CHANGED&actorId=usr_789&from=2026-09-01&page=1&limit=50
```

El endpoint requiere el permiso `audit.read`, aplica paginación y orden
estable, limita el tamaño de página y registra el acceso a la propia
auditoría cuando la política de cumplimiento lo requiera. No se autoriza
mediante nombres de rol codificados.
:::
:::

[]{#ch006.xhtml}

::: {#ch006.xhtml#logs-técnicos .section .level1}
# Logs técnicos

Los logs técnicos sirven para observabilidad, diagnóstico, alertas
tempranas y captura de excepciones. Son distintos de los logs de
auditoría: describen la salud de la aplicación, no acciones de negocio
que deban conservarse como evidencia.

::: {#ch006.xhtml#decisión-arquitectónica .section .level2}
## Decisión arquitectónica

El núcleo de la aplicación dependerá de una abstracción `ISystemLogger`,
no de una librería concreta. La interfaz expone operaciones como
`debug`, `info`, `warn` y `error`, todas con contexto estructurado.

``` text
ISystemLogger
  debug(message, context)
  info(message, context)
  warn(message, context)
  error(message, error, context)
```

Una implementación adaptadora ---por ejemplo `MonologSystemLogger`,
`WinstonSystemLogger` o `LogbackSystemLogger`, según la tecnología de la
plantilla--- traduce ese contrato a la librería seleccionada. La
inyección de dependencias permite sustituir el proveedor sin modificar
los casos de uso.
:::

::: {#ch006.xhtml#integración .section .level2}
## Integración

-   Un middleware inicial crea o propaga un `requestId` y lo añade a la
    respuesta.
-   Un middleware o manejador global de excepciones captura errores no
    controlados, invoca `ISystemLogger.error()` y devuelve un error
    `INTERNAL_ERROR` opaco.
-   Los servicios pueden registrar eventos técnicos relevantes mediante
    la interfaz, pero no deben registrar secretos ni duplicar eventos de
    auditoría.
-   La sanitización central elimina o enmascara `password`, `token`,
    `refreshToken`, `otp`, `Authorization`, `Cookie`, `Set-Cookie`,
    claves API y nombres equivalentes.
:::

::: {#ch006.xhtml#almacenamiento-y-consulta .section .level2}
## Almacenamiento y consulta

La salida predeterminada es JSON estructurado en `stdout`. La
infraestructura ---por ejemplo Loki, ELK, CloudWatch o Datadog---
recolecta, indexa, retiene y alerta sobre esos eventos. La base de datos
transaccional de la aplicación no se utiliza como repositorio primario
de logs técnicos porque competiría con las operaciones de negocio.

No se expone inicialmente un endpoint `GET /admin/system-logs`. La
consulta se realiza en la plataforma de observabilidad. Solo se añadirá
una API de consulta si existe un requisito explícito y un repositorio
técnico diseñado para ello.

Campos recomendados: `timestamp`, `level`, `requestId`, `service`,
`environment`, `context`, `message`, `error.type`, `error.code`,
`endpoint` y duración. Los stack traces solo se conservan en el entorno
de observabilidad y nunca se devuelven al consumidor.
:::
:::

[]{#ch007.xhtml}

::: {#ch007.xhtml#módulo-interno-de-notificaciones .section .level1}
# Módulo interno de notificaciones

El módulo centraliza el envío de correo, SMS, push, WhatsApp u otros
canales. No expone un controlador público: otros módulos se comunican
con él mediante un servicio interno o eventos.

::: {#ch007.xhtml#patrones-elegidos .section .level2}
## Patrones elegidos

::: {#ch007.xhtml#estrategia .section .level3}
### Estrategia

`INotificationChannel` define el contrato común:

``` text
send(recipient, message, context) -> DeliveryResult
```

Implementaciones iniciales:

-   `EmailChannel`
-   `SmsChannel`
-   `PushChannel`
-   `WhatsAppChannel`

Cada implementación encapsula un canal y su proveedor. Cambiar SendGrid
por SES, por ejemplo, no modifica autenticación ni los casos de uso
consumidores.
:::

::: {#ch007.xhtml#fábrica .section .level3}
### Fábrica

`NotificationChannelFactory` resuelve la estrategia correspondiente a un
tipo de canal mediante configuración e inyección de dependencias. La
fábrica no contiene reglas de negocio ni construye proveedores
manualmente dentro de los controladores.
:::

::: {#ch007.xhtml#servicioorquestador .section .level3}
### Servicio/orquestador

`NotificationService` es el único punto de entrada del módulo. Recibe
una notificación de dominio, carga la plantilla, selecciona uno o varios
canales mediante la fábrica, aplica preferencias y delega el envío.

``` text
NotificationService.notify(notification)
  -> TemplateRenderer.render(...)
  -> NotificationChannelFactory.resolve(channel)
  -> INotificationChannel.send(...)
```
:::
:::

::: {#ch007.xhtml#integración-y-resiliencia .section .level2}
## Integración y resiliencia

Los trabajos importantes se envían mediante una cola persistente, son
idempotentes y cuentan con reintentos limitados, backoff y una cola de
mensajes fallidos. El contenido no debe incluir contraseñas, refresh
tokens ni otros secretos persistentes. Los resultados técnicos del
proveedor van a logs técnicos; los eventos de negocio relevantes, a
auditoría.
:::
:::

[]{#ch008.xhtml}

::: {#ch008.xhtml#módulo-de-almacenamiento .section .level1}
# Módulo de almacenamiento

El módulo centraliza la recepción, validación, persistencia, lectura y
eliminación de archivos sin acoplar el dominio a un proveedor.

::: {#ch008.xhtml#patrones-elegidos-1 .section .level2}
## Patrones elegidos

::: {#ch008.xhtml#estrategia-1 .section .level3}
### Estrategia

`IStorageStrategy` define el contrato común:

``` text
upload(file, metadata) -> StoredFile
delete(storageKey) -> void
getSignedUrl(storageKey, expiresIn) -> url
```

Implementaciones previstas:

-   `LocalDiskStorage`: desarrollo y entornos controlados.
-   `S3Storage`: Amazon S3 y proveedores compatibles como MinIO o
    Cloudflare R2.
-   `CloudinaryStorage`: alternativa especializada para transformación
    de imágenes cuando el proyecto lo requiera.
:::

::: {#ch008.xhtml#fábrica-1 .section .level3}
### Fábrica

`StorageStrategyFactory` selecciona la estrategia configurada para el
entorno o el tipo de recurso. El resto de la aplicación no conoce SDK,
bucket ni credenciales del proveedor.
:::

::: {#ch008.xhtml#servicioorquestador-1 .section .level3}
### Servicio/orquestador

`StorageService` aplica las reglas comunes antes de delegar la
persistencia: autorización, tamaño, extensión, contenido real, tipo
MIME, nombre generado por el servidor, clasificación pública o privada y
análisis antimalware cuando el riesgo lo exija.

``` text
StorageService.upload(command)
  -> FileSecurityValidator.validate(file)
  -> StorageStrategyFactory.resolve(target)
  -> IStorageStrategy.upload(file, metadata)
  -> StoredFile
```

El servicio debe impedir path traversal, almacenar recursos privados por
defecto y usar URLs firmadas cuando corresponda. La base de datos
conserva metadatos y una `storageKey`, no una URL de proveedor asumida
como permanente. La autorización para subir, leer y eliminar pertenece
al backend y se expresa mediante permisos.
:::
:::
:::

[]{#ch009.xhtml}

::: {#ch009.xhtml#módulo-de-pagos .section .level1}
# Módulo de pagos

El módulo abstrae cobros, reembolsos y suscripciones sin exponer al
dominio los SDK de cada proveedor. No almacena datos completos de
tarjetas y utiliza los componentes alojados o tokenizados del proveedor
para reducir el alcance de PCI DSS.

::: {#ch009.xhtml#patrones-elegidos-2 .section .level2}
## Patrones elegidos

::: {#ch009.xhtml#estrategia-2 .section .level3}
### Estrategia

`IPaymentGateway` define las capacidades comunes:

``` text
createPaymentIntent(command) -> PaymentIntent
refund(command) -> RefundResult
cancelSubscription(command) -> SubscriptionResult
verifyWebhook(rawBody, headers) -> VerifiedPaymentEvent
```

Implementaciones iniciales:

-   `StripePaymentGateway`
-   `PayPalPaymentGateway`

Las diferencias inevitables entre proveedores se traducen a modelos
internos; no deben filtrarse directamente a los casos de uso.
:::

::: {#ch009.xhtml#fábrica-2 .section .level3}
### Fábrica

`PaymentGatewayFactory` resuelve el gateway según la configuración,
moneda, región o método admitido. No se instancia un SDK de pago dentro
de los controladores.
:::

::: {#ch009.xhtml#servicioorquestador-2 .section .level3}
### Servicio/orquestador

`PaymentService` ejecuta los casos de uso, valida reglas de negocio,
genera claves de idempotencia, persiste el estado local y delega la
operación externa al gateway.

``` text
PaymentService.createIntent(command)
  -> PaymentGatewayFactory.resolve(provider)
  -> IPaymentGateway.createPaymentIntent(command)
  -> PaymentRepository.save(...)
```
:::
:::

::: {#ch009.xhtml#webhooks .section .level2}
## Webhooks

`POST /webhooks/payments/{provider}` es público en el sentido de que no
usa la sesión de un usuario, pero no es confiable por defecto. Un
adaptador de webhook conserva el cuerpo original, selecciona el gateway
y verifica la firma antes de convertir el evento externo a un evento
interno.

Los webhooks deben aplicar protección contra replay, ser idempotentes y
registrar los identificadores de evento ya procesados. La recepción y el
procesamiento pueden separarse mediante una cola persistente. El estado
local del pago se actualiza a partir de eventos verificados, no de una
redirección del navegador. Los cambios relevantes se registran en
auditoría y los fallos de comunicación en logs técnicos.
:::
:::

Etapa 0 — Base técnica
Crear proyecto.
Configurar PostgreSQL.
Configurar Flyway.
Definir ApiResponse.
Manejo inicial de excepciones.
Perfiles dev, test y prod.
Etapa 1 — Seguridad
Modelo mínimo de identidad.
Registro y verificación.
Login.
JWT.
Refresh token.
Logout.
Recuperación.
Autorización por permisos.
Etapa 2 — Usuarios y RBAC
CRUD administrativo de usuarios.
Suspensión y reactivación.
CRUD de roles.
Catálogo de permisos.
Asignación de múltiples roles.
Resolución de permisos efectivos.
Invalidación de sesiones cuando cambie la autoridad.
Etapa 3 — Logs técnicos
Logging estructurado.
Correlación.
Sanitización.
Excepciones globales.
Observabilidad.
Etapa 4 — Auditoría
Eventos de seguridad y negocio.
Persistencia.
Inmutabilidad.
Outbox.
Consulta administrativa.
Etapa 5 — Notificaciones
Estrategia.
Fábrica.
Orquestador.
Email inicial.
Cola y reintentos.
Etapa 6 — Almacenamiento
Estrategias local y S3.
Validación.
Archivos privados.
URLs firmadas.
Etapa 7 — Pagos
IPaymentGateway.
Fábrica.
Stripe Sandbox.
Webhooks.
Idempotencia.
