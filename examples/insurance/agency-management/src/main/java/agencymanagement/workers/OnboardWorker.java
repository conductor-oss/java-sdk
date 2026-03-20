package agencymanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class OnboardWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agm_onboard";
    }

    @Override
    public TaskResult execute(Task task) {

        String agentId = (String) task.getInputData().get("agentId");
        System.out.printf("  [onboard] Agent %s onboarded%n", agentId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("onboarded", true);
        result.getOutputData().put("startDate", "2024-03-01");
        return result;
    }
}
