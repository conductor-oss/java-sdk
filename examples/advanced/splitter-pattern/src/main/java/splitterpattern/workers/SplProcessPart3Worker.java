package splitterpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SplProcessPart3Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "spl_process_part_3";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [part-3] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", java.util.Map.of("sku", "MONITOR-27", "subtotal", 449.99, "fulfilled", true));
        return result;
    }
}