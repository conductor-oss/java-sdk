package taskrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrtDispatchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "trt_dispatch";
    }

    @Override
    public TaskResult execute(Task task) {
        String pool = (String) task.getInputData().getOrDefault("selectedPool", "default");
        System.out.println("  [dispatch] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dispatchId", "DSP-" + Long.toString(System.currentTimeMillis(), 36));
        result.getOutputData().put("pool", pool);
        result.getOutputData().put("dispatched", true);
        return result;
    }
}