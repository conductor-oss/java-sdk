package actuarialworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "act_report";
    }

    @Override
    public TaskResult execute(Task task) {

        String lineOfBusiness = (String) task.getInputData().get("lineOfBusiness");
        System.out.printf("  [report] Actuarial report generated for %s%n", lineOfBusiness);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reportId", "RPT-actuarial-workflow-001");
        result.getOutputData().put("generated", true);
        return result;
    }
}
