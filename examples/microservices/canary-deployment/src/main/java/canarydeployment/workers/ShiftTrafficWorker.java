package canarydeployment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ShiftTrafficWorker implements Worker {

    @Override public String getTaskDefName() { return "cd_shift_traffic"; }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        if (serviceName == null) serviceName = "unknown-service";

        Object pctObj = task.getInputData().get("percentage");
        int percentage = 10;
        if (pctObj instanceof Number) percentage = ((Number) pctObj).intValue();
        else if (pctObj instanceof String) { try { percentage = Integer.parseInt((String) pctObj); } catch (NumberFormatException ignored) {} }

        System.out.println("  [traffic] Shifted " + percentage + "% to canary");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("shifted", true);
        result.getOutputData().put("percentage", percentage);
        return result;
    }
}
