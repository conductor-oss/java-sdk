package cateringmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class InquiryWorker implements Worker {
    @Override public String getTaskDefName() { return "cat_inquiry"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [inquiry] Catering inquiry from " + task.getInputData().get("clientName") + " for " + task.getInputData().get("guestCount") + " guests");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("requirements", Map.of("type", "corporate", "dietary", List.of("vegetarian", "gluten-free"), "service", "buffet"));
        return result;
    }
}
