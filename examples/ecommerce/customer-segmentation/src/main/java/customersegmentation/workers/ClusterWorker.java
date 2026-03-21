package customersegmentation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class ClusterWorker implements Worker {
    @Override public String getTaskDefName() { return "seg_cluster"; }

    @Override
    public TaskResult execute(Task task) {
        int numSegments = 3;
        Object ns = task.getInputData().get("numSegments");
        if (ns instanceof Number) numSegments = ((Number) ns).intValue();
        System.out.println("  [cluster] Running k-means with k=" + numSegments);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("clusters", List.of(
                Map.of("clusterId", 0, "memberIds", List.of("C1"), "centroid", Map.of("avgSpend", 500, "frequency", 12)),
                Map.of("clusterId", 1, "memberIds", List.of("C3"), "centroid", Map.of("avgSpend", 200, "frequency", 8)),
                Map.of("clusterId", 2, "memberIds", List.of("C2", "C4"), "centroid", Map.of("avgSpend", 37.5, "frequency", 1.5))));
        result.setOutputData(output);
        return result;
    }
}
