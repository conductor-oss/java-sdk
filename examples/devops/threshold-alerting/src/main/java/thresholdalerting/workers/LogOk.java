package thresholdalerting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Logs that the metric is within normal range.
 */
public class LogOk implements Worker {

    @Override
    public String getTaskDefName() {
        return "th_log_ok";
    }

    @Override
    public TaskResult execute(Task task) {
        String metricName = (String) task.getInputData().get("metricName");
        Object currentValue = task.getInputData().get("currentValue");

        System.out.println("[th_log_ok] " + metricName + " = " + currentValue + " -- within normal range");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        result.getOutputData().put("status", "healthy");
        return result;
    }
}
