package modelserving.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MsvPromoteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "msv_promote";
    }

    @Override
    public TaskResult execute(Task task) {
        Object tp = task.getInputData().get("testsPassed");
        boolean passed = Boolean.TRUE.equals(tp) || "true".equals(String.valueOf(tp));
        System.out.println("  [promote] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("promoted", passed);
        return result;
    }
}