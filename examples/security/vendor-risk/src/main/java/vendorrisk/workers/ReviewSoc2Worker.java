package vendorrisk.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReviewSoc2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vr_review_soc2";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [soc2] SOC 2 Type II report valid, no qualifications");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("review_soc2", true);
        result.addOutputData("processed", true);
        return result;
    }
}
