# Entrega — Práctica 03: Construcción Automática

**Curso:** Ingeniería de Software II
**Docente:** DSc. Edgar Sarmiento Calisaya
**Carrera:** Ciencia de la Computación

**Proyecto:** Online Shopping Store — e-commerce full-stack (Spring Boot + Angular)
**Repositorio base:** <https://github.com/zhulinn/SpringBoot-Angular7-Online-Shopping-Store>

**Integrantes:**

| Nombre | Rol |
|---|---|
| _(completar)_ | |
| _(completar)_ | |
| _(completar)_ | |

---

## 1. Resumen ejecutivo

Se tomó un e-commerce real (Spring Boot + Angular) y se configuró su **construcción
automática** con **Maven** (backend) y **npm + Angular CLI** (frontend).

Como el proyecto base estaba tecnológicamente detenido en 2019 (Java 11, Angular 7, y un
`pom.xml` que apuntaba a una *build snapshot* de Spring Boot que ya no existe en los
repositorios), **no era construible tal cual**. El primer trabajo fue actualizarlo hasta
un estado que compila y se prueba de forma reproducible; ese trabajo está documentado en
[`MIGRATION.md`](./MIGRATION.md) y sirve como línea base para las siguientes prácticas.

**Estado actual:** ambos módulos se construyen con un solo comando, con 39 pruebas
automatizadas en verde y análisis estático sin errores.

---

## 2. Objetivo de la práctica

> *"Construir un proyecto de software vía un modelo de sistema creado a través de una
> herramienta de construcción automática."*

**Cumplido.** El proyecto se construye íntegramente a partir de dos modelos declarativos
(`backend/pom.xml` y `frontend/package.json` + `angular.json`), sin pasos manuales.

---

## 3. Actividades (§6 de la práctica)

### 3.1 Configurar la herramienta de construcción automática

| Herramienta | Versión | Cómo se configuró |
|---|---|---|
| **Apache Maven** | 3.9.16 | vía **Maven Wrapper** (`mvnw`), incluido en el repositorio |
| **npm** | 10.x | incluido con Node.js 22 LTS |
| **Angular CLI** | 21.2 | dependencia de desarrollo del proyecto, no instalación global |

Se eligió el **Maven Wrapper** en lugar de una instalación global: el archivo
`backend/.mvn/wrapper/maven-wrapper.properties` declara qué versión de Maven usar, así que
todo el equipo —y un eventual servidor de integración continua— construye con exactamente
la misma versión, sin que nadie tenga que instalar nada.

> Hallazgo: el repositorio original **declaraba `mvnw` pero no incluía la carpeta
> `.mvn/wrapper/`**, así que el wrapper no funcionaba. Se reinstaló (wrapper 3.3.4 →
> Maven 3.9.16).

📄 Detalle completo: [`TUTORIAL.md` §2 y §3](./TUTORIAL.md)

---

### 3.2 Crear el modelo de construcción

La práctica pide que el modelo describa tres bloques de información. Dónde está cada uno:

#### a) Información del proyecto

| Dato | Backend (`pom.xml`) | Frontend (`package.json`) |
|---|---|---|
| Nombre | `<artifactId>shop-api</artifactId>` | `"name": "shop"` |
| Grupo | `<groupId>me.zhulin</groupId>` | — |
| Versión | `<version>0.0.1-SNAPSHOT</version>` | `"version": "0.0.0"` |
| Descripción | `<description>Online Shopping API Server</description>` | — |
| Tipo de archivo | `jar` (por defecto) | bundle estático |

#### b) Opciones de compilación

| Opción | Backend | Frontend |
|---|---|---|
| Compilador | `javac` (JDK 21) | `tsc` 5.9 + esbuild |
| Nivel de lenguaje | `<java.version>21</java.version>` | `"target": "ES2022"` |
| Codificación | `<project.build.sourceEncoding>UTF-8</...>` | UTF-8 |
| Procesadores de anotaciones | Lombok (`annotationProcessorPaths`) | — |
| Optimización | — | `"optimization": true` (perfil `production`) |
| Límites de tamaño | — | `budgets`: aviso 2 MB, error 5 MB |

