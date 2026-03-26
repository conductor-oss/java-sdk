package riskmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class IdentifyWorker implements Worker {
    @Override public String getTaskDefName() { return "rkm_identify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [identify] Risk found in project " + task.getInputData().get("projectId") + ": " + task.getInputData().get("description"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("risk", Map.of("id","RSK-001","description",String.valueOf(task.getInputData().get("description")),"category","technical")); return r;
    }
}
