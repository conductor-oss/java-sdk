package inventoryoptimization.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
import java.util.stream.*;

public class CalculateReorderWorker implements Worker {
    @Override public String getTaskDefName() { return "io_calculate_reorder"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> levels = (List<Map<String, Object>>) task.getInputData().get("stockLevels");
        if (levels == null) levels = List.of();
        List<Map<String, Object>> reorderPlan = levels.stream()
            .filter(s -> ((Number)s.get("current")).intValue() < ((Number)s.get("dailyUsage")).intValue() * 14)
            .map(s -> Map.<String, Object>of("sku", s.get("sku"), "currentStock", s.get("current"), "reorderQty", ((Number)s.get("dailyUsage")).intValue() * 30))
            .collect(Collectors.toList());
        System.out.println("  [reorder] " + reorderPlan.size() + " SKUs need reordering");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reorderPlan", reorderPlan); r.getOutputData().put("reorderCount", reorderPlan.size());
        return r;
    }
}
