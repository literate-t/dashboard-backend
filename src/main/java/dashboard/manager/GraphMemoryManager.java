package dashboard.manager;

import static dashboard.utils.Geo.calculateHaversineDistance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dashboard.dto.Coordinate;
import dashboard.dto.RoadEdge;
import dashboard.dto.RoadNode;
import dashboard.dto.SplitRoadDto;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class GraphMemoryManager {

  /**
   * 노드 ID로 노드 객체 찾기
   */
  private final Map<String, RoadNode> nodeMap = new HashMap<>();
  /**
   * 특정 노드와 연결된 모든 간선을 저장   *
   */
  private final Map<String, List<RoadEdge>> adjacencyList = new HashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @PostConstruct
  public void init() {
    try {
      InputStream inputStream = new ClassPathResource("static/split_edges.json").getInputStream();
      List<SplitRoadDto> splitRoadDtos = objectMapper.readValue(inputStream, new TypeReference<>() {
      });

      buildGraph(splitRoadDtos);

    } catch (Exception e) {

    }
  }

  private void buildGraph(List<SplitRoadDto> roadDataList) {
    for (int i = 0; i < roadDataList.size(); ++i) {
      SplitRoadDto roadData = roadDataList.get(i);
      List<List<Double>> coords = roadData.coordinates();
      if (coords == null || coords.size() < 2) {
        continue;
      }
      String parentRoadId = roadData.parentRoadId();

      // 출발, 도착 노드 정의
      List<Coordinate> path = new ArrayList<>();
      for (List<Double> coord : coords) {
        if (!coord.isEmpty()) {
          path.add(new Coordinate(coord.get(0), coord.get(1)));
        }
      }

      Coordinate startCoord = path.get(0);
      Coordinate endCoord = path.get(path.size() - 1);

      // 노드 ID는 "경도,위도"
      String startNodeId = startCoord.longitude() + "," + startCoord.latitude();
      String endNodeId = endCoord.longitude() + "," + endCoord.latitude();

      RoadNode startNode = nodeMap.computeIfAbsent(startNodeId, id -> new RoadNode(id, startCoord));
      RoadNode endNode = nodeMap.computeIfAbsent(endNodeId, id -> new RoadNode(id, endCoord));

      // 간선 가중치 계산(하버사인 공식 적용)
      String baseEdgeId = parentRoadId + "_" + (i + 1);
      String forwardEdgeId = baseEdgeId + "_F";
      String backwardEdgeId = baseEdgeId + "_R";

      double weight = calculateHaversineDistance(startCoord, endCoord);

      // 정방향
      RoadEdge edge = new RoadEdge(parentRoadId, forwardEdgeId, startNode, endNode, weight, path);

      // 역방향 통행
      List<Coordinate> reversedPath = new ArrayList<>(path);
      Collections.reverse(reversedPath);
      RoadEdge reverseEdge = new RoadEdge(parentRoadId, backwardEdgeId, endNode, startNode, weight,
          reversedPath);

      // 인접 간선 리스트 작성
      adjacencyList.computeIfAbsent(startNode.nodeId(), id -> new ArrayList<>()).add(edge);
      adjacencyList.computeIfAbsent(endNode.nodeId(), id -> new ArrayList<>()).add(reverseEdge);
    }
  }

  public List<RoadEdge> getEdgeFrom(String nodeId) {
    return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
  }

  public Map<String, RoadNode> getNodeMap() {
    return nodeMap;
  }
}
