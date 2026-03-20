package roamingmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ValidateAgreementWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rmg_validate_agreement";
    }

    @Override
    public TaskResult execute(Task task) {

        String homeNetwork = (String) task.getInputData().get("homeNetwork");
        String visitedNetwork = (String) task.getInputData().get("visitedNetwork");
        System.out.printf("  [validate] Roaming agreement between %s and %s valid%n", homeNetwork, visitedNetwork);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("agreement", java.util.Map.of("type", "bilateral", "rateClass", "EU-standard"));
        return result;
    }
}
