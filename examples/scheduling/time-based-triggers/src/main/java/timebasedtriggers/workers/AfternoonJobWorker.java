package timebasedtriggers.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AfternoonJobWorker implements Worker {
    @Override public String getTaskDefName() { return "tb_afternoon_job"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [afternoon] Running " + task.getInputData().get("jobType") + " in " + task.getInputData().get("timezone"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("executed", true);
        result.getOutputData().put("jobType", task.getInputData().get("jobType"));
        result.getOutputData().put("reportsGenerated", 12);
        return result;
    }
}
