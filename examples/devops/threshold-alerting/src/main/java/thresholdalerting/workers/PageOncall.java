package thresholdalerting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Pages the on-call engineer for critical alerts.
 */
public class PageOncall implements Worker {

    @Override
    public String getTaskDefName() {
        return "th_page_oncall";
    }

    @Override
    public TaskResult execute(Task task) {
        String metricName = (String) task.getInputData().get("metricName");
        Object currentValue = task.getInputData().get("currentValue");

        System.out.println("[th_page_oncall] CRITICAL: Paging on-call for " + metricName + " = " + currentValue);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("paged", true);
        result.getOutputData().put("channel", "pagerduty");
        result.getOutputData().put("oncallEngineer", "eng-oncall-1");
        result.getOutputData().put("incidentId", "INC-20260308-001");
        return result;
    }
}
