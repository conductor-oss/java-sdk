package featureflagrollout.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EnableSegmentWorker implements Worker {
    @Override public String getTaskDefName() { return "ff_enable_segment"; }

    @Override
    public TaskResult execute(Task task) {
        String flagName = (String) task.getInputData().get("flagName");
        if (flagName == null) flagName = "unknown-flag";
        String segment = (String) task.getInputData().get("segment");
        if (segment == null) segment = "all-users";

        Object pctObj = task.getInputData().get("percentage");
        int percentage = 10;
        if (pctObj instanceof Number) percentage = ((Number) pctObj).intValue();
        else if (pctObj instanceof String) { try { percentage = Integer.parseInt((String) pctObj); } catch (NumberFormatException ignored) {} }

        System.out.println("  [segment] Enabled for " + segment + " at " + percentage + "%");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enabled", true);
        result.getOutputData().put("segment", segment);
        result.getOutputData().put("percentage", percentage);
        return result;
    }
}
