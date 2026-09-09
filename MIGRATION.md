# Migración tecnológica — Online Shopping Store

Documento de trazabilidad del "antes / después". El objetivo fue **actualizar** el
proyecto original sin cambiar funcionalidad: mismas entidades, mismos endpoints,
mismas pantallas, mismos roles (CUSTOMER / EMPLOYEE / MANAGER).

Repositorio base: <https://github.com/zhulinn/SpringBoot-Angular7-Online-Shopping-Store>

---

## 1. Resumen de versiones

| Componente | Antes | Después |
|---|---|---|
| Java | 11 | **21 (LTS)** |
| Spring Boot | 2.2.0.BUILD-SNAPSHOT | **4.1.1** |
| Spring Framework | 5.2 | **7.x** |
| Spring Security | 5.2 | **7.x** |
| Hibernate / JPA | 5.4 (`javax.persistence`) | **7.x (`jakarta.persistence`)** |
| JJWT | 0.9.1 | **0.13.0** |
| Bean Validation | `javax.validation` | **`jakarta.validation`** |
| Servlet API | `javax.servlet` | **`jakarta.servlet`** |
| Maven | sin wrapper funcional | **3.9.16 + Maven Wrapper 3.3.4** |
| Pruebas backend | JUnit 4 + SpringRunner | **JUnit 5 + MockitoExtension** |
| Angular | 7.1 | **21.2** |
| TypeScript | 3.1 | **5.9** |
| RxJS | 6.3 | **7.8** |
| zone.js | 0.8 | **0.15** |
| Angular builder | `@angular-devkit/build-angular:browser` | **`@angular/build:application`** |
| Linter frontend | TSLint 5 (descontinuado) | **ESLint 9 + angular-eslint 21** |
| E2E | Protractor 5 (descontinuado) | eliminado (pendiente reemplazo) |
| PostgreSQL | 9.4.5 | **17-alpine** |
| Imagen backend | `openjdk:11-oracle` | **multi-etapa `maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-alpine`** |
| Imagen frontend | `nginx:1.13.3-alpine` | **multi-etapa `node:22-alpine` → `nginx:1.27-alpine`** |
| docker-compose | `version: '2'` | esquema actual, healthcheck y volumen persistente |

---

## 2. Cambios en el backend

### 2.1 `pom.xml`
- Parent `spring-boot-starter-parent` → **4.1.1**; se eliminaron los repositorios de
  *snapshots* y *milestones* de Spring (el original dependía de una build no publicada).
- `java.version` 11 → **21**.
- Se añadió `spring-boot-starter-validation`: desde Spring Boot 2.3 la validación
  ya no viene incluida en `spring-boot-starter-web`, y el proyecto usa `@Valid`, `@NotEmpty`, etc.
- `jjwt` 0.9.1 (artefacto único) → **`jjwt-api` + `jjwt-impl` + `jjwt-jackson` 0.13.0**.
- Se añadió `spring-security-test`.
- Lombok se declara como *annotation processor path* y se excluye del jar ejecutable.
- Surefire ejecuta Mockito como **javaagent** (`-javaagent:${org.mockito:mockito-core:jar}`),
  porque desde JDK 21 el auto-attach está deprecado y emite advertencia.

### 2.2 Maven Wrapper
El repositorio original declaraba `mvnw` pero **no incluía `.mvn/wrapper/`**, así que el
wrapper no funcionaba. Se reinstaló el wrapper 3.3.4 apuntando a Maven 3.9.16.

### 2.3 Migración `javax` → `jakarta`
Jakarta EE 9+ renombró todos los paquetes. Se migraron:
`javax.persistence.*`, `javax.validation.*` y `javax.servlet.*`
en entidades, formularios, filtros y controladores. (`javax.sql.DataSource` **no** cambia,
es parte del JDK.)

### 2.4 `SpringSecurityConfig`
`WebSecurityConfigurerAdapter` fue eliminado en Spring Security 6. La configuración se
reescribió como beans:

| Antes | Después |
|---|---|
| `extends WebSecurityConfigurerAdapter` | clase `@Configuration` plana |
| `configure(AuthenticationManagerBuilder)` + `jdbcAuthentication()` | bean `UserDetailsService` con `JdbcUserDetailsManager` |
| `authenticationManagerBean()` | bean `AuthenticationManager` (`ProviderManager` + `DaoAuthenticationProvider`) |
| `configure(HttpSecurity)` | bean `SecurityFilterChain` |
| `.antMatchers(...)` | `.requestMatchers(...)` |
| `.access("hasAnyRole('EMPLOYEE','MANAGER')")` | `.hasAnyRole("EMPLOYEE", "MANAGER")` |
| DSL encadenado (`.and()`) | DSL con lambdas |

