package qualityinspection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
import java.util.stream.*;

public class SampleWorker implements Worker {
    @Override public String getTaskDefName() { return "qi_sample"; }

    @Override
    public TaskResult execute(Task task) {
        Object sizeObj = task.getInputData().get("sampleSize");
        int size = sizeObj instanceof Number ? ((Number) sizeObj).intValue() : 10;
        String batchId = (String) task.getInputData().get("batchId");

        List<Map<String, Object>> samples = IntStream.rangeClosed(1, size)
                .mapToObj(i -> Map.<String, Object>of("id", "S-" + i, "unit", i))
                .collect(Collectors.toList());

        System.out.println("  [sample] Pulled " + size + " samples from batch " + batchId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("samples", samples);
        result.getOutputData().put("count", size);
        return result;
    }
}
