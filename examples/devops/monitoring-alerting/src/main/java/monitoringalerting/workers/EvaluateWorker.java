package monitoringalerting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EvaluateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ma_evaluate";
    }

    @Override
    public TaskResult execute(Task task) {
        String alertName = (String) task.getInputData().get("alertName");
        System.out.println("  [evaluate] " + alertName + ": severity warning");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("severity", "warning");
        return result;
    }
}
