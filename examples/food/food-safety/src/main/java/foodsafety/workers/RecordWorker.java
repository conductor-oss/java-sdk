package foodsafety.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class RecordWorker implements Worker {
    @Override public String getTaskDefName() { return "fsf_record"; }
    @Override public TaskResult execute(Task task) {
        String restaurantId = (String) task.getInputData().get("restaurantId");
        System.out.println("  [record] Recording inspection results for " + restaurantId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("report", Map.of("restaurantId", restaurantId != null ? restaurantId : "REST-10", "grade", "A", "score", 98, "status", "CERTIFIED"));
        return result;
    }
}
