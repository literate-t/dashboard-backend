package dashboard.dto;

import java.util.List;

public record DispatchBulkRequest(List<DispatchRequest> requests) {

}
