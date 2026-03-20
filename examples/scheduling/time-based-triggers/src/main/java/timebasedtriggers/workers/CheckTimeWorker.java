package timebasedtriggers.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckTimeWorker implements Worker {
    @Override public String getTaskDefName() { return "tb_check_time"; }

    @Override
    public TaskResult execute(Task task) {
        int hour = 9;
        try { hour = Integer.parseInt(String.valueOf(task.getInputData().get("currentHour"))); } catch (Exception ignored) {}
        String timezone = (String) task.getInputData().getOrDefault("timezone", "UTC");
        String timeWindow = "evening";
        if (hour >= 6 && hour < 12) timeWindow = "morning";
        else if (hour >= 12 && hour < 18) timeWindow = "afternoon";
        System.out.println("  [check-time] Hour: " + hour + " in " + timezone + " -> window: " + timeWindow);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("timeWindow", timeWindow);
        result.getOutputData().put("localTime", hour + ":00");
        result.getOutputData().put("timezone", timezone);
        return result;
    }
}
