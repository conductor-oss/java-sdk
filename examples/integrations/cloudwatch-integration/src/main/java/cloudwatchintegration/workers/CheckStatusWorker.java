package cloudwatchintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks CloudWatch alarm status.
 * Input: alarmName, currentValue, threshold
 * Output: state, stateReason
 */
public class CheckStatusWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cw_check_status";
    }

    @Override
    public TaskResult execute(Task task) {
        String alarmName = (String) task.getInputData().get("alarmName");
        double currentVal = 0;
        double thresholdVal = 0;
        try {
            currentVal = Double.parseDouble(String.valueOf(task.getInputData().get("currentValue")));
            thresholdVal = Double.parseDouble(String.valueOf(task.getInputData().get("threshold")));
        } catch (NumberFormatException e) { /* ignore */ }
        String state = currentVal > thresholdVal ? "ALARM" : "OK";
        String reason = "Current value " + currentVal + (state.equals("ALARM") ? " exceeds" : " is below") + " threshold " + thresholdVal;
        System.out.println("  [check] Alarm \"" + alarmName + "\": value=" + currentVal + ", threshold=" + thresholdVal + " -> " + state);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("state", "" + state);
        result.getOutputData().put("stateReason", "" + reason);
        return result;
    }
}
