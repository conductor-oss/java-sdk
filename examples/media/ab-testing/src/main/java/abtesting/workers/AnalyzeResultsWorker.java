package abtesting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AnalyzeResultsWorker implements Worker {
    @Override public String getTaskDefName() { return "abt_analyze_results"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [analyze] Processing " + task.getInputData().getOrDefault("pValue", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("pValue", 0.032);
        r.getOutputData().put("uplift", 21.6);
        r.getOutputData().put("confidence", 96.8);
        r.getOutputData().put("statisticallySignificant", true);
        r.getOutputData().put("effectSize", 0.54);
        return r;
    }
}
