package gradingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NotifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "grd_notify";
    }

    @Override
    public TaskResult execute(Task task) {
        String studentId = (String) task.getInputData().get("studentId");
        Object finalScore = task.getInputData().get("finalScore");

        System.out.println("  [notify] Student " + studentId + " notified - score: " + finalScore);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        result.getOutputData().put("method", "email");
        return result;
    }
}
