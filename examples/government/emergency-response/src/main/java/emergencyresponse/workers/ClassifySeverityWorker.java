package emergencyresponse.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ClassifySeverityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "emr_classify_severity";
    }

    @Override
    public TaskResult execute(Task task) {
        String incidentId = (String) task.getInputData().get("incidentId");
        System.out.printf("  [classify] Incident %s classified as high severity%n", incidentId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("severity", "high");
        result.getOutputData().put("responseLevel", 3);
        return result;
    }
}
