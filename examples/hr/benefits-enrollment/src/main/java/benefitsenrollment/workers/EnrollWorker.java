package benefitsenrollment.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class EnrollWorker implements Worker {
    @Override public String getTaskDefName() { return "ben_enroll"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [enroll] " + task.getInputData().get("employeeId") + " enrolled in selected benefits");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("enrollmentId", "BEN-607");
        r.getOutputData().put("monthlyPremium", 485);
        r.getOutputData().put("effectiveDate", "2024-01-01");
        return r;
    }
}
