package dashboard.dto;

import java.util.List;

public record PositionPayload(String vehicleId, double globalProgressRatio,
                              List<String> routeEdgeIds) {

}
