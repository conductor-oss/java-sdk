package riskmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List; import java.util.Map;
public class MitigateWorker implements Worker {
    @Override public String getTaskDefName() { return "rkm_mitigate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [mitigate] Creating mitigation plan for " + task.getInputData().get("severity") + " risk");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("plan", Map.of("severity",String.valueOf(task.getInputData().get("severity")),"actions",List.of("Assign owner","Set deadline","Review weekly"),"status","ACTIVE")); return r;
    }
}
