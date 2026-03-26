package parallelprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PprChunk2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ppr_chunk_2";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [chunk-2] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", java.util.List.of(8,10,12));
        result.getOutputData().put("processed", 3);
        return result;
    }
}