package inspectionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

public class InspectWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "inw_inspect";
    }

    @Override
    public TaskResult execute(Task task) {
        String propertyId = (String) task.getInputData().get("propertyId");
        System.out.printf("  [inspect] On-site inspection of %s%n", propertyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("findings", Map.of(
                "structural", "good",
                "electrical", "good",
                "plumbing", "good"
        ));
        return result;
    }
}
