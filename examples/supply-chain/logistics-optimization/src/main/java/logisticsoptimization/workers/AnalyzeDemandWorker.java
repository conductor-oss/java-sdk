package logisticsoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class AnalyzeDemandWorker implements Worker {
    @Override public String getTaskDefName() { return "lo_analyze_demand"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<?> orders = (List<?>) task.getInputData().get("orders");
        if (orders == null) orders = List.of();
        Map<String, Integer> demandMap = Map.of("north", 12, "south", 8, "east", 15, "west", 5);
        System.out.println("  [demand] Analyzed " + orders.size() + " orders in " + task.getInputData().get("region"));
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("demandMap", demandMap);
        r.getOutputData().put("orderCount", orders.size());
        return r;
    }
}
