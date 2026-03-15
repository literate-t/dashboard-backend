package dashboard.utils;

import dashboard.dto.Coordinate;
import dashboard.dto.RoadNode;
import java.util.Map;

public class Geo {

  private static final int EARTH_RADIUS_KILOMETERS = 6371;

  public static double calculateHaversineDistance(Coordinate start, Coordinate end) {
    double dLat = Math.toRadians(end.latitude() - start.latitude());
    double dLon = Math.toRadians(end.longitude() - start.longitude());
    double startLatRad = Math.toRadians(start.latitude());
    double endLatRad = Math.toRadians(end.latitude());

    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(startLatRad) * Math.cos(endLatRad) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return EARTH_RADIUS_KILOMETERS * c;
  }

  public static String findNearestNodeId(double targetLongitude, double targetLatitude,
      Map<String, RoadNode> nodeMap) {
    String nearestNodeId = null;
    double result = Double.MAX_VALUE;
    for (Map.Entry<String, RoadNode> entry : nodeMap.entrySet()) {
      double longitude = entry.getValue().coordinate().longitude();
      double latitude = entry.getValue().coordinate().latitude();

      double distance = calculateHaversineDistance(new Coordinate(longitude, latitude),
          new Coordinate(targetLongitude, targetLatitude));
      if (distance < result) {
        result = distance;
        nearestNodeId = entry.getKey();
      }
    }

    if (nearestNodeId == null) {
      throw new IllegalArgumentException("No nearest node id found");
    }

    return nearestNodeId;
  }
}
