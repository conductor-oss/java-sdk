package customersegmentation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class CollectDataWorker implements Worker {
    @Override public String getTaskDefName() { return "seg_collect_data"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [collect] Loading dataset " + task.getInputData().get("datasetId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("customers", List.of(
                Map.of("id", "C1", "avgSpend", 500, "frequency", 12, "recency", 5),
                Map.of("id", "C2", "avgSpend", 50, "frequency", 2, "recency", 60),
                Map.of("id", "C3", "avgSpend", 200, "frequency", 8, "recency", 10),
                Map.of("id", "C4", "avgSpend", 25, "frequency", 1, "recency", 180)));
        output.put("totalCustomers", 4);
        result.setOutputData(output);
        return result;
    }
}
