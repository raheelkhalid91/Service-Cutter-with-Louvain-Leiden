# Louvain Algorithm Integration in ServiceCutter

## Backend Code Integration

- **LouvainAlgorithm.java**: Core implementation of the Louvain community detection algorithm
  - Located at: `Engine/src/main/java/ch/hsr/servicecutter/solver/louvain/LouvainAlgorithm.java`
  - Purpose: Provides the mathematical implementation for detecting communities in graphs using the Louvain method
  - Key methods: `nodes()`, `edges()`, `partitionInit()`, `communities()`, `modularity()`

- **LouvainClusteringSolver.java**: ServiceCutter-specific wrapper for the Louvain algorithm
  - Located at: `Engine/src/main/java/ch/hsr/servicecutter/solver/LouvainClusteringSolver.java`
  - Purpose: Adapts the generic Louvain algorithm to ServiceCutter's microservice decomposition workflow
  - Extends `AbstractSolver<String, DefaultWeightedEdge>` to integrate with the solver framework
  - Uses embedded Louvain implementation to process user systems and coupling scores

- **SolverEndpoint.java**: REST API endpoint registration
  - Located at: `Engine/src/main/java/ch/hsr/servicecutter/solver/SolverEndpoint.java`
  - Purpose: Exposes the Louvain algorithm through the REST API
  - Constants defined:
    - `MODE_LOUVAIN = "Louvain"`
    - `MODES = new String[] { MODE_GIRVAN_NEWMAN, MODE_LEUNG, MODE_CW, MODE_MARKOV, MODE_LOUVAIN }`
  - Switch statement in `solveSystem()` method routes "Louvain" requests to `new LouvainClusteringSolver()`

## Frontend Dropdown Integration

- **solver.controller.js**: AngularJS controller configuration
  - Located at: `Editor/src/main/webapp/scripts/app/solver/solver.controller.js`
  - Purpose: Manages the solver UI and algorithm selection
  - Algorithm array: `$scope.availableAlgorithms = ['Leung','Girvan-Newman','Chinese Whispers','Markov (MCL)','Louvain','Leiden']`
  - Parameter initialization: `$scope.louvainResolution = 1.0`
  - Configuration building: Includes `'resolution': $scope.louvainResolution` in solverConfig.algorithmParams

- **solver.html**: Template rendering
  - Located at: `Editor/src/main/webapp/scripts/app/solver/solver.html`
  - Purpose: Displays the algorithm dropdown in the UI
  - HTML element: `<select class="form-control" ng-model="algorithm" ng-options="a for a in availableAlgorithms"></select>`
  - AngularJS directive binds the dropdown to the availableAlgorithms array

## Commands Used for Development and Deployment

- **Docker Compose Development Setup**:
  - Command: `docker-compose -f docker-compose-dev.yml up -d`
  - Purpose: Starts ServiceCutter in development mode with local code builds, mapping Editor to port 8080

- **Docker Compose Production Setup**:
  - Command: `docker-compose up -d`
  - Purpose: Starts ServiceCutter with pre-built images, mapping Editor to port 8081

- **Container Management**:
  - Command: `docker-compose down`
  - Purpose: Stops and removes all ServiceCutter containers and networks

- **Container Status Check**:
  - Command: `docker ps`
  - Purpose: Lists running containers to verify ServiceCutter services are active

- **Service Health Check**:
  - Command: `curl http://localhost:8080`
  - Purpose: Tests if the Editor service is responding on port 8080

- **Code Rebuild**:
  - Command: `docker-compose -f docker-compose-dev.yml build --no-cache`
  - Purpose: Forces a complete rebuild of Docker images from source code, ensuring latest changes are included

## Integration Workflow

1. **Backend Processing**: When "Louvain" is selected, the frontend sends a POST request to `/engine/solver/{systemId}` with algorithm: "Louvain"
2. **Parameter Handling**: The resolution parameter (default 1.0) is passed in algorithmParams
3. **Solver Instantiation**: SolverEndpoint creates a LouvainClusteringSolver instance
4. **Algorithm Execution**: LouvainClusteringSolver uses LouvainAlgorithm to process the graph and return service boundaries
5. **Result Display**: Frontend receives and displays the decomposed microservices

## Key Configuration Files

- **Sample Configuration**: `Samples/solver_louvain_config.json` demonstrates proper parameter structure
- **Schema Definition**: `JSON_Schemas/6_solverConfig.json` defines the expected algorithm configuration format
- **Test Cases**: `Engine/src/test/java/ch/hsr/servicecutter/solver/LouvainSolverTest.java` validates the integration