package capacityplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AnalyzeTrendsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cp_analyze_trends";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [analyze] 15% month-over-month growth");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("trend", "growing");
        result.addOutputData("rate", 15);
        return result;
    }
}
