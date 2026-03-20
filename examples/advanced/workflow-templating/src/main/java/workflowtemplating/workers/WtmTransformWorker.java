package workflowtemplating.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WtmTransformWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wtm_transform";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [transform] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformed", "transformed_data");
        return result;
    }
}