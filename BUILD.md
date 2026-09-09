# Guía de construcción (sin base de datos)

Cómo compilar, probar y empaquetar el proyecto completo **sin PostgreSQL, sin Docker y
sin ningún servicio externo**.

Esto funciona porque la construcción no toca la base de datos en ningún momento:

- Maven compila, ejecuta las pruebas y empaqueta el `.jar`. Las 37 pruebas del backend
  usan **Mockito**, no una base real.
- Angular compila y ejecuta sus pruebas en un navegador headless.

La base de datos **solo** hace falta para *ejecutar* la aplicación (`spring-boot:run`,
`docker compose up`). Ver la sección [Qué sí necesita base de datos](#qué-sí-necesita-base-de-datos).

---

## 1. Requisitos

| Herramienta | Versión mínima | Para qué |
|---|---|---|
| **JDK** | 21 o superior | backend |
| **Node.js** | 20.19+, 22.12+ o 24+ | frontend |
| **npm** | 10+ | frontend |
| **Google Chrome** | cualquiera reciente | solo para `npm run test:ci` |

No hace falta instalar Maven ni Angular CLI: el proyecto trae **Maven Wrapper**
(`mvnw`) y el CLI de Angular se instala como dependencia con `npm ci`.

Comprobar lo instalado:

```bash
java -version && node --version && npm --version
```

---

## 2. Backend (Spring Boot)

### Linux / macOS / Git Bash

```bash
cd backend
./mvnw clean package
```

### Windows PowerShell o CMD

`mvnw.cmd` **exige la variable `JAVA_HOME`** (a diferencia de `./mvnw`, que se conforma
con el `java` del PATH). Si no está definida verás:

```
Error: JAVA_HOME not found in your environment.
```

En esta máquina el JDK 21 está en `C:\Program Files\Java\jdk-21.0.12.1`. Para la sesión actual:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"
cd backend
.\mvnw.cmd clean package
```

Para dejarlo permanente (una sola vez, abrir una terminal nueva después):

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-21.0.12.1", "User")
```

> Ojo: `C:\Program Files\Common Files\Oracle\Java` **no** es un JAVA_HOME válido; es solo
> la carpeta de los lanzadores que Oracle pone en el PATH. Hay que apuntar a la raíz real
> del JDK (la que contiene `bin\` y `lib\`).

### Qué hace y qué produce

`clean package` encadena: `clean` → `compile` → `test` → `package`.

```
[INFO] Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building jar: .../backend/target/shop-api-0.0.1-SNAPSHOT.jar
[INFO] BUILD SUCCESS
```

Artefacto: **`backend/target/shop-api-0.0.1-SNAPSHOT.jar`** (~58 MB, jar ejecutable con
todas las dependencias dentro).

### Metas por separado

| Meta | Comando | Qué hace |
|---|---|---|
| Limpiar | `./mvnw clean` | borra `target/` |
| Compilar | `./mvnw compile` | solo `src/main/java` |
| Probar | `./mvnw test` | compila y ejecuta las 37 pruebas |
| Empaquetar | `./mvnw package` | genera el `.jar` |
| Empaquetar sin probar | `./mvnw package -DskipTests` | más rápido, se salta las pruebas |
| Informe de pruebas | — | queda en `backend/target/surefire-reports/` |

> La **primera** ejecución descarga Maven 3.9.16 y todas las dependencias (varios minutos,
> requiere internet). A partir de ahí todo sale del caché local `~/.m2` y funciona sin red
> añadiendo `-o` (modo offline).

---

## 3. Frontend (Angular)

```bash
cd frontend
npm ci
npm run build
```

`npm ci` instala **exactamente** las versiones de `package-lock.json` — es la orden
correcta para una construcción reproducible. (`npm install` puede actualizar el lockfile;
úsala solo cuando cambies dependencias a propósito.)

```
Initial total | 722.45 kB | 165.00 kB
Application bundle generation complete.
Output location: frontend/dist/shop
```

Artefacto: **`frontend/dist/shop/browser/`** — los archivos estáticos que sirve nginx.

> Fíjate en el subdirectorio `browser/`: el builder `@angular/build:application` de
> Angular 21 lo añade. En Angular 7 la salida quedaba directamente en `dist/shop/`.

### Scripts disponibles

| Script | Comando | Qué hace |
|---|---|---|
| Compilar (producción) | `npm run build` | bundle optimizado en `dist/shop/browser/` |
| Compilar (desarrollo) | `npm run watch` | recompila al guardar |
| Pruebas | `npm run test:ci` | 2 pruebas en Chrome headless, termina solo |
| Pruebas interactivas | `npm test` | abre Chrome y queda observando |
| Análisis estático | `npm run lint` | ESLint sobre `.ts` y `.html` |
| Servidor de desarrollo | `npm start` | `http://localhost:4200` |

Resultados esperados:

```
TOTAL: 2 SUCCESS
✖ 154 problems (0 errors, 154 warnings)
```

Los 154 avisos son **intencionales**: reglas nuevas de ESLint sobre el código heredado
(`*ngIf` → `@if`, inyección por constructor → `inject()`, `any` sin tipar). Están en modo
aviso para no bloquear la construcción; el detalle está en `MIGRATION.md` §3.6.
**`ng lint` termina con 0 errores, así que no rompe el pipeline.**

---

## 4. Construir todo de una vez

### Git Bash / Linux / macOS

```bash
cd backend && ./mvnw clean package && cd ../frontend && npm ci && npm run build && npm run test:ci && npm run lint
```

### PowerShell

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"; cd backend; .\mvnw.cmd clean package; if ($?) { cd ..\frontend; npm ci; npm run build; npm run test:ci; npm run lint }
```

Al terminar, los dos artefactos son:

```
backend/target/shop-api-0.0.1-SNAPSHOT.jar
frontend/dist/shop/browser/
```

---

## 5. Verificar que la construcción quedó bien

Sin base de datos no puedes *arrancar* el backend, pero sí comprobar que el artefacto
quedó bien formado. `java -jar` **no** sirve para esto: intenta levantar la aplicación y
falla al conectarse a PostgreSQL.

Comprobar que el jar es ejecutable y trae las clases y dependencias:

```bash
unzip -l backend/target/shop-api-0.0.1-SNAPSHOT.jar | grep -E "ShopApiApplication.class|BOOT-INF/lib/spring-boot-" | head
```

(Se usa `unzip` y no `jar` porque el `bin/` del JDK no suele estar en el PATH de Git Bash;
un `.jar` es un ZIP, así que `unzip` sirve igual.)

Debe aparecer `BOOT-INF/classes/me/zhulin/shopapi/ShopApiApplication.class` junto con los
jars de Spring Boot dentro de `BOOT-INF/lib/`.

Comprobar la clase principal declarada:

```bash
unzip -p backend/target/shop-api-0.0.1-SNAPSHOT.jar META-INF/MANIFEST.MF
```

Para el frontend, basta con que exista el punto de entrada:

```bash
ls frontend/dist/shop/browser/index.html
```

Y para revisar el resultado en el navegador sin backend (el catálogo saldrá vacío porque
no hay API, pero confirma que el bundle carga):

```bash
npx --yes http-server frontend/dist/shop/browser -p 4300
```

---

## 6. Qué sí necesita base de datos

Lo siguiente **no** es parte de la construcción y sí requiere PostgreSQL en marcha:

| Acción | Comando |
|---|---|
| Arrancar el backend | `./mvnw spring-boot:run` |
| Ejecutar el jar | `java -jar target/shop-api-0.0.1-SNAPSHOT.jar` |
| Levantar todo el stack | `docker compose up --build` |

Si arrancas el backend sin base de datos, falla al crear el `DataSource` con un error de
conexión a `localhost:5432`. Es esperado y **no significa que la construcción esté mal**.

La forma más rápida de tener la base es Docker:

```bash
docker run --name shop-db -e POSTGRES_PASSWORD=root -p 5432:5432 -d postgres:17-alpine
```

Con eso, `./mvnw spring-boot:run` levanta la API en `http://localhost:8080/api`, crea el
esquema y carga los datos de ejemplo desde `import.sql`.

---

## 7. Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `Error: JAVA_HOME not found in your environment` | `mvnw.cmd` sin `JAVA_HOME` | ver §2 |
| `JAVA_HOME is set to an invalid directory` | apunta a `Common Files\Oracle\Java` | apuntar a la raíz real del JDK |
| `class file has wrong version` | JDK menor que 21 | `java -version` y usar JDK 21+ |
| `npm ci` falla con `EBADENGINE` o similar | Node demasiado viejo | Node 20.19+, 22.12+ o 24+ |
| `npm run test:ci` no encuentra navegador | Chrome no instalado | instalar Chrome, o `npm run build` sin pruebas |
| `Cannot connect to localhost:5432` | intentaste **ejecutar**, no construir | ver §6 |
| Descargas lentísimas la primera vez | caché vacío | normal; solo la primera vez |

---

## 8. Resumen para la Práctica 03

| Elemento | Backend | Frontend |
|---|---|---|
| Lenguaje | Java 21 | TypeScript 5.9 |
| Gestor de dependencias | Maven 3.9 (wrapper) | npm 10 |
| Archivo de construcción | `backend/pom.xml` | `frontend/package.json` |
| Bloqueo de versiones | `pom.xml` (versiones fijas) | `package-lock.json` |
| Compilar | `./mvnw compile` | `npm run build` |
| Probar | `./mvnw test` (37) | `npm run test:ci` (2) |
| Análisis estático | — *(pendiente: SonarQube)* | `npm run lint` |
| Empaquetar | `./mvnw package` | `npm run build` |
| Artefacto | `shop-api-0.0.1-SNAPSHOT.jar` | `dist/shop/browser/` |
| ¿Necesita base de datos? | **No** | **No** |
