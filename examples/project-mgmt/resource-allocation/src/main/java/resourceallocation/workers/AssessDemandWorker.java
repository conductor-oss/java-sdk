package resourceallocation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AssessDemandWorker implements Worker {
    @Override public String getTaskDefName() { return "ral_assess_demand"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [demand] Project " + task.getInputData().get("projectId") + " needs " + task.getInputData().get("hoursNeeded") + "h");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("demand", Map.of("hours",task.getInputData().get("hoursNeeded"),"priority","high","startDate","2026-03-10")); return r;
    }
}
