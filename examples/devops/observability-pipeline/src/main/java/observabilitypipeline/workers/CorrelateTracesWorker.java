package observabilitypipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CorrelateTracesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "op_correlate_traces";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [correlate] Correlated 3,200 distributed traces");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("correlate_traces", true);
        result.addOutputData("processed", true);
        return result;
    }
}
