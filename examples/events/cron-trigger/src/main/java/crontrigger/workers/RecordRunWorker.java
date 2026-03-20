package crontrigger.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Records a completed cron job run.
 * Input: jobName, result
 * Output: recorded (true), runId
 *
 * Deterministic: always returns a fixed runId.
 */
public class RecordRunWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cn_record_run";
    }

    @Override
    public TaskResult execute(Task task) {
        String jobName = (String) task.getInputData().get("jobName");
        if (jobName == null) {
            jobName = "unknown";
        }

        String jobResult = (String) task.getInputData().get("result");
        if (jobResult == null) {
            jobResult = "unknown";
        }

        System.out.println("  [cn_record_run] Job \"" + jobName
                + "\" completed with result: " + jobResult);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recorded", true);
        result.getOutputData().put("runId", "run_fixed_001");
        return result;
    }
}
