package taskpriority.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TprClassifyPriorityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tpr_classify_priority";
    }

    @Override
    public TaskResult execute(Task task) {
        String urgency = (String) task.getInputData().getOrDefault("urgency", "medium");
        String impact = (String) task.getInputData().getOrDefault("impact", "medium");
        String priority = "medium";
        int slaMinutes = 60;
        if ("high".equals(urgency) && "high".equals(impact)) { priority = "high"; slaMinutes = 15; }
        else if ("low".equals(urgency) && "low".equals(impact)) { priority = "low"; slaMinutes = 240; }
        System.out.println("  [classify] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("priority", priority);
        result.getOutputData().put("slaMinutes", slaMinutes);
        return result;
    }
}