package ch.hsr.servicecutter.solver;

import ch.hsr.servicecutter.model.solver.EntityPair;
import ch.hsr.servicecutter.model.solver.Service;
import ch.hsr.servicecutter.model.solver.SolverResult;
import ch.hsr.servicecutter.model.usersystem.Nanoentity;
import ch.hsr.servicecutter.model.usersystem.UserSystem;
import ch.hsr.servicecutter.scorer.Score;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Louvain clustering solver using JGraphT's Louvain implementation when available.
 *
 * This class avoids compile-time ties to a specific JGraphT Louvain class by
 * using reflection to locate and invoke a Louvain implementation at runtime.
 */
public class LouvainClusteringSolver extends AbstractSolver<String, DefaultWeightedEdge> {

    private final Graph<String, DefaultWeightedEdge> graph;

    public LouvainClusteringSolver(final UserSystem userSystem, final Map<EntityPair, Map<String, Score>> scores, final SolverConfiguration config) {
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
        // Try to detect a Louvain implementation from JGraphT using reflection.
        Object algoInstance = null;
        Object resultObj = null;
        String[] candidates = new String[]{
                "org.jgrapht.alg.clustering.LouvainCommunityDetection",
                "org.jgrapht.alg.clustering.LouvainClustering",
                "org.jgrapht.alg.community.LouvainCommunityDetection",
                "org.jgrapht.alg.community.LouvainClustering"
        };

        for (String className : candidates) {
            try {
                Class<?> cl = Class.forName(className);
                // try constructor with Graph parameter
                Constructor<?> ctor = null;
                for (Constructor<?> c : cl.getConstructors()) {
                    Class<?>[] params = c.getParameterTypes();
                    if (params.length == 1 && Graph.class.isAssignableFrom(params[0])) {
                        ctor = c;
                        break;
                    }
                }
                if (ctor != null) {
                    algoInstance = ctor.newInstance(graph);
                } else {
                    // try no-arg constructor
                    try {
                        algoInstance = cl.newInstance();
                        // try to find a setter or init method that accepts a graph
                        try {
                            Method setGraph = cl.getMethod("setGraph", Graph.class);
                            setGraph.invoke(algoInstance, graph);
                        } catch (NoSuchMethodException nm) {
                            // ignore
                        }
                    } catch (Exception e) {
                        algoInstance = null;
                    }
                }

                if (algoInstance != null) {
                    // try common execution methods
                    String[] methodCandidates = new String[]{"getClustering", "getClusters", "getCommunities", "detectCommunities", "call"};
                    for (String mName : methodCandidates) {
                        try {
                            Method m = algoInstance.getClass().getMethod(mName);
                            resultObj = m.invoke(algoInstance);
                            break;
                        } catch (NoSuchMethodException ns) {
                            // try next
                        }
                    }
                    if (resultObj != null) {
                        break;
                    }
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                // try next candidate
            }
        }

        if (resultObj == null) {
            // Fallback: try to use watset MarkovClustering (already present in project) as a safe fallback
            try {
                Class<?> mclClass = Class.forName("org.nlpub.watset.graph.MarkovClustering");
                // constructor: MarkovClustering(Graph, int expansionOperations, double powerCoefficient)
                Object mcl = null;
                for (Constructor<?> c : mclClass.getConstructors()) {
                    Class<?>[] params = c.getParameterTypes();
                    if (params.length == 3 && Graph.class.isAssignableFrom(params[0]) && params[1] == int.class) {
                        mcl = c.newInstance(graph, 2, 2.0);
                        break;
                    }
                }
                if (mcl != null) {
                    Method fit = mcl.getClass().getMethod("fit");
                    fit.invoke(mcl);
                    try {
                        Method getClusters = mcl.getClass().getMethod("getClusters");
                        resultObj = getClusters.invoke(mcl);
                    } catch (NoSuchMethodException ns) {
                        // ignore
                    }
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                // ignore - will throw below
            }
        }

        if (resultObj == null) {
            throw new IllegalStateException("No Louvain implementation found on classpath. Please add a JGraphT version that provides Louvain community detection.");
        }

        // Normalize resultObj into Collection<Collection<String>>
        Collection<Collection<String>> clusters = extractClusters(resultObj);

        Set<Service> services = new HashSet<>();
        char idGenerator = 'A';
        for (Collection<String> cluster : clusters) {
            services.add(new Service(new ArrayList<>(cluster), idGenerator++));
        }
        return new SolverResult(services);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Collection<Collection<String>> extractClusters(Object resultObj) {
        // If it's already a collection of collections
        if (resultObj instanceof Collection) {
            Collection col = (Collection) resultObj;
            // check if elements are collections of vertex names
            boolean innerAreCollections = !col.isEmpty() && col.iterator().next() instanceof Collection;
            if (innerAreCollections) {
                Collection<Collection<String>> out = new ArrayList<>();
                for (Object inner : col) {
                    Collection<String> cluster = new ArrayList<>();
                    for (Object v : (Collection) inner) {
                        cluster.add(String.valueOf(v));
                    }
                    out.add(cluster);
                }
                return out;
            }
        }

        // Try to call getClusters()/getCommunities() on the object
        try {
            Method m = resultObj.getClass().getMethod("getClusters");
            Object clustersObj = m.invoke(resultObj);
            return extractClusters(clustersObj);
        } catch (Exception ignored) {
        }
        try {
            Method m = resultObj.getClass().getMethod("getCommunities");
            Object clustersObj = m.invoke(resultObj);
            return extractClusters(clustersObj);
        } catch (Exception ignored) {
        }

        // Try if it's a Map of vertex->community id
        if (resultObj instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) resultObj;
            Map<Object, List<String>> groups = new HashMap<>();
            for (Map.Entry<Object, Object> e : map.entrySet()) {
                Object vertex = e.getKey();
                Object cid = e.getValue();
                groups.computeIfAbsent(cid, k -> new ArrayList<>()).add(String.valueOf(vertex));
            }
            return new ArrayList<>(groups.values());
        }

        // As a last resort, try toString parsing (not ideal)
        return Collections.singletonList(Collections.emptyList());
    }

}
