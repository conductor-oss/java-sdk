package competingconsumers.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CcsProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ccs_process";
    }

    @Override
    public TaskResult execute(Task task) {
        String winner = (String) task.getInputData().getOrDefault("winner", "consumer-1");
        System.out.println("  [process] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "processed_by_" + winner);
        result.getOutputData().put("processingTimeMs", 120);
        return result;
    }
}