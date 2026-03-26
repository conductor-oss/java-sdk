package tutoringmatch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class StudentRequestWorker implements Worker {
    @Override public String getTaskDefName() { return "tut_student_request"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [request] " + task.getInputData().get("studentId") + " needs help with " + task.getInputData().get("subject"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestId", "REQ-679-001");
        return result;
    }
}
