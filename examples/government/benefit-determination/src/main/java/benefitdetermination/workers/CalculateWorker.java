package benefitdetermination.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CalculateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bnd_calculate";
    }

    @Override
    public TaskResult execute(Task task) {
        String eligibility = (String) task.getInputData().get("eligibility");
        int amount = "eligible".equals(eligibility) ? 850 : 0;
        System.out.printf("  [calculate] Monthly benefit: $%d%n", amount);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("benefitAmount", amount);
        return result;
    }
}
