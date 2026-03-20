package parallelprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PprChunk1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ppr_chunk_1";
    }

    @Override
    public TaskResult execute(Task task) {
        Object chunk = task.getInputData().getOrDefault("chunk", java.util.List.of());
        System.out.println("  [chunk-1] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", java.util.List.of(2,4,6));
        result.getOutputData().put("processed", 3);
        return result;
    }
}