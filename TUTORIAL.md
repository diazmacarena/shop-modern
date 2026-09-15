# Tutorial de construcción automática

**Curso:** Ingeniería de Software II · **Práctica 03 — Construcción Automática**
**Proyecto:** Online Shopping Store (Spring Boot + Angular)

Este tutorial cubre los tres puntos que pide el entregable: **la herramienta**, el
**paso a paso de instalación y configuración**, y el **paso a paso de construcción del
proyecto**.

---

## Índice

1. [Herramienta elegida](#1-herramienta-elegida)
2. [Instalación paso a paso](#2-instalación-paso-a-paso)
3. [Configuración de la herramienta](#3-configuración-de-la-herramienta)
4. [El modelo de construcción](#4-el-modelo-de-construcción)
5. [Construcción paso a paso](#5-construcción-paso-a-paso)
6. [Visualización de resultados](#6-visualización-de-resultados)
7. [Ciclo de vida de Maven](#7-ciclo-de-vida-de-maven)
8. [Problemas frecuentes](#8-problemas-frecuentes)

---

## 1. Herramienta elegida

El proyecto es **full-stack**, así que usa dos herramientas de construcción automática,
una por cada tecnología:

| | Backend | Frontend |
|---|---|---|
| **Herramienta** | **Apache Maven 3.9.16** | **npm 10 + Angular CLI 21** |
| **Lenguaje** | Java 21 | TypeScript 5.9 |
| **Modelo de construcción** | `backend/pom.xml` (XML) | `frontend/package.json` + `frontend/angular.json` (JSON) |
| **Bloqueo de versiones** | `pom.xml` (versiones explícitas) | `package-lock.json` |
| **Repositorio de librerías** | Maven Central | npm registry |
| **Artefacto producido** | `shop-api-0.0.1-SNAPSHOT.jar` | `dist/shop/browser/` |

### ¿Por qué Maven y no Gradle?

| Criterio | Maven | Gradle |
|---|---|---|
| Modelo | declarativo (XML) | imperativo (Groovy/Kotlin DSL) |
| Curva de aprendizaje | baja: el ciclo de vida es fijo y estándar | mayor: hay que aprender el DSL |
| Integración con Spring Boot | oficial, `spring-boot-starter-parent` gestiona ~200 versiones | también soportada |
| Reproducibilidad | alta: mismo `pom.xml` → mismo resultado | alta, pero el DSL permite lógica que rompe la reproducibilidad |
| Proyecto original | **ya usaba Maven** | — |

Se mantuvo Maven porque el proyecto base ya lo usaba y porque su modelo declarativo es
el que mejor ilustra el concepto de "modelo del sistema" que pide la práctica: el
`pom.xml` **describe** qué construir, no **cómo** hacerlo paso a paso.

### ¿Por qué npm y no Yarn o pnpm?

npm viene incluido con Node.js (cero instalación adicional), es el gestor que el Angular
CLI asume por defecto, y `npm ci` garantiza construcciones reproducibles desde el
lockfile. Yarn o pnpm serían válidos, pero añaden una dependencia sin aportar nada aquí.

---

## 2. Instalación paso a paso

### 2.1 JDK 21

Maven necesita un JDK, no solo un JRE.

1. Descargar **Eclipse Temurin 21 (LTS)** desde <https://adoptium.net/temurin/releases/?version=21>
   — elegir *Windows / x64 / JDK / .msi*.
2. Ejecutar el instalador. En la pantalla de componentes, activar **"Set JAVA_HOME variable"**
   (viene desactivado por defecto y ahorra el paso 4).
3. Abrir una **terminal nueva** y verificar:

```bash
java -version
```

Salida esperada (la versión menor puede variar):

```
openjdk version "21.0.12" 2024-07-16 LTS
OpenJDK Runtime Environment Temurin-21.0.12+7 (build 21.0.12+7-LTS)
```

4. Si no marcaste la casilla del paso 2, define `JAVA_HOME` a mano (PowerShell, una sola vez):

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-21.0.12.1", "User")
```

Cierra y abre la terminal, y comprueba:

```powershell
$env:JAVA_HOME
```

> **Trampa habitual:** `where java` en Windows suele devolver
> `C:\Program Files\Common Files\Oracle\Java\javapath\java.exe`. Esa carpeta **no** sirve
> como `JAVA_HOME`: es solo el lanzador que Oracle pone en el PATH. `JAVA_HOME` debe
> apuntar a la raíz real del JDK, la que contiene `bin\` y `lib\`.

### 2.2 Maven — no hay que instalarlo

El proyecto incluye el **Maven Wrapper**, que descarga y usa la versión correcta de Maven
automáticamente. Esto es lo que se recomienda hoy: garantiza que **todo el equipo y el
servidor de CI construyen con la misma versión de Maven**, sin instalaciones manuales.

Archivos del wrapper en el repositorio:

```
backend/
├── mvnw                                  ← script para Linux/macOS/Git Bash
├── mvnw.cmd                              ← script para Windows
└── .mvn/wrapper/maven-wrapper.properties ← declara qué Maven usar
```

Contenido de `maven-wrapper.properties`:

```properties
wrapperVersion=3.3.4
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip
```

La primera ejecución de `./mvnw` descarga Maven 3.9.16 a `~/.m2/wrapper/` y lo reutiliza
después. Verificar:

```bash
cd backend
./mvnw -version
```

> Si prefieres instalar Maven de forma global (no es necesario para este proyecto):
> descargar el binario de <https://maven.apache.org/download.cgi>, descomprimir en
> `C:\Program Files\Apache\maven`, y añadir `...\maven\bin` al PATH.

### 2.3 Node.js 22 LTS

1. Descargar el instalador **LTS** de <https://nodejs.org/> (*Windows Installer .msi*).
2. Instalar con las opciones por defecto (npm viene incluido).
3. En una terminal nueva:

```bash
node --version && npm --version
```

Angular 21 requiere Node `^20.19.0 || ^22.12.0 || >=24.0.0`.

### 2.4 Angular CLI — tampoco hay que instalarlo

El CLI se instala como **dependencia de desarrollo** del proyecto (`@angular/cli` en
`package.json`) y se invoca con `npx ng` o mediante los scripts de npm. No hace falta
`npm install -g @angular/cli`; de hecho es preferible evitarlo, porque una versión global
distinta a la del proyecto genera conflictos.

### 2.5 Resumen de verificación

```bash
java -version && node --version && npm --version && cd backend && ./mvnw -version
```

---

## 3. Configuración de la herramienta

### 3.1 Desde la terminal

No requiere configuración adicional: el wrapper y `package.json` traen todo declarado.

### 3.2 Desde IntelliJ IDEA

1. *File → Open* y seleccionar la carpeta `backend`. IntelliJ detecta el `pom.xml` e
   importa el proyecto automáticamente.
2. *File → Project Structure → Project SDK* → seleccionar **21**.
3. *Settings → Build, Execution, Deployment → Build Tools → Maven* → marcar
   **"Use Maven wrapper"**.
4. La ventana **Maven** (lateral derecho) lista todas las metas del ciclo de vida:
   `clean`, `validate`, `compile`, `test`, `package`, `install`. Doble clic ejecuta.

### 3.3 Desde VS Code

1. Instalar las extensiones **Extension Pack for Java** y **Angular Language Service**.
2. Abrir la carpeta raíz del proyecto.
3. La vista **Maven** aparece en el explorador; desde ahí se ejecutan las metas.
4. Los scripts de npm aparecen en la vista **NPM Scripts**.

---

## 4. El modelo de construcción

La práctica pide que el modelo describa tres cosas. Aquí está dónde vive cada una.

### 4.1 Información del proyecto

**`backend/pom.xml`**

```xml
<groupId>me.zhulin</groupId>
<artifactId>shop-api</artifactId>
<version>0.0.1-SNAPSHOT</version>
<name>shop-api</name>
<description>Online Shopping API Server</description>
```

El tipo de artefacto es `jar` (valor por defecto cuando no se declara `<packaging>`).
El nombre del archivo resultante se forma como `artifactId-version.jar` →
`shop-api-0.0.1-SNAPSHOT.jar`.

**`frontend/package.json`**

```json
{
  "name": "shop",
  "version": "0.0.0",
  "private": true
}
```

### 4.2 Opciones de compilación

**`backend/pom.xml`**

```xml
<properties>
    <java.version>21</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <jjwt.version>0.13.0</jjwt.version>
</properties>
```

`java.version` es una propiedad que `spring-boot-starter-parent` traduce a
`maven.compiler.source` y `maven.compiler.target`, es decir: **compilador `javac`,
nivel de lenguaje 21**.

Además se configura el procesador de anotaciones de Lombok y el agente de Mockito:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-javaagent:${org.mockito:mockito-core:jar} -Xshare:off</argLine>
    </configuration>
</plugin>
```

**`frontend/tsconfig.json`** (opciones del compilador TypeScript)

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ES2022",
    "moduleResolution": "bundler",
    "experimentalDecorators": true
  }
}
```

**`frontend/angular.json`** (opciones del empaquetador)

```json
"configurations": {
  "production": {
    "optimization": true,
    "outputHashing": "all",
    "sourceMap": false,
    "extractLicenses": true,
    "budgets": [{ "type": "initial", "maximumWarning": "2mb", "maximumError": "5mb" }]
  }
}
```

### 4.3 Gestión de dependencias

**`backend/pom.xml`** declara 9 dependencias directas. Maven resuelve las transitivas
solo: el árbol completo son **119 líneas** (ver `docs/evidencia/05-backend-dependency-tree.txt`).

| Dependencia | Para qué | Versión |
|---|---|---|
| `spring-boot-starter-data-jpa` | persistencia (Hibernate) | heredada del parent |
| `spring-boot-starter-web` | REST y Tomcat embebido | heredada |
| `spring-boot-starter-security` | autenticación y autorización | heredada |
| `spring-boot-starter-validation` | Bean Validation | heredada |
| `postgresql` | driver JDBC | heredada |
| `lombok` | generación de getters/setters | heredada |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | tokens JWT | `0.13.0` |
| `spring-boot-starter-test` | JUnit 5 + Mockito | heredada |
| `spring-security-test` | utilidades de prueba de seguridad | heredada |

El bloque clave es el **parent**, que centraliza las versiones de todo el ecosistema
Spring:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
</parent>
```

Gracias a él, la mayoría de dependencias se declaran **sin `<version>`**: la hereda del
parent, lo que evita conflictos de versiones entre librerías del mismo ecosistema.

Para inspeccionar el árbol de dependencias:

```bash
cd backend
./mvnw dependency:tree
```

**`frontend/package.json`** separa dependencias de ejecución y de desarrollo:

```json
"dependencies":    { "@angular/core": "^21.2.0", "rxjs": "~7.8.2", "bootstrap": "^4.6.2", ... },
"devDependencies": { "@angular/cli": "^21.2.0", "typescript": "~5.9.3", "karma": "~6.4.4", ... }
```

`package-lock.json` fija las versiones exactas de las 646 librerías resueltas, incluidas
las transitivas.

---

## 5. Construcción paso a paso

> Los comandos usan `./mvnw` (Git Bash / Linux / macOS). En PowerShell o CMD es
> `.\mvnw.cmd` y requiere `JAVA_HOME` definido (ver §2.1).

### Paso 1 — clonar el repositorio

```bash
git clone <URL-DEL-REPOSITORIO>
cd shop-modern
```

### Paso 2 — construir el backend

```bash
cd backend
./mvnw clean package
```

Esto ejecuta, en orden: `clean` → `validate` → `compile` → `test` → `package`.

**Resultado esperado** (log completo en `docs/evidencia/01-backend-mvnw-clean-package.log`):

```
[INFO] --- compiler:3.15.0:compile (default-compile) @ shop-api ---
[INFO] Compiling 43 source files with javac [debug parameters release 21] to target\classes

[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running me.zhulin.shopapi.service.impl.ProductServiceImplTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] Results:
[INFO] Tests run: 37, Failures: 0, Errors: 0, Skipped: 0

[INFO] --- jar:3.5.1:jar (default-jar) @ shop-api ---
[INFO] Building jar: C:\dev\shop-modern\backend\target\shop-api-0.0.1-SNAPSHOT.jar
[INFO] --- spring-boot:4.1.1:repackage (repackage) @ shop-api ---
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  25.731 s
```

### Paso 3 — construir el frontend

```bash
cd ../frontend
npm ci
npm run build
```

`npm ci` instala **exactamente** las versiones del lockfile. Es la orden correcta para
construcción reproducible; `npm install` puede modificar el lockfile.

**Resultado esperado** (log en `docs/evidencia/02-frontend-npm-run-build.log`):

```
Initial chunk files   | Names     |  Raw size | Estimated transfer size
main-ELFXIESL.js      | main      | 377.03 kB |                94.22 kB
styles-RTVVFTOU.css   | styles    | 160.86 kB |                17.63 kB
scripts-UONR3CDT.js   | scripts   | 149.97 kB |                41.82 kB
polyfills-5CFQRCPP.js | polyfills |  34.59 kB |                11.33 kB

                      | Initial total | 722.45 kB |              165.00 kB

Application bundle generation complete. [22.973 seconds]
Output location: C:\dev\shop-modern\frontend\dist\shop
```

### Paso 4 — ejecutar las pruebas del frontend

```bash
npm run test:ci
```

```
Chrome Headless 152.0.0.0 (Windows 10): Executed 2 of 2 SUCCESS
TOTAL: 2 SUCCESS
```

### Paso 5 — análisis estático

```bash
npm run lint
```

```
✖ 154 problems (0 errors, 154 warnings)
```

**0 errores**, así que la construcción no se rompe. Los 154 avisos son reglas nuevas de
ESLint sobre código heredado de Angular 7 (`*ngIf` → `@if`, inyección por constructor →
`inject()`, `any` sin tipar); están declarados como avisos a propósito y documentados
como deuda técnica en `MIGRATION.md` §3.6.

### Construcción completa en un solo comando

```bash
cd backend && ./mvnw clean package && cd ../frontend && npm ci && npm run build && npm run test:ci && npm run lint
```

---

## 6. Visualización de resultados

### 6.1 Artefactos generados

```bash
ls -lh backend/target/*.jar
ls frontend/dist/shop/browser/
```

| Artefacto | Ruta | Tamaño |
|---|---|---|
| Backend | `backend/target/shop-api-0.0.1-SNAPSHOT.jar` | ~58 MB |
| Frontend | `frontend/dist/shop/browser/` | 722 kB (165 kB transferidos) |

El jar es **ejecutable y autocontenido**: incluye Tomcat y todas las dependencias.
Comprobarlo:

```bash
unzip -p backend/target/shop-api-0.0.1-SNAPSHOT.jar META-INF/MANIFEST.MF
```

```
Main-Class: org.springframework.boot.loader.launch.JarLauncher
Start-Class: me.zhulin.shopapi.ShopApiApplication
```

### 6.2 Informe de pruebas

Surefire genera un informe por clase en `backend/target/surefire-reports/`:

```bash
ls backend/target/surefire-reports/
cat backend/target/surefire-reports/me.zhulin.shopapi.service.impl.ProductServiceImplTest.txt
```

Hay dos formatos: `.txt` (legible) y `.xml` (el que consumen los servidores de CI).

### 6.3 Evidencia guardada en el repositorio

Todos los logs de esta ejecución están versionados en `docs/evidencia/`:

| Archivo | Contenido |
|---|---|
| `01-backend-mvnw-clean-package.log` | construcción completa del backend, 37 pruebas |
| `02-frontend-npm-run-build.log` | construcción del frontend con tamaños de bundle |
| `03-frontend-npm-test.log` | pruebas del frontend en Chrome headless |
| `04-frontend-npm-lint.log` | análisis estático (0 errores) |
| `05-backend-dependency-tree.txt` | árbol completo de dependencias resueltas |

---

## 7. Ciclo de vida de Maven

Maven no ejecuta "comandos sueltos": ejecuta **fases de un ciclo de vida**, y cada fase
dispara todas las anteriores. Esto es el corazón de la construcción automática.

```
validate → compile → test → package → verify → install → deploy
```

| Fase | Qué hace | Comando |
|---|---|---|
| `validate` | valida el `pom.xml` | `./mvnw validate` |
| `compile` | compila `src/main/java` a `target/classes` | `./mvnw compile` |
| `test` | compila y ejecuta `src/test/java` | `./mvnw test` |
| `package` | empaqueta el `.jar` en `target/` | `./mvnw package` |
| `verify` | ejecuta pruebas de integración | `./mvnw verify` |
| `install` | copia el `.jar` al repositorio local `~/.m2` | `./mvnw install` |
| `deploy` | publica en un repositorio remoto | `./mvnw deploy` |

`clean` es un ciclo aparte que borra `target/`; por eso se suele escribir
`./mvnw clean package`.

**Consecuencia práctica:** `./mvnw package` **siempre** ejecuta las pruebas antes de
empaquetar. Si una prueba falla, no se genera el artefacto. Para saltarlas
deliberadamente:

```bash
./mvnw package -DskipTests
```

### Equivalencias en npm

npm no tiene ciclo de vida fijo; los scripts se declaran en `package.json`:

| Script | Comando | Equivalente en Maven |
|---|---|---|
| `build` | `npm run build` | `package` |
| `test:ci` | `npm run test:ci` | `test` |
| `lint` | `npm run lint` | — |
| `start` | `npm start` | `spring-boot:run` |

---

## 8. Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `Error: JAVA_HOME not found in your environment` | `mvnw.cmd` sin `JAVA_HOME` | definirla (§2.1); `./mvnw` en Git Bash no la exige |
| `JAVA_HOME is set to an invalid directory` | apunta a `Common Files\Oracle\Java` | apuntar a la raíz real del JDK |
| `class file has wrong version 65.0` | JDK menor que 21 | `java -version`, instalar JDK 21 |
| La primera construcción tarda muchísimo | descarga de Maven y de todas las dependencias | normal, solo la primera vez; luego usa el caché `~/.m2` |
| `npm ci` falla con `EBADENGINE` | Node demasiado antiguo | Node 20.19+, 22.12+ o 24+ |
| `npm run test:ci` no encuentra navegador | Chrome no instalado | instalar Chrome |
| `Cannot connect to localhost:5432` | intentaste **ejecutar**, no construir | la construcción no usa base de datos; ver `BUILD.md` §6 |
| El build falla en otra máquina pero no en la tuya | `npm install` cambió el lockfile | usar siempre `npm ci` |

---

## Documentos relacionados

| Documento | Contenido |
|---|---|
| [`BUILD.md`](./BUILD.md) | guía de construcción sin base de datos |
| [`DOCKER.md`](./DOCKER.md) | ejecución con Docker y Docker Compose |
| [`MIGRATION.md`](./MIGRATION.md) | migración Java 11→21 y Angular 7→21 |
| [`ENTREGA-PRACTICA-03.md`](./ENTREGA-PRACTICA-03.md) | mapeo de requisitos de la práctica a evidencia |
