package ticketmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ClassifyTicketWorker implements Worker {
    @Override public String getTaskDefName() { return "tkt_classify"; }

    @Override
    public TaskResult execute(Task task) {
        String desc = ((String) task.getInputData().getOrDefault("description", "")).toLowerCase();
        String category = desc.contains("login") ? "authentication" : desc.contains("slow") ? "performance" : "general";
        String priority = (desc.contains("cannot") || desc.contains("urgent")) ? "P1" : "P2";
        System.out.println("  [classify] " + task.getInputData().get("ticketId") + ": category=" + category + ", priority=" + priority);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("category", category);
        result.getOutputData().put("priority", priority);
        return result;
    }
}
