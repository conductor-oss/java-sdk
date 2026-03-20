package emergencyresponse.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CoordinateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "emr_coordinate";
    }

    @Override
    public TaskResult execute(Task task) {
        String incidentId = (String) task.getInputData().get("incidentId");
        System.out.printf("  [coordinate] Coordinating response for %s%n", incidentId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("outcome", "contained");
        result.getOutputData().put("duration", "45 minutes");
        result.getOutputData().put("casualties", 0);
        return result;
    }
}
