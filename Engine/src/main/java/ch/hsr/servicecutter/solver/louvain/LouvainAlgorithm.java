
package ch.hsr.servicecutter.solver.louvain;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Java port of the Louvain community detection algorithm.
 * Based on the JavaScript implementation from https://github.com/multivacplatform/louvain
 */
public class LouvainAlgorithm {

    // Constants
    private static final int PASS_MAX = -1;
    private static final double MIN = 0.0000001;

    // Local fields
    private List<String> originalGraphNodes;
    private List<Edge> originalGraphEdges;
    private Graph originalGraph;
    private Map<String, Integer> partitionInit;
    private Map<String, Integer> edgeIndex = new HashMap<>();

    // Inner classes
    public static class Edge {
        public String source;
        public String target;
        public double weight;

        public Edge(String source, String target, double weight) {
            this.source = source;
            this.target = target;
            this.weight = weight;
        }
    }

    public static class Graph {
        public List<String> nodes;
        public List<Edge> edges;
        public Map<String, Map<String, Double>> assocMat;
    }

    public static class Status {
        public Map<String, Integer> nodesToCom = new HashMap<>();
        public double totalWeight;
        public Map<Integer, Double> internals = new HashMap<>();
        public Map<Integer, Double> degrees = new HashMap<>();
        public Map<String, Double> gdegrees = new HashMap<>();
        public Map<String, Double> loops = new HashMap<>();
    }

    // Helpers
    private double getDegreeForNode(Graph graph, String node) {
        Map<String, Double> neighbors = graph.assocMat.get(node);
        if (neighbors == null) return 0;
        double weight = 0;
        for (Map.Entry<String, Double> entry : neighbors.entrySet()) {
            double value = entry.getValue();
            if (node.equals(entry.getKey())) {
                value *= 2;
            }
            weight += value;
        }
        return weight;
    }

    private List<String> getNeighboursOfNode(Graph graph, String node) {
        Map<String, Double> neighbors = graph.assocMat.get(node);
        return neighbors != null ? new ArrayList<>(neighbors.keySet()) : new ArrayList<>();
    }

    private Double getEdgeWeight(Graph graph, String node1, String node2) {
        Map<String, Double> neighbors = graph.assocMat.get(node1);
        return neighbors != null ? neighbors.get(node2) : null;
    }

    private double getGraphSize(Graph graph) {
        double size = 0;
        for (Edge edge : graph.edges) {
            size += edge.weight;
        }
        return size;
    }

    private void addEdgeToGraph(Graph graph, Edge edge) {
        updateAssocMat(graph, edge);
        String key = edge.source + "_" + edge.target;
        if (edgeIndex.containsKey(key)) {
            graph.edges.get(edgeIndex.get(key)).weight = edge.weight;
        } else {
            graph.edges.add(edge);
            edgeIndex.put(key, graph.edges.size() - 1);
        }
    }

    private Map<String, Map<String, Double>> makeAssocMat(List<Edge> edgeList) {
        Map<String, Map<String, Double>> mat = new HashMap<>();
        for (Edge edge : edgeList) {
            mat.computeIfAbsent(edge.source, k -> new HashMap<>()).put(edge.target, edge.weight);
            mat.computeIfAbsent(edge.target, k -> new HashMap<>()).put(edge.source, edge.weight);
        }
        return mat;
    }

    private void updateAssocMat(Graph graph, Edge edge) {
        graph.assocMat.computeIfAbsent(edge.source, k -> new HashMap<>()).put(edge.target, edge.weight);
        graph.assocMat.computeIfAbsent(edge.target, k -> new HashMap<>()).put(edge.source, edge.weight);
    }

