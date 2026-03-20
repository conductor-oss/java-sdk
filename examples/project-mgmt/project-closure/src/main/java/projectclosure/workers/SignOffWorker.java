package projectclosure.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SignOffWorker implements Worker {
    @Override public String getTaskDefName() { return "pcl_sign_off"; }

    @Override
    public TaskResult execute(Task task) {
        String projectId = (String) task.getInputData().get("projectId");
        System.out.println("  [Sign Off] Obtaining sign-off for project " + projectId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("signOff", "approved");
        result.getOutputData().put("signedBy", "Project Sponsor");
        result.getOutputData().put("date", "2026-03-14");
        return result;
    }
}
