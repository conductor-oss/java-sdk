package projectclosure.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class ReviewDeliverablesWorker implements Worker {
    @Override public String getTaskDefName() { return "pcl_review_deliverables"; }

    @Override
    public TaskResult execute(Task task) {
        String projectId = (String) task.getInputData().get("projectId");
        System.out.println("  [Review Deliverables] Reviewing deliverables for project " + projectId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deliverables", List.of(
            Map.of("name", "Requirements Doc", "status", "complete"),
            Map.of("name", "Source Code", "status", "complete"),
            Map.of("name", "Test Reports", "status", "complete")
        ));
        result.getOutputData().put("allComplete", true);
        return result;
    }
}
