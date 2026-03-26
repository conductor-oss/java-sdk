package warehousemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
import java.util.stream.*;

public class PutAwayWorker implements Worker {
    @Override public String getTaskDefName() { return "wm_put_away"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) task.getInputData().get("receivedItems");
        final List<Map<String, Object>> items = rawItems != null ? rawItems : List.of();
        List<Map<String, Object>> locations = IntStream.range(0, items.size())
                .mapToObj(i -> Map.<String, Object>of("sku", items.get(i).getOrDefault("sku", ""), "location", "A" + (i + 1) + "-R3-S" + (i + 5)))
                .collect(Collectors.toList());
        System.out.println("  [put-away] Stored " + items.size() + " items in designated locations");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("locations", locations);
        return r;
    }
}
