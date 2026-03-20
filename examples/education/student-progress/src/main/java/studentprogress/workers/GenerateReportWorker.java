package studentprogress.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateReportWorker implements Worker {
    @Override public String getTaskDefName() { return "spr_generate_report"; }

    @Override
    public TaskResult execute(Task task) {
        String studentId = (String) task.getInputData().get("studentId");
        System.out.println("  [report] Progress report generated for " + studentId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("generated", true);
        result.getOutputData().put("format", "PDF");
        return result;
    }
}
