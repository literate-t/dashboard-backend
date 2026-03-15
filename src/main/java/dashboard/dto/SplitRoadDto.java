package dashboard.dto;

import java.util.List;

public record SplitRoadDto(String parentRoadId, List<List<Double>> coordinates) {}
