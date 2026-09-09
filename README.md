
# Online Shop Application
[![License](https://img.shields.io/badge/license-MIT-green)](./LICENSE)

#### A full-stack Online Shop web application using Spring Boot 4 and Angular 21.

> **Version modernizada.** Este arbol es el proyecto original de [zhulinn](https://github.com/zhulinn/SpringBoot-Angular7-Online-Shopping-Store)
> con todas sus dependencias actualizadas (Java 11 -> 21, Spring Boot 2.2 -> 4.1, Angular 7 -> 21).
> La funcionalidad no cambio. El detalle de la migracion esta en [MIGRATION.md](./MIGRATION.md).

This is a Single Page Appliaction with client-side rendering. It includes [backend](https://github.com/zhulinn/SpringBoot-Angular7-ShoppingCart/tree/backend) and [frontend](https://github.com/zhulinn/SpringBoot-Angular7-ShoppingCart/tree/frontend) two seperate projects on different branches.
The frontend client makes API calls to the backend server when it is running.
> This project is based on my previous project [Online-Shopping-Store](https://github.com/zhulinn/Online-Shopping-Store), which uses FreeMarker as template engine for server-side rendering. 
> 
#### Live Demo: [https://springboot-angular-shop.herokuapp.com/](https://springboot-angular-shop.herokuapp.com/) Heroku has removed free tier on Postgres, the demo is no longer working...:(

For Heroku application repo cloning, please check [Angular7-SpringBoot-hybrid-project](https://github.com/zhulinn/Angular7-SpringBoot-hybrid-project).

## Screenshot
![](https://raw.githubusercontent.com/zhulinn/blog/hexo/source/uploads/post_pics/spring-angular/cart.png)

## Features
- REST API
- Docker
- Docker Compose
- JWT authentication
- Cookie based visitors' shopping cart
- Persistent customers' shopping cart
- Cart & order management
- Checkout
- Catalogue
- Order management
- Pagination
## Technology Stacks
**Backend**
  - Java 21 (LTS)
  - Spring Boot 4.1
  - Spring Security 7 (SecurityFilterChain)
  - JWT Authentication (JJWT 0.13)
  - Spring Data JPA
  - Hibernate 7
  - PostgreSQL 17
  - Maven 3.9 (Maven Wrapper incluido)
  - JUnit 5 + Mockito

**Frontend**
  - Angular 21
  - TypeScript 5.9
  - Angular CLI + builder `@angular/build:application`
  - RxJS 7.8
  - ESLint (angular-eslint 21)
  - Karma + Jasmine
  - Bootstrap 4.6

## Database Schema

![](https://raw.githubusercontent.com/zhulinn/blog/hexo/source/uploads/post_pics/spring-angular/db.png)

## How to  Run

Start the backend server before the frontend client.  

**Backend**

  1. Install [PostgreSQL](https://www.postgresql.org/download/) and a JDK 21+.
  2. Configure datasource in `application.yml` (or use the `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` env vars).
  3. `cd backend`.
  4. Run `./mvnw clean package` (Windows: `mvnw.cmd clean package`).
  5. Run `./mvnw spring-boot:run`.
  6. Spring Boot will import mock data into database by executing `import.sql` automatically.
  7. The backend server is running on [localhost:8080]().

**Frontend**
  1. Install [Node.js](https://nodejs.org/) 20.19+, 22.12+ or 24+ and npm.
  2. `cd frontend`.
  3. Run `npm install`.
  4. Run `npm start` (equivalente a `ng serve`)
  5. The frontend client is running on [localhost:4200]().
  
Note: The backend API url is configured in `src/environments/environment.ts` of the frontend project. It is `localhost:8080/api` by default.
  
#### Run in Docker
You can build the image and run the container with Docker. 
Los `Dockerfile` son ahora multi-etapa: compilan el proyecto dentro de la imagen,
asi que no hace falta construir nada a mano.

```bash
docker compose up --build
```

- Frontend: http://localhost
- Backend:  http://localhost:8080/api

## Cuentas de ejemplo

Cargadas por `import.sql`. Contrasena: **`123`** en todas.

| Correo | Rol |
|---|---|
| `customer1@email.com` | CUSTOMER |
| `customer2@email.com` | CUSTOMER |
| `employee1@email.com` | EMPLOYEE |
| `manager1@email.com` | MANAGER |

## Comandos de construccion

> Guia detallada (incluye como construir **sin base de datos**): [BUILD.md](./BUILD.md)

| Tarea | Backend | Frontend |
|---|---|---|
| Compilar | `./mvnw clean compile` | `npm run build` |
| Pruebas | `./mvnw test` | `npm run test:ci` |
| Analisis estatico | - | `npm run lint` |
| Empaquetar | `./mvnw clean package` | `npm run build` |

