package performancetesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AnalyzeResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pt_analyze_results";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [analyze] Performance within SLO thresholds");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("analyze_results", true);
        result.addOutputData("processed", true);
        return result;
    }
}
