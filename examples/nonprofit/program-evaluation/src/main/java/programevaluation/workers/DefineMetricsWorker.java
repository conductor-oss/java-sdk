package programevaluation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class DefineMetricsWorker implements Worker {
    @Override public String getTaskDefName() { return "pev_define_metrics"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [metrics] Defining evaluation metrics for " + task.getInputData().get("programName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("metrics", List.of("reach","outcomes","cost-effectiveness","satisfaction")); return r;
    }
}
