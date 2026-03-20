package plagiarismdetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SubmitWorker implements Worker {
    @Override public String getTaskDefName() { return "plg_submit"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [submit] " + task.getInputData().get("studentId") + " submitted " + task.getInputData().get("assignmentId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("submissionId", "SUB-678-001");
        return result;
    }
}
