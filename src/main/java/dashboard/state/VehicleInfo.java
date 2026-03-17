package dashboard.state;

import dashboard.dto.RoadEdge;
import java.util.List;

public class VehicleInfo {

  private final String vehicleId;
  private final double baseSpeed;
  private List<RoadEdge> plannedPath;

  private double globalProgressRatio = 0.0;

  private double accumulatedDistance;
  private double totalRouteDistance;

  private boolean finished;
  private boolean routeUpdated;

  public VehicleInfo(String vehicleId, List<RoadEdge> plannedPath, double baseSpeed) {
    this.vehicleId = vehicleId;
    this.baseSpeed = baseSpeed;
    this.accumulatedDistance = 0.0;

    updatePath(plannedPath);
  }

  private void updatePath(List<RoadEdge> newPath) {
    this.plannedPath = newPath;
    this.totalRouteDistance = newPath.stream().mapToDouble(RoadEdge::weight).sum();
    this.routeUpdated = true;
  }

  // 한 틱마다 호출
  public void move(double deltaSeconds) {
    if (finished || plannedPath.isEmpty()) {
      return;
    }

    // 틱 단위로 이동하는 물리적 거리
    double moveDistance = baseSpeed * deltaSeconds;
    accumulatedDistance += moveDistance;

    if (totalRouteDistance <= accumulatedDistance) {
      finished = true;
      globalProgressRatio = 1.0;
      accumulatedDistance = totalRouteDistance;
      return;
    }

    updateGlobalProgressRatio();
  }

  private void updateGlobalProgressRatio() {
    if (totalRouteDistance <= 0) {
      globalProgressRatio = 1.0;
      return;
    }

    globalProgressRatio = Math.min(1, accumulatedDistance / totalRouteDistance);
  }

  // 현재 간선의 진행률
  public double getGlobalProgressRatio() {
    if (totalRouteDistance == 0) {
      return 1;
    }

    return globalProgressRatio;
  }

  public boolean isFinished() {
    return finished;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public boolean isRouteUpdated() {
    return routeUpdated;
  }

  public List<String> extractRouteEdgeIds() {
    return plannedPath.stream().map(RoadEdge::edgeId).toList();
  }

  public void clearRouteUpdatedFlag() {
    this.routeUpdated = false;
  }
}
