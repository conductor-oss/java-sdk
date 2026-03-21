package capacityplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RecommendWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cp_recommend";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [recommend] Add 3 nodes, $450/mo");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("action", "scale-up");
        result.addOutputData("nodes", 3);
        result.addOutputData("cost", 450);
        return result;
    }
}
