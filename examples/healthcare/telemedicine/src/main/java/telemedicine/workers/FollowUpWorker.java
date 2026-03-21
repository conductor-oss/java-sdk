package telemedicine.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class FollowUpWorker implements Worker {

    @Override
    public String getTaskDefName() { return "tlm_followup"; }

    @Override
    public TaskResult execute(Task task) {
        Object raw = task.getInputData().get("followUpNeeded");
        boolean needed = Boolean.TRUE.equals(raw) || "true".equals(String.valueOf(raw));
        String followUpDate = needed ? "2024-03-25" : null;
        System.out.println("  [follow-up] " + (needed ? "Scheduled for " + followUpDate : "Not needed"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("followUpDate", followUpDate);
        output.put("followUpScheduled", needed);
        result.setOutputData(output);
        return result;
    }
}
