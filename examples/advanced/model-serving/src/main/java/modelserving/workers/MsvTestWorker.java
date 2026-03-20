package modelserving.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MsvTestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "msv_test";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [test] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("allPassed", true);
        result.getOutputData().put("latencyP50", 12);
        result.getOutputData().put("latencyP99", 45);
        return result;
    }
}