package gradingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SubmitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "grd_submit";
    }

    @Override
    public TaskResult execute(Task task) {
        String studentId = (String) task.getInputData().get("studentId");
        String assignmentId = (String) task.getInputData().get("assignmentId");

        System.out.println("  [submit] Student " + studentId + " submitted " + assignmentId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("submissionId", "SUB-673-001");
        result.getOutputData().put("onTime", true);
        return result;
    }
}
