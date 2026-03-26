package insuranceunderwriting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class CollectAppWorker implements Worker {
    @Override public String getTaskDefName() { return "uw_collect_app"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Application " + task.getInputData().get("applicationId") + ": " + task.getInputData().get("coverageType") + ", $" + task.getInputData().get("coverageAmount"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("applicantData", Map.of("age", 35, "healthClass", "preferred", "smoker", false, "occupation", "software_engineer", "bmi", 24.5, "familyHistory", "no_significant"));
        return r;
    }
}
