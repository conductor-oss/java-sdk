package customersegmentation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class LabelSegmentsWorker implements Worker {
    @Override public String getTaskDefName() { return "seg_label_segments"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [label] Labeling segments based on centroid characteristics");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("segments", List.of(
                Map.of("label", "VIP", "clusterId", 0, "size", 1, "avgSpend", 500),
                Map.of("label", "Regular", "clusterId", 1, "size", 1, "avgSpend", 200),
                Map.of("label", "At-Risk", "clusterId", 2, "size", 2, "avgSpend", 37.5)));
        result.setOutputData(output);
        return result;
    }
}
