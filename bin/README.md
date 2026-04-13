# Jewelry Store Management System UI (Spring Boot + Thymeleaf)

This project includes frontend pages and a complete database-backed login flow for management users.

## Structure

- `src/main/java/com/example/Jewelry/controller/LoginController.java`
- `src/main/java/com/example/Jewelry/repository/AccountRepository.java`
- `src/main/java/com/example/Jewelry/config/DBConnection.java`
- `src/main/resources/templates/management/login.html`
- `src/main/resources/static/css/management/login.css`
- `src/main/resources/templates/admin/dashboard.html`

## Login Flow

- `GET /login` -> show login page
- `POST /login` -> validate account from `Account` table (`Status = 'ACTIVE'`)
- success -> redirect to `/management/dashboard`
- fail -> stay on login page with error message

`/management/dashboard` currently redirects to `/admin/dashboard`.

## Database

`DBConnection` uses SQL Server URL:

- `jdbc:sqlserver://localhost:1433;databaseName=JewelryStoreDB`

Default credentials in code:

- username: `sa`
- password: `123456`

Update them in `src/main/java/com/example/Jewelry/config/DBConnection.java` to match your local SQL Server.

## Run

```powershell
mvn spring-boot:run
```

## Main URLs

- `http://localhost:8080/`
- `http://localhost:8080/login`
- `http://localhost:8080/management/dashboard`
- `http://localhost:8080/admin/dashboard`
