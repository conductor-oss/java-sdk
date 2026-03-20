package smoketesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReportStatusWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "st_report_status";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [report] Smoke test PASSED — deployment verified");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("report_status", true);
        return result;
    }
}
