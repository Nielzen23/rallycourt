# Database Setup

Prepare the frontend, backend, PostgreSQL, and MongoDB using Docker Compose.

## Services

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- PostgreSQL: `localhost:5432`
- MongoDB: `localhost:27017`

## Default Credentials

### PostgreSQL

- Database: `rallycourt`
- Username: `rallycourt`
- Password: `rallycourt`

### MongoDB

- Root Username: `rallycourt`
- Root Password: `rallycourt`
- Database: `rallycourt`

## Run

```powershell
cd D:\dev\RallyCourt\docker
docker compose -f docker-compose.yaml up -d
```

## Stop

```powershell
docker compose -f docker-compose.yaml down
```

## Remove Containers and Volumes

```powershell
docker compose -f docker-compose.yaml down -v
```
