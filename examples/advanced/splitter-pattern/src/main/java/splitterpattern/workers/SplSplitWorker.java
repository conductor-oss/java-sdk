package splitterpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SplSplitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "spl_split";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [split] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("parts", java.util.List.of(java.util.Map.of("sku","LAPTOP-15","quantity",1,"price",1299.99), java.util.Map.of("sku","MOUSE-WL","quantity",2,"price",29.99), java.util.Map.of("sku","MONITOR-27","quantity",1,"price",449.99)));
        result.getOutputData().put("partCount", 3);
        return result;
    }
}