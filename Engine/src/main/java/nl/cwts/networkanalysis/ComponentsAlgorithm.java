/*
 * Copyright (c) 2018 Centre for Science and Technology Studies (CWTS), Leiden University
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */

package nl.cwts.networkanalysis;

/**
 * Algorithm for finding the connected components of a network.
 *
 * @author Ludo Waltman
 * @author Nees Jan van Eck
 * @author Vincent Traag
 */
public class ComponentsAlgorithm implements ClusteringAlgorithm
{
    /**
     * Constructs a components algorithm.
     */
    public ComponentsAlgorithm()
    {
    }

    /**
     * Finds the connected components of a network.
     *
     * @param network Network
     *
     * @return Connected components
     */
    public Clustering findClustering(Network network)
    {
        boolean[] nodesVisited;
        int i, j, k;
        int[] nodes;

        Clustering clustering = new Clustering(network.getNNodes());

        clustering.nClusters = 0;
        nodesVisited = new boolean[network.nNodes];
        nodes = new int[network.nNodes];
        for (i = 0; i < network.nNodes; i++)
            if (!nodesVisited[i])
            {
                clustering.clusters[i] = clustering.nClusters;
                nodesVisited[i] = true;
                nodes[0] = i;
                j = 1;
                k = 0;
                do
                {
                    for (int v : network.neighbors(nodes[k]))
                        if (!nodesVisited[v])
                        {
                            clustering.clusters[v] = clustering.nClusters;
                            nodesVisited[v] = true;
                            nodes[j] = v;
                            j++;
                        }
                    k++;
                } while (k < j);

                clustering.nClusters++;
            }

        clustering.orderClustersByNNodes();

        return clustering;
    }
}