package featurestore.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class FstServeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fst_serve";
    }

    @Override
    public TaskResult execute(Task task) {
        String fgName = (String) task.getInputData().getOrDefault("featureGroupName", "fg");
        System.out.println("  [serve] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("serving", true);
        result.getOutputData().put("endpoint", "feast://" + fgName);
        return result;
    }
}