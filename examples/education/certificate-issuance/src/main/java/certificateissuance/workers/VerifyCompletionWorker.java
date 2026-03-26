package certificateissuance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyCompletionWorker implements Worker {
    @Override public String getTaskDefName() { return "cer_verify_completion"; }

    @Override
    public TaskResult execute(Task task) {
        String studentId = (String) task.getInputData().get("studentId");
        String courseId = (String) task.getInputData().get("courseId");
        System.out.println("  [verify] Student " + studentId + " completed " + courseId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("completed", true);
        result.getOutputData().put("completionDate", "2024-05-15");
        result.getOutputData().put("grade", "A");
        return result;
    }
}
