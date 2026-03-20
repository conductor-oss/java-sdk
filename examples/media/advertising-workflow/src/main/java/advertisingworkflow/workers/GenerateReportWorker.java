package advertisingworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateReportWorker implements Worker {
    @Override public String getTaskDefName() { return "adv_generate_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Processing " + task.getInputData().getOrDefault("reportUrl", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reportUrl", "https://ads.example.com/reports/530");
        return r;
    }
}