#### c) Gestión de dependencias

| | Backend | Frontend |
|---|---|---|
| Dependencias directas | 9 | 13 runtime + 14 desarrollo |
| Dependencias resueltas | árbol de 119 líneas | 646 paquetes |
| Repositorio | Maven Central | npm registry |
| Bloqueo de versiones | `spring-boot-starter-parent` 4.1.1 | `package-lock.json` |

El `<parent>` de Spring Boot centraliza las versiones de todo el ecosistema, por eso la
mayoría de dependencias se declaran **sin `<version>`**: se evita así el "infierno de
versiones" entre librerías del mismo stack.

📄 Modelos completos: [`backend/pom.xml`](./backend/pom.xml) · [`frontend/package.json`](./frontend/package.json) · [`frontend/angular.json`](./frontend/angular.json)
📄 Explicación detallada: [`TUTORIAL.md` §4](./TUTORIAL.md)

---

### 3.3 Construir el proyecto por medio del modelo

Comando único para toda la construcción:

```bash
cd backend && ./mvnw clean package && cd ../frontend && npm ci && npm run build && npm run test:ci && npm run lint
```

| Fase | Backend | Frontend |
|---|---|---|
| Limpieza | `./mvnw clean` | — |
| Resolución de dependencias | automática desde `pom.xml` | `npm ci` |
| Compilación | `./mvnw compile` (43 archivos) | `npm run build` |
| Pruebas | `./mvnw test` (**37**) | `npm run test:ci` (**2**) |
| Análisis estático | — | `npm run lint` (**0 errores**) |
| Empaquetado | `./mvnw package` | `npm run build` |

📄 Guía paso a paso: [`TUTORIAL.md` §5](./TUTORIAL.md) · [`BUILD.md`](./BUILD.md)

---

### 3.4 Visualizar resultados de construcción

#### Backend

```
[INFO] Compiling 43 source files with javac [debug parameters release 21] to target\classes
[INFO] Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building jar: backend\target\shop-api-0.0.1-SNAPSHOT.jar
[INFO] BUILD SUCCESS
[INFO] Total time:  25.731 s
```

#### Frontend

```
Initial chunk files   | Names     |  Raw size | Estimated transfer size
main-ELFXIESL.js      | main      | 377.03 kB |                94.22 kB
styles-RTVVFTOU.css   | styles    | 160.86 kB |                17.63 kB
scripts-UONR3CDT.js   | scripts   | 149.97 kB |                41.82 kB
polyfills-5CFQRCPP.js | polyfills |  34.59 kB |                11.33 kB
                      | Initial total | 722.45 kB |            165.00 kB

Application bundle generation complete. [22.973 seconds]
```

```
Chrome Headless 152.0.0.0 (Windows 10): Executed 2 of 2 SUCCESS
TOTAL: 2 SUCCESS

✖ 154 problems (0 errors, 154 warnings)
```

#### Artefactos generados

| Artefacto | Ruta | Tamaño |
|---|---|---|
| Backend (jar ejecutable) | `backend/target/shop-api-0.0.1-SNAPSHOT.jar` | ~58 MB |
| Frontend (bundle estático) | `frontend/dist/shop/browser/` | 722 kB / 165 kB transferidos |

#### Evidencia versionada

Los logs reales de esta ejecución están en [`docs/evidencia/`](./docs/evidencia):

| Archivo | Contenido |
|---|---|
| `01-backend-mvnw-clean-package.log` | construcción completa del backend |
| `02-frontend-npm-run-build.log` | construcción del frontend |
| `03-frontend-npm-test.log` | pruebas del frontend |
| `04-frontend-npm-lint.log` | análisis estático |
| `05-backend-dependency-tree.txt` | árbol de dependencias resueltas |

Además, Surefire genera informes por clase en `backend/target/surefire-reports/`
(7 archivos `.txt` legibles y 7 `.xml` para servidores de CI).

