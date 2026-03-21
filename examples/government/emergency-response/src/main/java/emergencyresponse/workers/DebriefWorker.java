package emergencyresponse.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DebriefWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "emr_debrief";
    }

    @Override
    public TaskResult execute(Task task) {
        String incidentId = (String) task.getInputData().get("incidentId");
        String outcome = (String) task.getInputData().get("outcome");
        System.out.printf("  [debrief] Incident %s debriefed — %s%n", incidentId, outcome);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reportFiled", true);
        result.getOutputData().put("lessonsLearned", 3);
        return result;
    }
}
