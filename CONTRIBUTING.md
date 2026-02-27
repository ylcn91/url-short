# Contributing to URLShort

Thank you for your interest in contributing! This guide will help you get started.

## Getting Started

### Prerequisites

- Java 21+
- Node.js 20+
- PostgreSQL 14+
- Redis 7+
- Apache Kafka (optional, for click event processing)

### Local Setup

```bash
# Clone the repository
git clone https://github.com/ylcn91/url-short.git
cd url-short

# Start backend
./mvnw spring-boot:run

# Start frontend (in a separate terminal)
cd frontend
npm install
npm run dev
```

The backend runs on `http://localhost:8080` and the frontend on `http://localhost:3000`.

## Development Workflow

1. **Fork** the repository
2. **Create a branch** from `main`: `git checkout -b feature/your-feature`
3. **Make your changes** following the code style guidelines below
4. **Write tests** for new functionality
5. **Run the test suite**: `./mvnw test`
6. **Submit a pull request** against `main`

## Code Style

### Backend (Java)

- Java 21 features: records, pattern matching, sealed classes
- Lombok `@Slf4j` for logging (no manual `LoggerFactory`)
- Spring Boot 3.x conventions
- Service classes are concrete (no unnecessary interfaces)
- Response DTOs are Java records with manual Builder pattern
- Request DTOs are Lombok `@Data` classes (for validation annotations)
- Flyway for database migrations (`src/main/resources/db/migration/`)

### Frontend (Next.js)

- Next.js 14 with App Router
- TypeScript strict mode
- Tailwind CSS for styling
- shadcn/ui component library
- Zustand for state management

## Project Structure

```
url-short/
├── src/main/java/com/urlshort/
│   ├── config/          # Spring configuration
│   ├── controller/      # REST controllers
│   ├── domain/          # JPA entities
│   ├── dto/             # Data transfer objects (by domain)
│   │   ├── auth/
│   │   ├── link/
│   │   ├── workspace/
│   │   └── ...
│   ├── event/           # Kafka producers/consumers
│   ├── exception/       # Custom exceptions
│   ├── repository/      # Spring Data JPA repositories
│   ├── security/        # JWT, filters, user details
│   ├── service/         # Business logic
│   └── util/            # Utility classes
├── src/test/java/       # Tests
├── frontend/            # Next.js frontend
└── docs/                # Project documentation
```

## Testing

- **Unit tests**: JUnit 5 + Mockito
- **Integration tests**: `@SpringBootTest` with Testcontainers
- **Architecture tests**: ArchUnit for enforcing conventions

Run all tests:

```bash
./mvnw test
```

Run a specific test class:

```bash
./mvnw test -Dtest=ShortLinkServiceTest
```

## Commit Messages

Use clear, descriptive commit messages:

```
feat: Add bulk link creation endpoint
fix: Resolve cache invalidation on link update
refactor: Convert response DTOs to Java records
test: Add workspace service unit tests
docs: Update API documentation
```

## Reporting Issues

- Use [GitHub Issues](https://github.com/ylcn91/url-short/issues)
- Include steps to reproduce, expected vs actual behavior
- Attach relevant logs or screenshots

## License

By contributing, you agree that your contributions will be licensed under the project's license.
