package parallelprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PprSplitWorkWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ppr_split_work";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [split] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("chunks", java.util.List.of(java.util.List.of(1,2,3), java.util.List.of(4,5,6), java.util.List.of(7,8,9)));
        result.getOutputData().put("totalChunks", 3);
        return result;
    }
}