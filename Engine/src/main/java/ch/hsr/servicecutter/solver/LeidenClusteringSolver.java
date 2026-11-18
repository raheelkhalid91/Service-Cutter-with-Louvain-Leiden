package ch.hsr.servicecutter.solver;

import ch.hsr.servicecutter.model.solver.EntityPair;
import ch.hsr.servicecutter.model.solver.Service;
import ch.hsr.servicecutter.model.solver.SolverResult;
import ch.hsr.servicecutter.model.usersystem.Nanoentity;
import ch.hsr.servicecutter.model.usersystem.UserSystem;
import ch.hsr.servicecutter.scorer.Score;
import ch.hsr.servicecutter.solver.leiden.LeidenAlgorithm;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.*;

/**
 * Leiden clustering solver using embedded Leiden implementation (stub).
 */
public class LeidenClusteringSolver extends AbstractSolver<String, DefaultWeightedEdge> {

    private final Graph<String, DefaultWeightedEdge> graph;

    public LeidenClusteringSolver(final UserSystem userSystem, final Map<EntityPair, Map<String, Score>> scores, final SolverConfiguration config) {
        super(userSystem, scores);
        graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        buildNodes();
        buildEdges();
    }

    @Override
    protected void createNode(final String name) {
        graph.addVertex(name);
    }

    @Override
    protected String getNode(final String name) {
        return name;
    }

    @Override
    protected void createEdgeAndSetWeight(final Nanoentity first, final Nanoentity second, final double weight) {
        DefaultWeightedEdge edge = graph.addEdge(createNodeIdentifier(first), createNodeIdentifier(second));
        setWeight(edge, weight);
    }

    @Override
    protected void removeEdge(final DefaultWeightedEdge edge) {
        graph.removeEdge(edge);
    }

    @Override
    protected DefaultWeightedEdge getEdge(final Nanoentity first, final Nanoentity second) {
        return graph.getEdge(createNodeIdentifier(first), createNodeIdentifier(second));
    }

    @Override
    protected Iterable<DefaultWeightedEdge> getEdges() {
        return graph.edgeSet();
    }

    @Override
    protected double getWeight(final DefaultWeightedEdge edge) {
        return graph.getEdgeWeight(edge);
    }

    @Override
    protected void setWeight(final DefaultWeightedEdge edge, final double weight) {
        graph.setEdgeWeight(edge, weight);
    }

    @Override
    public SolverResult solve() {
        // Convert graph into Leiden-compatible format
        List<String> nodes = new ArrayList<>(graph.vertexSet());
        List<LeidenAlgorithm.Edge> edges = new ArrayList<>();
        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            String source = graph.getEdgeSource(edge);
            String target = graph.getEdgeTarget(edge);
            double weight = graph.getEdgeWeight(edge);
            edges.add(new LeidenAlgorithm.Edge(source, target, weight));
        }

        LeidenAlgorithm leiden = new LeidenAlgorithm().nodes(nodes).edges(edges);
        Map<String, Integer> partition = leiden.run();

        // Group by community
        Map<Integer, List<String>> groups = new HashMap<>();
        for (Map.Entry<String, Integer> entry : partition.entrySet()) {
            groups.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }
        Collection<Collection<String>> clusters = new ArrayList<>(groups.values());

        Set<Service> services = new HashSet<>();
        char idGenerator = 'A';
        for (Collection<String> cluster : clusters) {
            services.add(new Service(new ArrayList<>(cluster), idGenerator++));
        }
        return new SolverResult(services);
    }

}
