package eventsplit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Combines results from all three parallel sub-event processors.
 * Input: resultA, resultB, resultC
 * Output: status ("all_sub_events_processed"), results ([resultA, resultB, resultC])
 */
public class CombineResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sp_combine_results";
    }

    @Override
    public TaskResult execute(Task task) {
        Object resultA = task.getInputData().get("resultA");
        Object resultB = task.getInputData().get("resultB");
        Object resultC = task.getInputData().get("resultC");

        List<Object> results = List.of(
                resultA != null ? resultA : "unknown",
                resultB != null ? resultB : "unknown",
                resultC != null ? resultC : "unknown"
        );

        System.out.println("  [sp_combine_results] Combined results: " + results);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "all_sub_events_processed");
        result.getOutputData().put("results", results);
        return result;
    }
}