Las reglas de autorización son **exactamente las mismas** que en el original, con una
corrección obligatoria: Spring 6+ usa `PathPatternParser`, que **no admite `**` en medio
de un patrón**. `"/seller/**/delete"` lanzaba `PatternParseException` al arrancar y se
reemplazó por `"/seller/product/*/delete"`, que cubre el único endpoint existente
(`DELETE /seller/product/{id}/delete`).

### 2.5 `ShopApiApplication`
`WebMvcConfigurerAdapter` (eliminado en Spring 5) → se implementa `WebMvcConfigurer`
directamente. El CORS se declara explícito (`allowedOriginPatterns`, métodos, cabeceras)
porque en Spring 6+ un `addMapping("/**")` sin más ya no permite todos los métodos.

### 2.6 `JwtProvider`
JJWT 0.13 cambió la API y endureció la firma:

| Antes | Después |
|---|---|
| `Jwts.builder().setSubject(...)` | `.subject(...)` |
| `.setIssuedAt` / `.setExpiration` | `.issuedAt` / `.expiration` |
| `.signWith(SignatureAlgorithm.HS512, String)` | `.signWith(SecretKey, Jwts.SIG.HS512)` |
| `Jwts.parser().setSigningKey(s).parseClaimsJws(t)` | `Jwts.parser().verifyWith(key).build().parseSignedClaims(t)` |
| `.getBody()` | `.getPayload()` |

> **Importante:** HS512 exige ahora una clave de **al menos 512 bits (64 caracteres)**.
> El secreto original (`me.zhulin`, 9 caracteres) ya no es válido y fue reemplazado por
> un valor por defecto de 81 caracteres, sobreescribible con la variable `JWT_SECRET`.
> (Un primer intento con 63 caracteres seguía fallando: `The signing key's size is 504 bits`.)

### 2.7 `application.yml`
| Antes | Después | Motivo |
|---|---|---|
| `spring.datasource.platform` | `spring.sql.init.platform` | renombrada en Boot 2.5 |
| `spring.datasource.initialization-mode` | `spring.sql.init.mode` | renombrada en Boot 2.5 |
| `spring.datasource.continue-on-error` | `spring.sql.init.continue-on-error` | renombrada en Boot 2.5 |
| `server.servlet.contextPath` | `server.servlet.context-path` | forma canónica |
| `database-platform: PostgreSQL9Dialect` | (eliminado) | dialecto retirado en Hibernate 6 |
| `hibernate.temp.use_jdbc_metadata_defaults` | (eliminado) | workaround obsoleto |
| credenciales fijas | variables de entorno con valor por defecto | despliegue en Docker/CI |

Se añadió `spring.jpa.defer-datasource-initialization: true`. `import.sql` se sigue
cargando por Hibernate igual que antes.

### 2.8 `import.sql` — datos de ejemplo

**El problema más serio de la migración.** El archivo original insertaba **por posición**:

```sql
INSERT INTO "public"."users" VALUES (2147483641, 't', '3200 West Road', ...);
```

Eso dependía del orden de columnas que generaba Hibernate 5 (el `id` primero y el resto
alfabéticamente). Hibernate 6/7 genera otro orden — por ejemplo `users` pasa a ser
`(active, id, address, email, ...)` y `order_main` queda
`(order_amount, order_status, create_time, order_id, ...)`. El resultado es que **todos los
INSERT fallan** con errores de conversión de tipos y la aplicación arranca sin datos:
sin productos, sin usuarios y sin poder iniciar sesión.

Los 47 `INSERT` se reescribieron nombrando sus columnas de forma explícita. Los datos son
idénticos; ahora son independientes de la versión de Hibernate.

Otros cambios derivados del mismo salto:
- La PK de `cart` pasó de `cart_id` a `user_id` (comportamiento de `@MapsId` en Hibernate 6+).
- `GenerationType.AUTO` ya no usa `identity` en PostgreSQL sino secuencias
  (`users_seq`, `order_main_seq`, …). No hay colisión con los ids de ejemplo, que están
  cerca de `Integer.MAX_VALUE`.

### 2.9 Pruebas
- `@RunWith(SpringRunner.class)` → `@ExtendWith(MockitoExtension.class)`
  (con `@MockitoSettings(strictness = LENIENT)` para no romper los *stubs* heredados).