    // Core Algorithm
    private void initStatus(Graph graph, Status status, Map<String, Integer> part) {
        status.nodesToCom.clear();
        status.totalWeight = 0;
        status.internals.clear();
        status.degrees.clear();
        status.gdegrees.clear();
        status.loops.clear();
        status.totalWeight = getGraphSize(graph);

        if (part == null) {
            for (int i = 0; i < graph.nodes.size(); i++) {
                String node = graph.nodes.get(i);
                status.nodesToCom.put(node, i);
                double deg = getDegreeForNode(graph, node);
                if (deg < 0) throw new IllegalArgumentException("Bad graph type, use positive weights!");
                status.degrees.put(i, deg);
                status.gdegrees.put(node, deg);
                Double loopWeight = getEdgeWeight(graph, node, node);
                status.loops.put(node, loopWeight != null ? loopWeight : 0);
                status.internals.put(i, status.loops.get(node));
            }
        } else {
            for (String node : graph.nodes) {
                int com = part.get(node);
                status.nodesToCom.put(node, com);
                double deg = getDegreeForNode(graph, node);
                status.degrees.put(com, status.degrees.getOrDefault(com, 0.0) + deg);
                status.gdegrees.put(node, deg);
                double inc = 0.0;
                List<String> neighbours = getNeighboursOfNode(graph, node);
                for (String neighbour : neighbours) {
                    Double weight = graph.assocMat.get(node).get(neighbour);
                    if (weight <= 0) throw new IllegalArgumentException("Bad graph type, use positive weights");
                    if (part.get(neighbour) == com) {
                        if (neighbour.equals(node)) {
                            inc += weight;
                        } else {
                            inc += weight / 2.0;
                        }
                    }
                }
                status.internals.put(com, status.internals.getOrDefault(com, 0.0) + inc);
            }
        }
    }

    private double modularity(Status status) {
        double links = status.totalWeight;
        double result = 0.0;
        Set<Integer> communities = new HashSet<>(status.nodesToCom.values());
        for (int com : communities) {
            double inDegree = status.internals.getOrDefault(com, 0.0);
            double degree = status.degrees.getOrDefault(com, 0.0);
            if (links > 0) {
                result += inDegree / links - Math.pow(degree / (2.0 * links), 2);
            }
        }
        return result;
    }

    private Map<Integer, Double> neighcom(String node, Graph graph, Status status) {
        Map<Integer, Double> weights = new HashMap<>();
        List<String> neighbourhood = getNeighboursOfNode(graph, node);
        for (String neighbour : neighbourhood) {
            if (!neighbour.equals(node)) {
                double weight = graph.assocMat.get(node).getOrDefault(neighbour, 1.0);
                int neighbourcom = status.nodesToCom.get(neighbour);
                weights.put(neighbourcom, weights.getOrDefault(neighbourcom, 0.0) + weight);
            }
        }
        return weights;
    }

    private void insert(String node, int com, double weight, Status status) {
        status.nodesToCom.put(node, com);
        status.degrees.put(com, status.degrees.getOrDefault(com, 0.0) + status.gdegrees.get(node));
        status.internals.put(com, status.internals.getOrDefault(com, 0.0) + weight + status.loops.get(node));
    }

    private void remove(String node, int com, double weight, Status status) {
        status.degrees.put(com, status.degrees.get(com) - status.gdegrees.get(node));
        status.internals.put(com, status.internals.get(com) - weight - status.loops.get(node));
        status.nodesToCom.put(node, -1);
    }

    private Map<String, Integer> renumber(Map<String, Integer> dict) {
        Map<String, Integer> ret = new HashMap<>(dict);
        Map<Integer, Integer> newValues = new HashMap<>();
        int count = 0;
        for (Map.Entry<String, Integer> entry : dict.entrySet()) {
            int value = entry.getValue();
            if (!newValues.containsKey(value)) {
                newValues.put(value, count);
                count++;
            }
            ret.put(entry.getKey(), newValues.get(value));
        }
        return ret;
    }

    private void oneLevel(Graph graph, Status status) {
        boolean modif = true;
        int nbPassDone = 0;
        double curMod = modularity(status);
        double newMod = curMod;
        while (modif && nbPassDone != PASS_MAX) {
            curMod = newMod;
            modif = false;
            nbPassDone++;
            for (String node : graph.nodes) {
                int comNode = status.nodesToCom.get(node);
                double degcTotw = status.gdegrees.get(node) / (status.totalWeight * 2.0);
                Map<Integer, Double> neighCommunities = neighcom(node, graph, status);
                remove(node, comNode, neighCommunities.getOrDefault(comNode, 0.0), status);
                int bestCom = comNode;
                double bestIncrease = 0;
                for (Map.Entry<Integer, Double> entry : neighCommunities.entrySet()) {
                    int com = entry.getKey();
                    double incr = entry.getValue() - status.degrees.getOrDefault(com, 0.0) * degcTotw;
                    if (incr > bestIncrease) {
                        bestIncrease = incr;
                        bestCom = com;
                    }
                }
                insert(node, bestCom, neighCommunities.getOrDefault(bestCom, 0.0), status);
                if (bestCom != comNode) {
                    modif = true;
                }
            }
            newMod = modularity(status);
            if (newMod - curMod < MIN) {
                break;
            }
        }
    }

