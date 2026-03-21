package multicluster.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MclPrepareWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mcl_prepare";
    }

    @Override
    public TaskResult execute(Task task) {
        int size = task.getInputData().get("datasetSize") instanceof Number ? ((Number) task.getInputData().get("datasetSize")).intValue() : 100000;
        int half = size / 2;
        System.out.println("  [prepare] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("partitionA", "records_1_to_" + half);
        result.getOutputData().put("partitionB", "records_" + (half + 1) + "_to_" + size);
        return result;
    }
}