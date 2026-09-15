# Guía para ejecutar el proyecto con Docker

Cómo levantar la base de datos, el backend y (opcionalmente) el frontend con Docker Desktop
en Windows.

> Para **compilar** sin Docker ni base de datos, ver [BUILD.md](./BUILD.md).

---

## 0. Antes de empezar: dos cosas que confunden

### La pantalla de Gordon no es lo que necesitas

Al abrir Docker Desktop aparece **Gordon**, el asistente de IA ("What can I help you with?",
"Open a project folder"). **No lo necesitas para nada.** Todo esto se hace desde la terminal.
Docker Desktop solo tiene que estar **abierto y corriendo** — es el motor; la ventana puede
quedarse minimizada.

Para confirmar que el motor está encendido, mira la esquina inferior izquierda de Docker
Desktop: debe decir **Engine running** (o el ícono de la ballena en verde).

### `docker` no funciona en terminales que ya tenías abiertas

El instalador agrega Docker al PATH, pero **las terminales abiertas antes de instalarlo no
lo ven**. Si sale:

```
docker: command not found
```

...la solución casi siempre es **cerrar la terminal y abrir una nueva** (también cierra y
reabre VS Code si lo usas desde ahí).

Comprobar que quedó bien:

```bash
docker version
docker compose version
```

Debe responder con `Client` y `Server`. Si dice `Cannot connect to the Docker daemon`,
Docker Desktop no está abierto: ábrelo y espera a que diga *Engine running*.

> En esta máquina, Docker Desktop se instaló **por usuario**, no en `Program Files`:
> `C:\Users\mcdia\AppData\Local\Programs\DockerDesktop\resources\bin`
> Esa ruta ya está en el PATH de usuario, así que una terminal nueva la toma sola.
> Si aun así no la toma, puedes usarla directo en la sesión actual:
>
> ```powershell
> $env:PATH = "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin;$env:PATH"
> ```

---

## 1. Tu caso: backend en Docker + frontend con `npm start`

Es la combinación más cómoda para desarrollar: la API y la base en contenedores, y el
frontend en el servidor de desarrollo de Angular con recarga automática.

### Paso 1 — levantar base de datos y backend

Desde la raíz del proyecto (`C:\dev\shop-modern`):

```bash
docker compose up -d --build db backend
```

- `--build` construye la imagen del backend (compila el proyecto **dentro** del contenedor).
- `-d` lo deja corriendo en segundo plano.

> **La primera vez tarda varios minutos.** Maven descarga todas las dependencias dentro de
> la imagen y no reutiliza tu caché `~/.m2`. Parece congelado, pero está trabajando. Las
> siguientes veces son mucho más rápidas gracias al caché de capas de Docker.

### Paso 2 — comprobar que arrancó

```bash
docker compose ps
```

Esperado:

```
NAME                     STATUS
shop-modern-backend-1    Up
shop-modern-db-1         Up (healthy)
```

Y la API respondiendo:

```bash
curl http://localhost:8080/api/product
```

Debe devolver el catálogo en JSON (empieza con `{"content":[{"categoryType":0,...`).

En PowerShell, si `curl` se comporta raro (es un alias de `Invoke-WebRequest`), usa:

```powershell
curl.exe http://localhost:8080/api/product
```

### Paso 3 — levantar el frontend

En **otra** terminal:

```bash
cd frontend
npm start
```

Abre <http://localhost:4200>. El frontend ya apunta a `//localhost:8080/api`
(está en `src/environments/environment.ts`), que es exactamente donde quedó el backend
en Docker. No hay que configurar nada más.

Inicia sesión con `manager1@email.com` / `123`.

---

## 2. Alternativa: todo en Docker

Si prefieres no usar `npm start`, levanta también el frontend (nginx sirviendo el build
de producción):

```bash
docker compose up -d --build
```

| Servicio | URL |
|---|---|
| Frontend (nginx) | <http://localhost> |
| Backend (API) | <http://localhost:8080/api> |
| Base de datos | interna, puerto 5432 (no expuesta al host) |

En este modo nginx hace de proxy: las llamadas a `/api/` del navegador se reenvían al
contenedor del backend (ver `frontend/nginx/default.conf`).

> La imagen del frontend también tarda la primera vez: hace `npm ci` y `ng build` dentro
> del contenedor.

---

## 3. Comandos del día a día

| Qué quieres | Comando |
|---|---|
| Ver qué está corriendo | `docker compose ps` |
| Ver los logs del backend | `docker compose logs -f backend` |
| Ver los logs de la base | `docker compose logs -f db` |
| Reiniciar el backend | `docker compose restart backend` |
| Parar todo (conservar datos) | `docker compose stop` |
| Volver a arrancar | `docker compose start` |
| Parar y borrar contenedores | `docker compose down` |
| Borrar **también los datos** | `docker compose down -v` |
| Reconstruir tras cambiar código | `docker compose up -d --build backend` |
| Entrar al contenedor del backend | `docker compose exec backend sh` |
| Abrir psql en la base | `docker compose exec db psql -U postgres` |

Todos se ejecutan desde la raíz del proyecto, donde está `docker-compose.yml`.

> **Importante:** Docker **no** recompila solo. Si cambias código Java, tienes que correr
> `docker compose up -d --build backend` para que el cambio entre.

---

## 4. Cuentas de ejemplo

Se cargan solas desde `import.sql` al arrancar. Contraseña: **`123`** en todas.

| Correo | Rol |
|---|---|
| `customer1@email.com` | CUSTOMER |
| `customer2@email.com` | CUSTOMER |
| `employee1@email.com` | EMPLOYEE |
| `manager1@email.com` | MANAGER |

> Cada reinicio del backend **recrea el esquema y recarga los datos** (`ddl-auto: create`).
> Lo que registres se pierde al reiniciar. Es el comportamiento del proyecto original.

---

## 5. Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `docker: command not found` | terminal abierta antes de instalar Docker | cerrar y abrir una terminal nueva |
| `Cannot connect to the Docker daemon` | Docker Desktop cerrado | abrirlo y esperar *Engine running* |
| `port is already allocated` (8080) | algo más usa el puerto | `docker compose down`, o cambiar a `"8081:8080"` en `docker-compose.yml` |
| `port is already allocated` (80) | IIS, Skype u otro servidor web | usar el modo de la §1, o mapear `"8000:80"` |
| El build se queda "quieto" mucho rato | Maven/npm descargando dentro de la imagen | esperar; solo la primera vez |
| El frontend carga pero sin productos | el backend no está arriba | `docker compose ps` y `docker compose logs backend` |
| `401` al iniciar sesión | contraseña incorrecta | es `123` |
| Cambié código Java y no se refleja | Docker no recompila solo | `docker compose up -d --build backend` |

Para ver qué falló siempre:

```bash
docker compose logs backend
```

---

## 6. Verificado en esta máquina

Con Docker Engine 29.7.2 y Docker Compose v5.5.1:

| Comprobación | Resultado |
|---|---|
| `docker compose up -d --build db backend` | contenedores arriba, `db` *healthy* |
| `GET /api/product` | HTTP 200 con el catálogo completo |
| `POST /api/login` (`manager1@email.com` / `123`) | HTTP 200, `role: ROLE_MANAGER`, JWT emitido |
| `GET /api/profile/...` con el token | HTTP 200 |
| `GET /api/profile/...` sin token | HTTP 401 |
