package modelregistry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MrgDeployWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mrg_deploy";
    }

    @Override
    public TaskResult execute(Task task) {
        Object app = task.getInputData().get("approved");
        boolean approved = Boolean.TRUE.equals(app) || "true".equals(String.valueOf(app));
        System.out.println("  [deploy] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deployed", approved);
        return result;
    }
}