package ch.hsr.servicecutter.solver.leiden;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Temporary Leiden algorithm wrapper (stub) to allow integration.
 * Currently delegates to the embedded Louvain implementation until the
 * CWTS Leiden code is copied/added to the project.
 */
public class LeidenAlgorithm {

    public static class Edge {
        public final String source;
        public final String target;
        public final double weight;

        public Edge(final String source, final String target, final double weight) {
            this.source = source;
            this.target = target;
            this.weight = weight;
        }
    }

    private List<String> nodes = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();

    public LeidenAlgorithm nodes(final List<String> nds) {
        this.nodes = nds;
        return this;
    }

    public LeidenAlgorithm edges(final List<Edge> edgs) {
        this.edges = edgs;
        return this;
    }

    /**
     * Run Leiden clustering using the embedded CWTS Leiden implementation.
     */
    public Map<String, Integer> run() {
        // Defensive checks
        if (nodes == null || nodes.isEmpty()) {
            return new HashMap<>();
        }

        // Map node IDs to integer indices expected by the Leiden Network
        Map<String, Integer> nodeIndex = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            nodeIndex.put(nodes.get(i), i);
        }

        int m = edges == null ? 0 : edges.size();
        int[] src = new int[m];
        int[] tgt = new int[m];
        double[] w = new double[m];
        for (int i = 0; i < m; i++) {
            Edge e = edges.get(i);
            Integer s = nodeIndex.get(e.source);
            Integer t = nodeIndex.get(e.target);
            if (s == null || t == null) {
                // Unknown node referenced in edge; skip this edge
                continue;
            }
            src[i] = s;
            tgt[i] = t;
            w[i] = e.weight;
        }

        // Build large arrays expected by the network constructor
        nl.cwts.util.LargeIntArray[] edgeLists = new nl.cwts.util.LargeIntArray[2];
        edgeLists[0] = new nl.cwts.util.LargeIntArray(src);
        edgeLists[1] = new nl.cwts.util.LargeIntArray(tgt);
        nl.cwts.util.LargeDoubleArray edgeWeights = new nl.cwts.util.LargeDoubleArray(w);

        // Construct the network (undirected; edges included once)
        double[] nodeWeights = new double[nodes.size()];
        java.util.Arrays.fill(nodeWeights, 1.0);
        nl.cwts.networkanalysis.Network network = new nl.cwts.networkanalysis.Network(nodeWeights, edgeLists, edgeWeights, false, true);

        // Run Leiden algorithm
        nl.cwts.networkanalysis.LeidenAlgorithm alg = new nl.cwts.networkanalysis.LeidenAlgorithm();
        // Use a higher resolution to prefer smaller clusters on small graphs
        alg.setResolution(2.0);
        nl.cwts.networkanalysis.Clustering clustering = alg.findClustering(network);

        // Map results back to node IDs
        Map<String, Integer> partition = new HashMap<>();
        int[] clusters = clustering.getClusters();
        for (int i = 0; i < clusters.length && i < nodes.size(); i++) {
            partition.put(nodes.get(i), clusters[i]);
        }

        return partition;
    }
}
