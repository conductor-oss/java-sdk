package costoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RecommendWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "co_recommend";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [recommend] 5 optimization recommendations generated");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("recommend", true);
        result.addOutputData("processed", true);
        return result;
    }
}
