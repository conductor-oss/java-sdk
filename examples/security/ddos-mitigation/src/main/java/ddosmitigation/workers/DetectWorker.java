package ddosmitigation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DetectWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ddos_detect";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [detect] api-gateway: traffic 500% above baseline");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("detectId", "DETECT-1353");
        result.addOutputData("success", true);
        return result;
    }
}
