package orderedprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class OprSortBySequenceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "opr_sort_by_sequence";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [sort] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sortedMessages", task.getInputData().getOrDefault("receivedMessages", java.util.List.of()));
        result.getOutputData().put("expectedOrder", java.util.List.of(1, 2, 3, 4, 5));
        return result;
    }
}