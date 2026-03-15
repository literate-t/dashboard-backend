package dashboard.controller;

import dashboard.dto.DispatchBulkRequest;
import dashboard.dto.DispatchRequest;
import dashboard.service.NodeService;
import dashboard.service.PathService;
import dashboard.service.TrafficSimulationService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/traffic")
public class TrafficController {

  private final TrafficSimulationService trafficSimulationService;
  private final PathService pathService;
  private final NodeService nodeService;

  public TrafficController(TrafficSimulationService trafficSimulationService,
      PathService pathService, NodeService nodeService) {
    this.trafficSimulationService = trafficSimulationService;
    this.pathService = pathService;
    this.nodeService = nodeService;
  }

  @PostMapping("/dispatch")
  public ResponseEntity<String> dispatchVehicle(@RequestBody DispatchRequest dispatchRequest) {
    trafficSimulationService.startMove(dispatchRequest.vehicleId(), dispatchRequest.speed(),
        dispatchRequest.startNodeId(), dispatchRequest.endNodeId());
    return ResponseEntity.ok("Vehicle [" + dispatchRequest.vehicleId() + "] is ready");
  }

  @PostMapping("/dispatch/bulk")
  public ResponseEntity<String> dispatchVehiclesBulk(
      @RequestBody DispatchBulkRequest request) {
    int successCount = trafficSimulationService.startBulkMove(request.requests());
    return ResponseEntity.ok(
        successCount + " vehicles dispatched successfully");
  }

  @GetMapping("/nearest")
  public ResponseEntity<Map<String, String>> getNearestNode(@RequestParam Double lng,
      @RequestParam Double lat) {
    String nearestNodeId = pathService.findNearestNodeId(lng, lat);
    return ResponseEntity.ok(Map.of("nodeId", nearestNodeId));
  }

  @GetMapping("/nodeIds")
  public ResponseEntity<Map<String, List<String>>> getNodeIds() {
    List<String> allCrossNodes = nodeService.getAllCrossNodes();
    return ResponseEntity.ok(Map.of("nodes", allCrossNodes));
  }
}
