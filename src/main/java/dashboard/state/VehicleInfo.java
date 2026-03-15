package dashboard.state;

import dashboard.dto.RoadEdge;
import java.util.List;

public class VehicleInfo {

  private final String vehicleId;
  private final double speed;
  private List<RoadEdge> plannedPath;
  private double totalRouteDistance;
  private double currentAccumulatedDistance;
  private boolean finished;
  private boolean routeUpdated;

  public VehicleInfo(String vehicleId, List<RoadEdge> plannedPath, double speed) {
    this.vehicleId = vehicleId;
    this.speed = speed;
    this.currentAccumulatedDistance = 0.0;

    updatePath(plannedPath);
  }

  private void updatePath(List<RoadEdge> newPath) {
    this.plannedPath = newPath;
    this.totalRouteDistance = newPath.stream().mapToDouble(RoadEdge::weight).sum();
    this.routeUpdated = true;
  }

  // 한 틱마다 호출
  public void move(double deltaSeconds) {
    if (finished) {
      return;
    }

    // 틱 단위로 이동하는 물리적 거리
    double moveDistance = speed * deltaSeconds;
    currentAccumulatedDistance += moveDistance;

    if (currentAccumulatedDistance >= totalRouteDistance) {
      currentAccumulatedDistance = totalRouteDistance;
      finished = true;
    }
  }

  // 현재 간선의 진행률
  public double getGlobalProgressRatio() {
    if (totalRouteDistance == 0) {
      return 1;
    }

    return currentAccumulatedDistance / totalRouteDistance;
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
