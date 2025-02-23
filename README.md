
# Currency Converter Application

This application is a currency converter built with Kotlin, Spring Boot, Gradle, jOOQ, and PostgreSQL. It exposes REST APIs to create and query currency conversion requests. The project is designed to be built, tested, and run from the command-line, and it can also be deployed inside Docker containers.

---

## Prerequisites

- Java 21 
- Gradle 
- Docker & Docker Compose 
- PostgreSQL
---

## Building the Application

To build the project from the command-line, run:

```bash
./gradlew clean build
```

This command compiles the code, runs tests, and produces a JAR file in `build/libs`.

---

## Testing the Application

To run the tests from the command-line, execute:

```bash
./gradlew test
```

The tests cover both unit and integration scenarios (including those using Testcontainers with PostgreSQL). Test results will be available in `build/reports/tests/test/index.html`.

---

## Running the Application Locally

To run the application on your local machine (outside of Docker), use:

```bash
./gradlew bootRun
```

The application will start on port **8080**. Make sure your `application.yml` is configured to bind to `0.0.0.0` (or your desired network interface) so that it is accessible:

```yaml
server:
  address: 0.0.0.0
  port: 8080
```

You can then access the API endpoints (for example, via Postman or your browser) at:

```
http://localhost:8080/api/conversion
```

---

## Running the Application in a Docker Container

### Step 1: Build the Docker Image

From the project root (where the Dockerfile is located), build the image:

```bash
docker build -t backend1 .
```

### Step 2: Prepare and Run Docker Compose

Ensure you have a `docker-compose.yml` file similar to the following:

```yaml
services:
  postgres:
    image: postgres:17
    container_name: postgres
    environment:
      POSTGRES_DB: currency
      POSTGRES_USER: currency
      POSTGRES_PASSWORD: currency
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    restart: always

  app:
    image: backend1
    container_name: backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/currency
      SPRING_DATASOURCE_USERNAME: currency
      SPRING_DATASOURCE_PASSWORD: currency
    depends_on:
      - postgres
    restart: always

volumes:
  postgres-data:
    driver: local
```

Run the containers using:

```bash
docker-compose up -d
```

This command starts both the PostgreSQL and backend containers. The backend container will be accessible at [http://localhost:8080](http://localhost:8080).

### Troubleshooting Containerized Tests

If you run integration tests inside a container and they fail (e.g., Testcontainers errors), ensure that:
- Docker is running on your host.
- The Docker socket is mounted into your test container if necessary.
- The environment variable `TESTCONTAINERS_HOST_OVERRIDE` is set appropriately (e.g., `host.docker.internal`).

---

## Project Structure

```
currency-converter-kotlin/
├── libs.versions.toml                  #  Shared dependency versions
├── build.gradle.kts                    # Gradle build script (Kotlin DSL)
├── docker-compose.yml                  # Docker Compose configuration (app + PostgreSQL)
├── Dockerfile                          # Dockerfile for building the application image
├── README.md                           # Project documentation
└── src/
    ├── jooq/
    │   └── main/
    │       └── kotlin/
    │           └── lv/bogatirjovs/currencycalculatorkotlin/
    │               ├── keys/          # jOOQ-generated keys
    │               ├── tables/        # jOOQ-generated tables
    │               ├── pojos/         # jOOQ-generated POJOs
    │               └── records/       # jOOQ-generated record classes
    ├── main/
    │   ├── kotlin/
    │   │   └── lv/bogatirjovs/currencyconverterkotlin/
    │   │       ├── api/              # REST controllers/endpoints
    │   │       ├── dto/              # Data Transfer Objects
    │   │       ├── enu/              # Enums (e.g., ConversionStatus)
    │   │       ├── exception/        # Custom exception classes
    │   │       ├── repositories/     # Repositories / DAO logic (jOOQ usage)
    │   │       ├── services/         # Business logic (service layer)
    │   │       └── utils/            # Utility/helper classes
    │   └── resources/
    │       └── application.yml       # Application configuration (Spring Boot)

```

---

## Configuration

Key configuration properties are defined in `src/main/resources/application.yml`:

```yaml
currency-conversion:
  defaultFee: 0.01
  ecb-api: https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/currency
    username: currency
    password: currency
    driver-class-name: org.postgresql.Driver
  jooq:
    sql-dialect: POSTGRES
```

- **currency-conversion.defaultFee**: The default fee to apply on conversion requests.
- **currency-conversion.ecb-api**: URL to download daily exchange rates from ECB.
- **spring.datasource**: Database connection details.
- **spring.jooq.sql-dialect**: jOOQ dialect used for SQL generation.

---

## Additional Notes

- **Building, Testing, and Running:**  
  All tasks can be executed from the command-line using the Gradle Wrapper (`./gradlew ...`).
- **Docker:**  
  For containerized deployment, the Dockerfile builds the application image and docker-compose.yml orchestrates both the application and PostgreSQL container.
- **Robust Code:**  
  The project emphasizes robust, readable code with clear separation between API, service, and repository layers.
- **Documentation:**  
  This README explains how to build, test, and run the application. If you have any further questions, please refer to the documentation provided in the source code or contact the project maintainers.

---

## How to Run the Project

1. **Build and Test:**
   ```bash
   ./gradlew clean build
   ./gradlew test
   ```

2. **Run Locally:**
   ```bash
   ./gradlew bootRun
   ```
   The application will be available at: [http://localhost:8080](http://localhost:8080)

3. **Build Docker Image:**
   ```bash
   docker build -t backend1 .
   ```

4. **Run with Docker Compose:**
   ```bash
   docker-compose up -d
   ```
   The application will be accessible at: [http://localhost:8080](http://localhost:8080)
