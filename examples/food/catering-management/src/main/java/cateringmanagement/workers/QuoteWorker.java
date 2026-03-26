package cateringmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class QuoteWorker implements Worker {
    @Override public String getTaskDefName() { return "cat_quote"; }
    @Override public TaskResult execute(Task task) {
        int perGuest = 45;
        int guestCount = 100;
        Object gc = task.getInputData().get("guestCount");
        if (gc instanceof Number) guestCount = ((Number) gc).intValue();
        int total = perGuest * guestCount;
        System.out.println("  [quote] Quote: $" + total + " ($" + perGuest + "/guest)");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("total", total);
        result.addOutputData("budget", total);
        result.addOutputData("perGuest", perGuest);
        return result;
    }
}
