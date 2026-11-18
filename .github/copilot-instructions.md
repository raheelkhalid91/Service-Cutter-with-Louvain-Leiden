# ServiceCutter AI Coding Guidelines

## Architecture Overview
ServiceCutter is a microservices decomposition tool with two main components:
- **Editor**: JHipster-generated web UI (Spring Boot + AngularJS) for user interaction
- **Engine**: REST API backend (Spring Boot + Jersey) providing core decomposition logic

Components communicate via HTTP REST API under `/engine` path. Data flows: Editor → Engine API → Postgres DB.

## Key Components & Data Flow
- **Nanoentities**: Atomic domain elements (e.g., Customer.name, Order.total)
- **User Representations**: Stakeholder views of the system
- **Coupling Criteria**: Rules for measuring service cohesion/coupling
- **Solver**: Algorithm that suggests service boundaries based on coupling analysis

API endpoints: `/engine/systems`, `/engine/solver`, `/engine/import`, `/engine/couplingcriteria`

## Development Workflows
### Full Stack Development
```bash
# Start all services with Docker (recommended for testing)
docker-compose up -d

# Or run components separately for development
cd Engine && mvn spring-boot:run -Drun.jvmArguments='-Dserver.port=8090'
cd Editor && mvn spring-boot:run -Drun.jvmArguments='-Dserver.port=8080'
```

### Frontend Development (Editor)
```bash
cd Editor
npm install  # Install Node dependencies
grunt serve  # Start dev server with live reload
```

### Build & Test
```bash
# Full build
mvn clean package

# Editor production build
cd Editor && mvn -Pprod clean package

# Run tests
cd Editor && grunt test  # Karma/Jasmine unit tests
mvn test                 # Backend unit tests
```

## Project Conventions
### Code Structure
- **Engine**: Jersey REST endpoints in `ch.hsr.servicecutter` package
- **Editor**: JHipster standard structure (`src/main/webapp` for frontend)
- Multi-module Maven project with parent POM

### API Patterns
- REST endpoints use `@Path` annotations with path params (e.g., `@Path("/systems/{id}")`)
- JSON serialization handled by Jackson with custom `ObjectMapperContextResolver`
- Error handling: Standard HTTP status codes, no custom exception framework

### Data Models
- Defined in JSON schemas (`JSON_Schemas/` directory)
- Key entities: UserSystem, Nanoentity, CouplingInstance, SolverResult
- Database: Postgres with JPA/Hibernate (Editor), in-memory storage (Engine)

### Frontend Patterns
- AngularJS 1.x with JHipster scaffolding
- Bower for dependency management (legacy, replaced by npm in modern JHipster)
- Grunt for build tasks (replaced by Webpack in modern JHipster)

## Integration Points
- **Database**: Postgres container, accessed via JDBC in Editor
- **Cross-service**: Editor proxies Engine API calls via `application.links.engine.*` properties
- **External**: No external APIs, self-contained tool

## Common Patterns
- **Import Flow**: ERM → User Representations → Nanoentities → Coupling Analysis → Solver
- **Configuration**: Spring profiles (`prod`, `dev`) for environment-specific settings
- **Logging**: Logback configuration in `src/main/resources`

## Key Files
- `Engine/src/main/java/ch/hsr/servicecutter/EngineServiceApplication.java`: Main Spring Boot app
- `Editor/src/main/webapp/app/`: AngularJS application code
- `docker-compose.yml`: Full deployment configuration
- `JSON_Schemas/`: API contract definitions