    private Graph inducedGraph(Map<String, Integer> partition, Graph graph) {
        Graph ret = new Graph();
        ret.nodes = new ArrayList<>();
        ret.edges = new ArrayList<>();
        ret.assocMat = new HashMap<>();
        edgeIndex.clear();
        // add nodes from partition values
        Set<Integer> partitionValues = new HashSet<>(partition.values());
        ret.nodes.addAll(partitionValues.stream().map(String::valueOf).collect(Collectors.toList()));
        for (Edge edge : graph.edges) {
            double weight = edge.weight;
            int com1 = partition.get(edge.source);
            int com2 = partition.get(edge.target);
            Double wPrec = getEdgeWeight(ret, String.valueOf(com1), String.valueOf(com2));
            double newWeight = (wPrec != null ? wPrec : 0) + weight;
            Edge newEdge = new Edge(String.valueOf(com1), String.valueOf(com2), newWeight);
            addEdgeToGraph(ret, newEdge);
        }
        return ret;
    }

    private Map<String, Integer> partitionAtLevel(List<Map<String, Integer>> dendogram, int level) {
        Map<String, Integer> partition = new HashMap<>(dendogram.get(0));
        for (int i = 1; i < level + 1; i++) {
            for (String key : new HashSet<>(partition.keySet())) {
                String node = key;
                Integer com = partition.get(key);
                if (com != null) {
                    Integer newCom = dendogram.get(i).get(String.valueOf(com));
                    if (newCom != null) {
                        partition.put(node, newCom);
                    }
                }
            }
        }
        return partition;
    }

    private List<Map<String, Integer>> generateDendogram(Graph graph, Map<String, Integer> partInit) {
        if (graph.edges.isEmpty()) {
            Map<String, Integer> part = new HashMap<>();
            for (String node : graph.nodes) {
                part.put(node, 0);
            }
            return Arrays.asList(part);
        }
        Status status = new Status();
        initStatus(graph, status, partInit);
        double mod = modularity(status);
        List<Map<String, Integer>> statusList = new ArrayList<>();
        oneLevel(graph, status);
        double newMod = modularity(status);
        Map<String, Integer> partition = renumber(status.nodesToCom);
        statusList.add(partition);
        mod = newMod;
        Graph currentGraph = inducedGraph(partition, graph);
        initStatus(currentGraph, status, null);
        while (true) {
            oneLevel(currentGraph, status);
            newMod = modularity(status);
            if (newMod - mod < MIN) {
                break;
            }
            partition = renumber(status.nodesToCom);
            statusList.add(partition);
            mod = newMod;
            currentGraph = inducedGraph(partition, currentGraph);
            initStatus(currentGraph, status, null);
        }
        return statusList;
    }

    public Map<String, Integer> run() {
        List<Map<String, Integer>> dendogram = generateDendogram(originalGraph, partitionInit);
        return partitionAtLevel(dendogram, dendogram.size() - 1);
    }

    public LouvainAlgorithm nodes(List<String> nds) {
        originalGraphNodes = nds;
        return this;
    }

    public LouvainAlgorithm edges(List<Edge> edgs) {
        if (originalGraphNodes == null) throw new IllegalStateException("Please provide the graph nodes first!");
        originalGraphEdges = edgs;
        Map<String, Map<String, Double>> assocMat = makeAssocMat(edgs);
        originalGraph = new Graph();
        originalGraph.nodes = originalGraphNodes;
        originalGraph.edges = originalGraphEdges;
        originalGraph.assocMat = assocMat;
        return this;
    }

    public LouvainAlgorithm partitionInit(Map<String, Integer> prttn) {
        partitionInit = prttn;
        return this;
    }
}