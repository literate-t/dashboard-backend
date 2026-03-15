package dashboard.dto;

import java.util.List;

public record BatchedPositionPayload(List<VehicleState> vehicles) {

  public record VehicleState(String vehicleId, double globalProgressRatio,
                             List<String> routeEdgeIds) {

  }
}
