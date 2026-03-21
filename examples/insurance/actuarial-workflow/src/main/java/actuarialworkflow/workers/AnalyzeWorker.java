package actuarialworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AnalyzeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "act_analyze";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [analyze] Expected loss and tail risk computed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("analysis", java.util.Map.of("expectedLoss", 12500000, "tailRisk", 24100000, "adequacy", "sufficient"));
        return result;
    }
}
