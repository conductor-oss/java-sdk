package featurestore.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class FstRegisterWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fst_register";
    }

    @Override
    public TaskResult execute(Task task) {
        String fgName = (String) task.getInputData().getOrDefault("featureGroupName", "fg");
        System.out.println("  [register] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("registryId", "FG-" + fgName + "-" + System.currentTimeMillis());
        result.getOutputData().put("registered", true);
        return result;
    }
}