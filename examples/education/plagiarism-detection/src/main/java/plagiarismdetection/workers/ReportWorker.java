package plagiarismdetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "plg_report"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Plagiarism report for " + task.getInputData().get("studentId") + ": " + task.getInputData().get("verdict"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("generated", true);
        return result;
    }
}
