package servicemigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sm_assess";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [assess] payment-service: 3 dependencies, 2 data stores");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("assessId", "ASSESS-1338");
        result.addOutputData("success", true);
        return result;
    }
}
