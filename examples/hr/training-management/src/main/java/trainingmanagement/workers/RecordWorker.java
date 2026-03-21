package trainingmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class RecordWorker implements Worker {
    @Override public String getTaskDefName() { return "trm_record"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [record] Certification " + task.getInputData().get("certificationId") + " added to " + task.getInputData().get("employeeId") + " profile");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("recorded", true);
        return r;
    }
}
