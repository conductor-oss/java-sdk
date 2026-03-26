package performancetesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pt_generate_report";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [report] Performance report generated with trend analysis");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("generate_report", true);
        return result;
    }
}
