package demandforecasting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class PlanWorker implements Worker {
    @Override public String getTaskDefName() { return "df_plan"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        Map<String, Object> f = (Map<String, Object>) task.getInputData().get("forecast");
        int total = f != null && f.get("total") instanceof Number ? ((Number)f.get("total")).intValue() : 10000;
        Map<String, Object> plan = Map.of("reorderPoint", 2000, "safetyStock", 500, "orderQty", total);
        System.out.println("  [plan] Reorder at 2000 units, safety stock: 500");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("plan", plan);
        return r;
    }
}
