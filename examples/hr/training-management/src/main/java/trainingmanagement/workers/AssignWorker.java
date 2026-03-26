package trainingmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AssignWorker implements Worker {
    @Override public String getTaskDefName() { return "trm_assign"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [assign] " + task.getInputData().get("employeeId") + " assigned to course " + task.getInputData().get("courseId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("enrollmentId", "ENR-700");
        r.getOutputData().put("dueDate", "2024-04-30");
        return r;
    }
}
