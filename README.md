# Rally Court Reservation System

Rally Court is a court reservation system for managing sports court bookings, reservation history, payment flow, 
court administration, and user activity visibility. 

The project is split into separate frontend and backend services and is designed to run locally through Docker Compose.

## Tech Stack

### Backend
- Java 21
- Spring Boot 3
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Data MongoDB
- Spring Validation
- Spring AOP
- Liquibase
- PostgreSQL
- MongoDB
- Gradle

### Frontend
- React 19
- TypeScript
- Vite
- React Router
- Axios
- Leaflet / React Leaflet
- Tailwind CSS
- Vitest

## Features

### Booking Limitations
- Reservations are constrained by court availability and configured court status.
- Court bookings are only allowed within the next 2 weeks.
- Booking duration affects computed reservation amount.
- Ongoing reservations and reservation history are separated in the dashboard.
- Reservation lists are paginated in the dashboard for easier review.
- Payment is currently handled as an internal mocked flow, not through a live gateway.
- Unpaid bookings will temporarily reserve the selected schedule for 5 minutes.

### What Users Can Do
- Register and sign in
- View available courts
- Create reservations
- View ongoing reservations
- View reservation history
- Submit mocked payments for pending reservations
- View court locations on reservation entries through map links

### What Admin Can Do
- Manage courts
- Review court details and upcoming reservations
- Use Court Management map preview to inspect registered court locations
- Remove or edit courts from the court details workflow
- See user activity based on aspect logging
- Review user stats, court stats, and sports booking stats from the admin dashboard

## Architecture

The application follows a separated frontend-backend architecture:

- The frontend is a standalone React application served independently from the backend API.
- The backend is a Spring Boot service exposing REST endpoints for authentication, reservation, payment, dashboard, court management, and activity monitoring.
- PostgreSQL stores transactional and relational data such as users, courts, reservations, and payments.
- MongoDB stores analytics-oriented and activity-log data, especially user activity captured through aspect-based logging.
- Liquibase manages database schema and seed changes for both PostgreSQL and MongoDB.
- Docker Compose is used to run the local multi-service environment.

## Session Management

- The backend does not store JWTs server-side.
- Authentication requests return two tokens: a short-lived JWT and a server-stored session token.
- The JWT is sent in the `Authorization: Bearer <token>` header and is validated statelessly by Spring Security.
- JWT expiry is currently set to 20 minutes.
- The session token is stored in PostgreSQL and is used only to renew the session through `/api/auth/refresh`.
- Session token expiry is currently set to 8 hours.
- When a session token is used for refresh, the previous token is invalidated and a new JWT plus a new session token are issued.
- The backend does not use server-side HTTP sessions; Spring Security is configured with `SessionCreationPolicy.STATELESS`.

## Geoapify Usage

- Geoapify is used for court geolocation and address autocomplete features.
- The integration is used to retrieve exact map coordinates and provide address suggestions during court registration and search workflows.
- The current Geoapify account uses the free-tier plan intended for development/testing purposes only.
- Current limitations:
  - 3,000 API requests per day
  - 5 requests per second

## CI/CD

- GitHub Actions is used for basic CI automation.
- The pipeline currently performs automated test execution and Docker image build validation.
- The workflow is intended primarily for development verification and coding exercise automation.

## Running the Application

### Prerequisites
- Docker Desktop
- Java 21
- Node.js 22+

### Running application via Docker

```bash
docker compose -f docker/docker-compose.yaml up
```
- Docker Compose automatically initializes PostgreSQL, MongoDB, and application dependencies.
- Initial startup may take a few minutes during image build and database initialization.

### Default Local URLs

- Frontend: http://localhost:5173
- Backend: http://localhost:8080

### Default Admin Account

- Email: `adminrallycourt@rallycourt.local`
- Password: `RallyCourt123`

### Development Data

- The application includes pre-seeded development data managed through Liquibase migrations.
- Sample data such as users, courts, and bookings are automatically populated during application startup for the development environment.

### Accessing PostgreSQL and MongoDB Terminals

When the stack is running through `docker/docker-compose.yaml`, you can open database shells from your terminal with the following commands.

PostgreSQL via container:

```bash
docker exec -it rallycourt-postgres psql -U rallycourt -d rallycourt
```

MongoDB via container:

```bash
docker exec -it rallycourt-mongodb mongosh -u rallycourt -p rallycourt --authenticationDatabase admin rallycourt
```

If you have local database clients installed on your machine and prefer connecting through the exposed ports instead of `docker exec`, use:

PostgreSQL via local `psql` client:

```bash
psql -h localhost -p 5432 -U rallycourt -d rallycourt
```

MongoDB via local `mongosh` client:

```bash
mongosh "mongodb://rallycourt:rallycourt@localhost:27017/rallycourt?authSource=admin"
```

Default local database credentials from the Docker Compose setup:

- PostgreSQL database: `rallycourt`
- PostgreSQL username: `rallycourt`
- PostgreSQL password: `rallycourt`
- MongoDB database: `rallycourt`
- MongoDB username: `rallycourt`
- MongoDB password: `rallycourt`

## Design Decisions

- PostgreSQL was used for transactional consistency across court, reservation, payment, and user data.
- MongoDB was used for flexible activity logging and analytics-oriented read models.
- Spring AOP was used for async activity tracking.
- The implementation is intentionally focused on MVP reservation workflow coverage before deeper platform features.

## Assumptions / Limitations

- No payment gateway integration; payment flow is currently mocked.
- No mobile application.
- Email verification is not implemented.

## Future Improvements

- Real-time court availability
- Notification system
- Mobile responsiveness improvements
- Advanced analytics dashboard
