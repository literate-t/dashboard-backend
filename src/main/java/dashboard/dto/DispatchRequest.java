package dashboard.dto;

public record DispatchRequest(String vehicleId, String startNodeId, String endNodeId,
                              double speed) {

}
