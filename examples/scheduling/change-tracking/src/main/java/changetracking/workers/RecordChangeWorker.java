package changetracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class RecordChangeWorker implements Worker {
    @Override public String getTaskDefName() { return "chg_record"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [record] Recording " + task.getInputData().get("changeType") + " change (risk: " + task.getInputData().get("riskLevel") + ")");
        r.getOutputData().put("recorded", true);
        r.getOutputData().put("changeId", "CHG-20260308-001");
        return r;
    }
}
