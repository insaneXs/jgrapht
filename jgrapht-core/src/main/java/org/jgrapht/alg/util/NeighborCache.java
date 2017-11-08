/*
 * (C) Copyright 20017-2017, by Szabolcs Besenyei and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
package org.jgrapht.alg.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;

/**
 * Maintains a cache of each vertex's neighbors. While lists of neighbors can be obtained from
 * {@link Graphs}, they are re-calculated at each invocation by walking a vertex's incident edges,
 * which becomes inordinately expensive when performed often.
 * 
 * <p>
 * The cache also keeps track of successors and predecessors for each vertex.
 * This means that the result of the union of calling predecessorsOf(v) and successorsOf(v) is 
 * equal to the result of calling neighborsOf(v) for a given vertex v.
 * 
 * @param <V> the vertex type
 * @param <E> the edge type
 * 
 * @author Szabolcs Besenyei
 * @since November 2017
 */
public class NeighborCache<V, E>
    implements GraphListener<V, E>
{
    private Map<V, NeighborIndex.Neighbors<V>> successors = new HashMap<>();
    private Map<V, NeighborIndex.Neighbors<V>> predecessors = new HashMap<>();
    private Map<V, NeighborIndex.Neighbors<V>> neighbors = new HashMap<>();

    private Graph<V, E> graph;

    /**
     * Constructor
     * 
     * @param graph the input graph
     * @throws NullPointerException if the input graph is null
     */
    public NeighborCache(Graph<V, E> graph)
    {
        this.graph = Objects.requireNonNull(graph);
    }

    /**
     * Returns the unique predecessors of the given vertex if it exists in the cache, otherwise it
     * is initialized.
     * 
     * @param v the given vertex
     * @return the unique predecessors of the given vertex
     */
    public Set<V> predecessorsOf(V v)
    {
        return fetch(
            v,
            predecessors,
            k -> new NeighborIndex.Neighbors<>(Graphs.predecessorListOf(graph, v)));
    }

    /**
     * Returns the unique successors of the given vertex if it exists in the cache, otherwise it is
     * initialized.
     * 
     * @param v the given vertex
     * @return the unique successors of the given vertex
     */
    public Set<V> successorsOf(V v)
    {
        return fetch(
            v,
            successors,
            k -> new NeighborIndex.Neighbors<>(Graphs.successorListOf(graph, v)));
    }

    /**
     * Returns the unique neighbors of the given vertex if it exists in the cache, otherwise it is
     * initialized.
     * 
     * @param v the given vertex
     * @return the unique neighbors of the given vertex
     */
    public Set<V> neighborsOf(V v)
    {
        return fetch(
            v,
            neighbors,
            k -> new NeighborIndex.Neighbors<>(Graphs.neighborListOf(graph, v)));
    }

    private Set<V> fetch(V vertex,
        Map<V, NeighborIndex.Neighbors<V>> map,
        Function<V, NeighborIndex.Neighbors<V>> func)
    {
        return map.computeIfAbsent(vertex, func).getNeighbors();
    }

    @Override
    public void edgeAdded(GraphEdgeChangeEvent<V, E> e)
    {
        E edge = e.getEdge();
        V source = graph.getEdgeSource(edge);
        V target = graph.getEdgeTarget(edge);

        if (successors.containsKey(source)) {
            successors.get(source).addNeighbor(target);
        } else {
            successorsOf(source);
        }

        if (predecessors.containsKey(target)) {
            predecessors.get(target).addNeighbor(source);
        } else {
            predecessorsOf(target);
        }

        if (neighbors.containsKey(source)) {
            neighbors.get(source).addNeighbor(target);
        } else {
            neighborsOf(source);
        }
        if (neighbors.containsKey(target)) {
            neighbors.get(target).addNeighbor(source);
        } else {
            neighborsOf(target);
        }
    }

    @Override
    public void edgeRemoved(GraphEdgeChangeEvent<V, E> e)
    {
        V source = e.getEdgeSource();
        V target = e.getEdgeTarget();

        if (successors.containsKey(source)) {
            successors.get(source).removeNeighbor(target);
        }

        if (predecessors.containsKey(target)) {
            predecessors.get(target).removeNeighbor(source);
        }

        if (neighbors.containsKey(source)) {
            neighbors.get(source).removeNeighbor(target);
        }
        if (neighbors.containsKey(target)) {
            neighbors.get(target).removeNeighbor(source);
        }
    }

    @Override
    public void vertexAdded(GraphVertexChangeEvent<V> e)
    {
        // Nothing to cache until there are edges
    }

    @Override
    public void vertexRemoved(GraphVertexChangeEvent<V> e)
    {
        successors.remove(e.getVertex());
        predecessors.remove(e.getVertex());
        neighbors.remove(e.getVertex());
    }

}
