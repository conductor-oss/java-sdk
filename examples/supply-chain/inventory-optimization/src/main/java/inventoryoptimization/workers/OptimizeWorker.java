package inventoryoptimization.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
import java.util.stream.*;

public class OptimizeWorker implements Worker {
    @Override public String getTaskDefName() { return "io_optimize"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> plan = (List<Map<String, Object>>) task.getInputData().get("reorderPlan");
        if (plan == null) plan = List.of();
        List<Map<String, Object>> optimized = plan.stream()
            .map(p -> {
                Map<String, Object> m = new HashMap<>(p);
                m.put("reorderQty", (int) Math.ceil(((Number)p.get("reorderQty")).intValue() * 0.9));
                return m;
            }).collect(Collectors.toList());
        System.out.println("  [optimize] Optimized " + optimized.size() + " orders — 10% quantity reduction via bulk pricing");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("optimizedPlan", optimized); r.getOutputData().put("costSavings", "12%");
        return r;
    }
}
