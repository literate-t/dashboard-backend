package dashboard.service;

import dashboard.dto.BatchedPositionPayload;
import dashboard.dto.BatchedPositionPayload.VehicleState;
import dashboard.dto.DispatchRequest;
import dashboard.dto.RoadEdge;
import dashboard.dto.TopEdgeStat;
import dashboard.state.VehicleInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class TrafficSimulationService {

  private final SimpMessagingTemplate messagingTemplate;
  private final Map<String, VehicleInfo> activeVehicles = new ConcurrentHashMap<>();
  private final PathService pathService;

  public TrafficSimulationService(SimpMessagingTemplate messagingTemplate,
      PathService pathService) {
    this.messagingTemplate = messagingTemplate;
    this.pathService = pathService;
  }

  public void startMove(String vehicleId, double speed, String startNode, String endNode) {
    List<RoadEdge> shortestPath = pathService.findShortestPath(startNode, endNode);

    if (shortestPath == null || shortestPath.isEmpty()) {
      throw new IllegalArgumentException("경로를 찾을 수 없습니다" + startNode + "->" + endNode);
    }

    VehicleInfo vehicleInfo = new VehicleInfo(vehicleId, shortestPath, speed);
    activeVehicles.put(vehicleId, vehicleInfo);
  }

  public int startBulkMove(List<DispatchRequest> requests) {
    requests.parallelStream().forEach(request -> {
      List<RoadEdge> shortestPath = pathService.findShortestPath(request.startNodeId(),
          request.endNodeId());
      if (shortestPath == null || shortestPath.isEmpty()) {
//        System.err.println("경로 찾기 실패: " + request.vehicleId());
        return;
      }

      VehicleInfo vehicleInfo = new VehicleInfo(request.vehicleId(), shortestPath, request.speed());
      activeVehicles.put(request.vehicleId(), vehicleInfo);
    });

    return activeVehicles.size();
  }

  @Scheduled(fixedRate = 100)
  public void broadcastPositions() {
    if (activeVehicles.isEmpty()) {
      return;
    }

    // 비교적 단순 작업에서는 데이터를 나누고 병합하는 과정에 대한 오버헤드가 더 클 수 있음
    // 데이터가 수만 개일 때는 성능 개선에 도움이 될 수도
//    List<VehicleState> batchedStates = activeVehicles.entrySet().parallelStream()
//        .map((vehicleSet) -> {
//          String vehicleId = vehicleSet.getKey();
//          VehicleInfo vehicleInfo = vehicleSet.getValue();
//          // 100ms 마다 이동하기
//          vehicleInfo.move(deltaSeconds);
//
//          List<String> routeEdgeIds = null;
//          if (vehicleInfo.isRouteUpdated()) {
//            routeEdgeIds = vehicleInfo.extractRouteEdgeIds();
//            vehicleInfo.clearRouteUpdatedFlag();
//          }
//
//          return new VehicleState(vehicleId, vehicleInfo.getGlobalProgressRatio(), routeEdgeIds);
//        }).toList();

    double deltaSeconds = 0.1;
    List<VehicleState> batchedStates = new ArrayList<>();
    // 약한 일관성(접근 시점의 데이터 스냅샷을 순회)
    activeVehicles.forEach((vehicleId, vehicleInfo) -> {
      // 100ms 마다 이동하기
      vehicleInfo.move(deltaSeconds);

      List<String> routeEdgeIds = null;
      if (vehicleInfo.isRouteUpdated()) {
        routeEdgeIds = vehicleInfo.extractRouteEdgeIds();
        vehicleInfo.clearRouteUpdatedFlag();
      }

      batchedStates.add(
          new VehicleState(vehicleId, vehicleInfo.getGlobalProgressRatio(), routeEdgeIds));
    });

    BatchedPositionPayload batchedPayload = new BatchedPositionPayload(batchedStates);
    messagingTemplate.convertAndSend("/topic/vehicle/positions", batchedPayload);

    activeVehicles.values().removeIf(VehicleInfo::isFinished);
  }

  @Scheduled(fixedRate = 1000)
  public void broadcastRoadStatistics() {
    if (activeVehicles.isEmpty()) {
      messagingTemplate.convertAndSend("/topic/vehicle/statistics/top-roads",
          Collections.emptyList());
      return;
    }

    Map<String, Long> roadTrafficCounts = activeVehicles.values().stream()
        .filter(v -> v.getGlobalProgressRatio() < 1.0)
        .map(this::extractParentRoadName).filter(Objects::nonNull)
        .collect(Collectors.groupingBy(name -> name, Collectors.counting()));

    List<TopEdgeStat> list = roadTrafficCounts.entrySet().stream()
        .map(entry -> new TopEdgeStat(entry.getKey(), entry.getValue().intValue()))
        .sorted(Comparator.comparingInt(TopEdgeStat::vehicleCount).reversed())
        .limit(10)
        .toList();

    messagingTemplate.convertAndSend("/topic/vehicle/statistics/top-roads", list);
  }

  private String extractParentRoadName(VehicleInfo vehicleInfo) {
    List<RoadEdge> edges = vehicleInfo.getPlannedEdges();
    if (edges == null || edges.isEmpty()) {
      return null;
    }

    double totalWeight = edges.stream().mapToDouble(RoadEdge::weight).sum();
    double traversedDistance = totalWeight * vehicleInfo.getGlobalProgressRatio();

    String currentParentRoadId = null;
    for (RoadEdge edge : edges) {
      if (traversedDistance <= edge.weight()) {
        currentParentRoadId = edge.parentRoadId();
        break;
      }
      traversedDistance -= edge.weight();
    }

    if (currentParentRoadId == null) {
      currentParentRoadId = edges.get(edges.size() - 1).parentRoadId();
    }

    return currentParentRoadId;
  }
}