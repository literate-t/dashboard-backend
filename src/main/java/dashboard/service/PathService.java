package dashboard.service;

import dashboard.dto.RoadEdge;
import dashboard.manager.GraphMemoryManager;
import dashboard.utils.Geo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.springframework.stereotype.Service;

@Service
public class PathService {

  private final GraphMemoryManager graphMemoryManager;

  public PathService(GraphMemoryManager graphMemoryManager) {
    this.graphMemoryManager = graphMemoryManager;
  }

  public List<RoadEdge> findShortestPath(String startNodeId, String endNodeId) {
    if (startNodeId == null || startNodeId.equals(endNodeId)) {
      return Collections.emptyList();
    }

    // 다음 방문할 노드를 결정
    PriorityQueue<NodeCost> pq = new PriorityQueue<>();

    // 최소 누적 거리 계산
    Map<String, Double> distanceMap = new HashMap<>();

    // 경로 역추적을 위함
    Map<String, RoadEdge> prevRoadEdgeMap = new HashMap<>();

    pq.add(new NodeCost(startNodeId, 0.0));
    distanceMap.put(startNodeId, 0.0);

    while (!pq.isEmpty()) {
      NodeCost nodeCost = pq.poll();
      String currentNodeId = nodeCost.nodeId();
      double currentAccumulatedWeight = nodeCost.weight();

      // 목적지 도착
      if (currentNodeId.equals(endNodeId)) {
        break;
      }

      // 이미 갱신된 누적값이 더 작으면 다른 노드를 탐색
      if (currentAccumulatedWeight > distanceMap.getOrDefault(currentNodeId, Double.MAX_VALUE)) {
        continue;
      }

      List<RoadEdge> edges = graphMemoryManager.getEdgeFrom(currentNodeId);
      edges.forEach(edge -> {
        String nextNodeId = edge.endNode().nodeId();
        double nextWeight = currentAccumulatedWeight + edge.weight();
        double nextAccumulated = distanceMap.getOrDefault(nextNodeId, Double.MAX_VALUE);

        if (nextWeight < nextAccumulated) {
          distanceMap.put(nextNodeId, nextWeight);
          prevRoadEdgeMap.put(nextNodeId, edge);
          pq.add(new NodeCost(nextNodeId, nextWeight));
        }
      });
    }

    return getPath(endNodeId, prevRoadEdgeMap);
  }

  private List<RoadEdge> getPath(String endNodeId,
      Map<String, RoadEdge> prevRoadEdgeMap) {

    if (!prevRoadEdgeMap.containsKey(endNodeId)) {
      return Collections.emptyList();
    }

    List<RoadEdge> path = new ArrayList<>();
    String currentNodeId = endNodeId;

    while (prevRoadEdgeMap.containsKey(currentNodeId)) {
      RoadEdge roadEdge = prevRoadEdgeMap.get(currentNodeId);
      path.add(roadEdge);
      currentNodeId = roadEdge.startNode().nodeId();
    }

    Collections.reverse(path);
    return path;
  }

  public String findNearestNodeId(double targetLongitude, double targetLatitude) {
    return Geo.findNearestNodeId(targetLongitude, targetLatitude, graphMemoryManager.getNodeMap());
  }

  private record NodeCost(String nodeId, double weight) implements Comparable<NodeCost> {

    @Override
    public int compareTo(NodeCost o) {
      return Double.compare(weight, o.weight);
    }
  }
}
