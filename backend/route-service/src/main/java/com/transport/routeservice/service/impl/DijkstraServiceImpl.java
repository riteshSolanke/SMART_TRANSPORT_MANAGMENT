package com.transport.routeservice.service.impl;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DijkstraServiceImpl {
    public Map<Long, Double> findShortestDistances(Long sourceStopId,
                                                   Map<Long, List<Edge>> graph) {
        Map<Long, Double> distances = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(
                Comparator.comparingDouble(n -> n.distance));
        distances.put(sourceStopId, 0.0);
        pq.add(new Node(sourceStopId, 0.0));
        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (current.distance > distances.getOrDefault(current.stopId, Double.MAX_VALUE)) {
                continue;
            }
            List<Edge> neighbors = graph.getOrDefault(current.stopId, Collections.emptyList());
            for (Edge edge : neighbors) {
                double newDist = current.distance + edge.weight;
                if (newDist < distances.getOrDefault(edge.targetStopId, Double.MAX_VALUE)) {
                    distances.put(edge.targetStopId, newDist);
                    pq.add(new Node(edge.targetStopId, newDist));
                }
            }
        }
        return distances;
    }

    public record Node(Long stopId, double distance) {}

    public record Edge(Long targetStopId, double weight) {}

}
