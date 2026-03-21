package emergencyresponse.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DetectWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "emr_detect";
    }

    @Override
    public TaskResult execute(Task task) {
        String incidentType = (String) task.getInputData().get("incidentType");
        String location = (String) task.getInputData().get("location");
        System.out.printf("  [detect] Incident detected: %s at %s%n", incidentType, location);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("incidentId", "INC-emergency-response-001");
        result.getOutputData().put("detectedAt", "2024-03-10T14:30:00Z");
        return result;
    }
}