---

## 4. Entregables (§7 de la práctica)

| Entregable solicitado | Dónde está | Estado |
|---|---|---|
| **Proyecto GitHub** (commits, branches) | historial del repositorio | ✅ |
| **Modelo de construcción** (archivo de configuración) | `backend/pom.xml`, `frontend/package.json`, `frontend/angular.json` | ✅ |
| **Tutorial**: herramienta | [`TUTORIAL.md` §1](./TUTORIAL.md) | ✅ |
| **Tutorial**: paso a paso de instalación/configuración | [`TUTORIAL.md` §2 y §3](./TUTORIAL.md) | ✅ |
| **Tutorial**: paso a paso de construcción | [`TUTORIAL.md` §5](./TUTORIAL.md) | ✅ |

### Documentación del repositorio

| Documento | Contenido |
|---|---|
| [`TUTORIAL.md`](./TUTORIAL.md) | **entregable principal**: herramienta, instalación, configuración, construcción |
| [`BUILD.md`](./BUILD.md) | construcción sin base de datos, con solución de problemas |
| [`DOCKER.md`](./DOCKER.md) | ejecución con Docker Compose |
| [`MIGRATION.md`](./MIGRATION.md) | trazabilidad de la actualización tecnológica |
| [`README.md`](./README.md) | descripción del proyecto y arranque rápido |

---

## 5. Hallazgos técnicos

La actualización no fue un cambio de números de versión. Tres defectos **solo aparecieron
al ejecutar la aplicación**, no al compilar:

| # | Problema | Efecto | Solución |
|---|---|---|---|
| 1 | `import.sql` insertaba **por posición**, según el orden de columnas de Hibernate 5 | Hibernate 7 genera otro orden → los 47 `INSERT` fallaban y la app arrancaba **sin datos ni usuarios** | reescribir los 47 `INSERT` nombrando columnas explícitamente |
| 2 | `"/seller/**/delete"` en la configuración de seguridad | `PathPatternParser` (Spring 6+) no admite `**` intermedio → `PatternParseException` **al arrancar** | `"/seller/product/*/delete"` |
| 3 | Clave JWT de 9 caracteres | HS512 exige ≥512 bits → **el login fallaba** | clave de 81 caracteres, sobreescribible por `JWT_SECRET` |

**Lección para la práctica:** una construcción verde no garantiza un sistema que funciona.
Esto justifica directamente las siguientes prácticas del curso: pruebas de integración
automatizadas e integración continua.

---

## 6. Métricas

| Métrica | Valor |
|---|---|
| Archivos Java compilados | 43 (main) + 7 (test) |
| Pruebas backend | 37 ✅ |
| Pruebas frontend | 2 ✅ |
| Errores de análisis estático | 0 |
| Avisos de análisis estático | 154 (deuda técnica documentada) |
| Dependencias directas backend | 9 |
| Dependencias resueltas frontend | 646 paquetes |
| Tiempo de construcción backend | ~26 s |
| Tiempo de construcción frontend | ~23 s |

---

## 7. Trabajo futuro

Alineado con la evolución prevista del proyecto:

| Práctica | Tema | Estado de preparación |
|---|---|---|
| 04 | Pruebas automatizadas | base lista: JUnit 5 + Mockito + Karma |
| 05 | Calidad de código | ESLint configurado; 154 avisos identificados como backlog; falta SonarQube |
| 06 | Integración continua | `docker compose` funcionando; falta el pipeline de GitHub Actions |
| 07 | Seguridad | JWT y roles operativos; pendiente rotación de secretos y HTTPS |

---

## 8. Cómo reproducir esta entrega

```bash
git clone <URL-DEL-REPOSITORIO>
cd shop-modern
cd backend && ./mvnw clean package
cd ../frontend && npm ci && npm run build && npm run test:ci && npm run lint
```

**Requisitos:** JDK 21+, Node.js 20.19+/22.12+/24+, Google Chrome (solo para las pruebas
del frontend). **No se necesita base de datos para construir.**
