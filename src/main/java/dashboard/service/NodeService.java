package dashboard.service;

import dashboard.manager.GraphMemoryManager;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NodeService {

  private final GraphMemoryManager graphMemoryManager;

  public NodeService(GraphMemoryManager graphMemoryManager) {
    this.graphMemoryManager = graphMemoryManager;
  }

  public List<String> getAllCrossNodes() {
    List<String> allCrossNodes = new ArrayList<>();
    graphMemoryManager.getNodeMap().keySet().parallelStream().forEach(allCrossNodes::add);

    return allCrossNodes;
  }
}
