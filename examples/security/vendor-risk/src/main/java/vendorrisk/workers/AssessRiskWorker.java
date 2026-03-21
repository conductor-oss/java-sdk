package vendorrisk.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssessRiskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vr_assess_risk";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [risk] Risk score: 72/100 — medium risk, data encryption gaps identified");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("assess_risk", true);
        result.addOutputData("processed", true);
        return result;
    }
}