- `org.junit.Test` / `@Before` → `org.junit.jupiter.api.Test` / `@BeforeEach`.
- `@Test(expected = MyException.class)` → `assertThrows(MyException.class, () -> { ... })`.
- `org.junit.Assert.assertThat` → `org.hamcrest.MatcherAssert.assertThat`.
- Se eliminó `ShopApiApplicationTests`: era una *suite* de JUnit 4 (`@RunWith(Suite.class)`)
  que solo reagrupaba las demás clases; Surefire ya las descubre automáticamente.

**Resultado: 37 pruebas, 37 en verde.**

> Nota para pruebas futuras: Spring Boot 4 dividió `spring-boot-starter-test` en módulos.
> `@AutoConfigureMockMvc` ya no está en `spring-boot-test-autoconfigure`; hay que añadir
> `spring-boot-starter-webmvc-test` y el paquete pasó a ser
> `org.springframework.boot.webmvc.test.autoconfigure`.

---

## 3. Cambios en el frontend

### 3.1 `package.json`
Todas las dependencias Angular saltaron de 7.x a 21.x. Se eliminaron paquetes muertos:
`core-js` (ya no hace falta), `codelyzer`, `tslint`, `protractor`, `jasminewd2`, `ts-node`.
Se conserva **Bootstrap 4.6** a propósito: subir a Bootstrap 5 renombraría las clases de
utilidad (`ml-*`, `mr-*`, `data-toggle`, …) y cambiaría la interfaz, lo que ya no sería
"solo actualizar".

### 3.2 `angular.json`
Reescrito al esquema actual:
- builder `@angular-devkit/build-angular:browser` → **`@angular/build:application`**
  (`main` → `browser`, `polyfills` pasa a ser un array).
- `browserTarget` → `buildTarget`; se añadió la configuración `development`.
- Opciones retiradas por el CLI: `extractCss`, `aot`, `buildOptimizer`, `vendorChunk`.
- `defaultProject` ya no existe.
- Proyecto `shop-e2e` (Protractor) eliminado.
- `lint` pasa del builder de TSLint a `@angular-eslint/builder:lint`.

### 3.3 Estructura de archivos
| Antes | Después |
|---|---|
| `src/polyfills.ts` | eliminado → `"polyfills": ["zone.js"]` en `angular.json` |
| `src/test.ts` (`require.context`) | eliminado; el builder descubre los `.spec.ts` |
| `src/karma.conf.js` | eliminado (config por defecto del builder) |
| `src/tsconfig.app.json`, `src/tsconfig.spec.json` | movidos a la raíz |
| `src/browserslist` | `.browserslistrc` con navegadores vigentes |
| `tslint.json`, `src/tslint.json` | `eslint.config.js` (flat config) |
| `e2e/` (Protractor) | eliminado |

### 3.4 `tsconfig.json`
`target` ES5 → **ES2022**, `module` ES2015 → ES2022, `moduleResolution` node → **bundler**.
Se dejó `strict: false` y `strictTemplates: false` a propósito: activarlos obligaría a
tipar todo el código heredado, que es un cambio de código, no una actualización.

### 3.5 Código de la aplicación
- **`standalone: false`** añadido a los 13 componentes. Desde Angular 19 los componentes
  son *standalone* por defecto y no pueden declararse en un `NgModule`; esta es la
  migración oficial para aplicaciones basadas en módulos.
- `HttpClientModule` (deprecado) → `provideHttpClient(withInterceptorsFromDi())` en
  `AppModule.providers`. Los dos interceptores de clase siguen funcionando igual.
- `throwError(error)` → `throwError(() => error)` (RxJS 7).
- `styles.css`: `@import "~bootstrap/..."` → `@import "bootstrap/..."`; el prefijo `~`
  de webpack ya no existe con esbuild.
- Se añadió `src/app/app.component.spec.ts`: el proyecto original **no tenía ninguna
  prueba de frontend** y el builder de Karma necesita al menos un `.spec.ts` para correr.

### 3.6 ESLint
`ng lint` termina en **0 errores y 154 avisos**. Los avisos corresponden a reglas nuevas
que exigirían reescribir código y quedan documentados como trabajo pendiente para la
práctica de calidad:

| Regla | Avisos | Qué pide |
|---|---:|---|
| `@angular-eslint/template/prefer-control-flow` | 53 | `*ngIf`/`*ngFor` → `@if`/`@for` |
| `@angular-eslint/prefer-inject` | 44 | inyección por constructor → `inject()` |
| `@typescript-eslint/no-unused-vars` | 28 | variables sin usar |
| `@typescript-eslint/no-explicit-any` | 16 | tipar los `any` |
| `@angular-eslint/prefer-standalone` | 13 | migrar de `NgModule` a *standalone* |
| `@angular-eslint/template/eqeqeq` | 13 | `==` → `===` en plantillas |
| otros | 2 | métodos de ciclo de vida vacíos |

