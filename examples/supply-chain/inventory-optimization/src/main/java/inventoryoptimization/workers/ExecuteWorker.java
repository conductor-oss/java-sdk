package inventoryoptimization.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class ExecuteWorker implements Worker {
    @Override public String getTaskDefName() { return "io_execute"; }
    @Override public TaskResult execute(Task task) {
        List<?> optimizedPlan = (List<?>) task.getInputData().get("optimizedPlan");
        int orders = optimizedPlan != null ? optimizedPlan.size() : 0;
        System.out.println("  [execute] Placed " + orders + " purchase orders");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("ordersPlaced", orders); r.getOutputData().put("status", "submitted");
        return r;
    }
}
