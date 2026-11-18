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
 * Interface for clustering algorithms.
 *
 * @author Ludo Waltman
 * @author Nees Jan van Eck
 * @author Vincent Traag
 */
public interface ClusteringAlgorithm
{
    /**
     * Finds a clustering of the nodes in a network.
     *
     * @param network Network
     *
     * @return Clustering
     */
    public Clustering findClustering(Network network);
}