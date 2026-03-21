package benefitdetermination.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ApplyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bnd_apply";
    }

    @Override
    public TaskResult execute(Task task) {
        String applicantId = (String) task.getInputData().get("applicantId");
        String programType = (String) task.getInputData().get("programType");
        System.out.printf("  [apply] Benefits application from %s for %s%n", applicantId, programType);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("applicationId", "APP-benefit-determination-001");
        return result;
    }
}
