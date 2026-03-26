package compliancescanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cs_generate_report";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [report] Compliance report generated");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("generate_report", true);
        result.addOutputData("processed", true);
        return result;
    }
}
