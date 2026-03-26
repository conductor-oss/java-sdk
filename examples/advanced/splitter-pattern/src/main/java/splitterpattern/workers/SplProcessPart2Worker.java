package splitterpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SplProcessPart2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "spl_process_part_2";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [part-2] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", java.util.Map.of("sku", "MOUSE-WL", "subtotal", 59.98, "fulfilled", true));
        return result;
    }
}