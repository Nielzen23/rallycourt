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
- Booking duration affects computed reservation amount.
- Ongoing reservations and reservation history are separated in the dashboard.
- Reservation lists are paginated in the dashboard for easier review.
- Payment is currently handled as an internal mocked flow, not through a live gateway.

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

## Running the Application

### Prerequisites
- Docker Desktop
- Java 21
- Node.js 22+

### Running application via Docker

```bash
docker compose -f docker/docker-compose.yaml up
```

### Default Admin Account

- Email: `adminrallycourt@rallycourt.local`
- Password: `RallyCourt123`


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
