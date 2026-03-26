package parallelprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PprChunk3Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ppr_chunk_3";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [chunk-3] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", java.util.List.of(14,16,18));
        result.getOutputData().put("processed", 3);
        return result;
    }
}