También queda pendiente `angular-eslint/templateAccessibility` (~90 avisos de accesibilidad),
desactivado por ahora para no bloquear la construcción.

---

## 4. Docker

- **Backend**: `Dockerfile` multi-etapa. Antes exigía haber ejecutado `mvn package` a mano
  y copiaba el jar; ahora compila dentro de la imagen y corre como usuario sin privilegios.
- **Frontend**: igual, compila con Node dentro de la imagen. El builder `application`
  publica en `dist/shop/browser` (antes `dist/shop`), y el `COPY` se ajustó.
- **`docker-compose.yml`**: se quitó `version:` (obsoleto), PostgreSQL 9.4.5 → 17-alpine,
  `depends_on` con `condition: service_healthy` (antes el backend arrancaba antes que la BD),
  volumen persistente y credenciales por variables de entorno.

---

## 5. Verificación realizada

| Comprobación | Comando | Resultado |
|---|---|---|
| Compilación backend | `./mvnw clean compile` | OK |
| Pruebas backend | `./mvnw test` | 37/37 OK |
| Empaquetado backend | `./mvnw clean package` | `shop-api-0.0.1-SNAPSHOT.jar` |
| Instalación frontend | `npm install` | 646 paquetes |
| Compilación frontend | `npm run build` | OK (165 kB transferidos) |
| Pruebas frontend | `npm run test:ci` | 2/2 OK |
| Lint frontend | `npm run lint` | 0 errores / 154 avisos |

Además, sobre una copia desechable del backend con H2 en memoria (para no añadir
dependencias al proyecto entregado) se verificó que la aplicación **arranca y funciona**:

| Comprobación | Resultado |
|---|---|
| El contexto de Spring levanta completo (seguridad, JPA, filtro JWT) | OK |
| `import.sql` carga los datos | 4 usuarios y 12 productos |
| `POST /register` (BCrypt + creación de carrito) | OK |
| `POST /login` devuelve un JWT válido | OK |
| Endpoint protegido con token | 200 |
| Mismo endpoint sin token | 401 |
| Login de las 3 cuentas de ejemplo con sus roles correctos | OK |
| `DELETE /seller/product/{id}/delete` con CUSTOMER y con EMPLOYEE | 4xx (denegado) |
| `DELETE /seller/product/{id}/delete` con MANAGER | 2xx (permitido) |
| `GET /cart` con CUSTOMER / con MANAGER | 2xx / 4xx |
| `GET /product` (catálogo público) | 200 |

Estos hallazgos surgieron precisamente de esa verificación: el patrón `**` inválido,
la clave JWT corta y el `import.sql` posicional **no se detectan compilando**, solo
al arrancar la aplicación.

**No verificado** (requiere Docker en la máquina): `docker compose up --build` y el
frontend servido por nginx contra el backend real. Es lo único que queda por probar.

## 6. Decisiones y deuda pendiente

1. **Angular 21, no 22.** Angular 22 exige Node `^22.22.3 || ^24.15.0 || >=26`; la máquina
   de desarrollo tiene Node 24.11.1. Con Angular 21 todo compila y corre. Subir a 22 es
   un `ng update @angular/core@22 @angular/cli@22` una vez actualizado Node.
2. **Bootstrap se queda en 4.6** para no alterar la interfaz (ver 3.1).
3. **Sin E2E.** Protractor está descontinuado y se eliminó. Cypress o Playwright son el
   reemplazo natural, pero es *añadir*, no *actualizar*.
4. **`ddl-auto: create`** se mantiene como en el original: la base se recrea en cada
   arranque. Para un entorno real habría que pasar a Flyway o Liquibase — y eso además
   eliminaría de raíz la fragilidad de `import.sql` descrita en 2.8.
5. **Secreto JWT por defecto en el repositorio.** Se dejó un valor de desarrollo
   sobreescribible por `JWT_SECRET`; no debe usarse en producción.
6. **`strict: false` en TypeScript** y reglas de ESLint en modo aviso: es deuda técnica
   consciente, y es exactamente el material de la práctica de calidad de código.

---

## 7. Cuentas de ejemplo

Vienen en `import.sql`. La contraseña **no estaba documentada** en el proyecto original;
se recuperó verificando el hash BCrypt contra candidatos comunes.

| Correo | Contraseña | Rol |
|---|---|---|
| `customer1@email.com` | `123` | CUSTOMER |
| `customer2@email.com` | `123` | CUSTOMER |
| `employee1@email.com` | `123` | EMPLOYEE |
| `manager1@email.com` | `123` | MANAGER |

Son credenciales de demostración: no deben sobrevivir a un despliegue real.
