package securityposture.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CalculateScoreWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sp_calculate_score";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [score] Overall security posture: B, 85/100");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("calculate_score", true);
        return result;
    }
}
