package beneficiarytracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class ProvideServicesWorker implements Worker {
    @Override public String getTaskDefName() { return "btr_provide_services"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [services] Providing services for " + task.getInputData().get("beneficiaryId") + " in program " + task.getInputData().get("programId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("services", List.of("food-assistance","health-screening","tutoring")); r.addOutputData("sessionsCompleted", 6); return r;
    }
}
