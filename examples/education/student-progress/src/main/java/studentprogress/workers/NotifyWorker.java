package studentprogress.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class NotifyWorker implements Worker {
    @Override public String getTaskDefName() { return "spr_notify"; }

    @Override
    public TaskResult execute(Task task) {
        String studentId = (String) task.getInputData().get("studentId");
        Object gpa = task.getInputData().get("gpa");
        System.out.println("  [notify] Student " + studentId + " notified - GPA: " + gpa);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        result.getOutputData().put("recipients", List.of("student", "advisor"));
        return result;
    }
}
