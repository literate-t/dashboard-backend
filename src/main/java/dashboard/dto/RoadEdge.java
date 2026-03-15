package dashboard.dto;

import java.util.List;

public record RoadEdge(
    String parentRoadId,
    String edgeId,
    RoadNode startNode,
    RoadNode endNode,
    double weight,
    List<Coordinate> path
) {

}
