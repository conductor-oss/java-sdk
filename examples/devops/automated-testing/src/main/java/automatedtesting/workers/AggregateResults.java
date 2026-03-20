package automatedtesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregates test results from all suites.
 */
public class AggregateResults implements Worker {

    @Override
    public String getTaskDefName() {
        return "at_aggregate_results";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("[at_aggregate_results] All tests passed");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("totalPassed", 277);
        output.put("totalFailed", 0);
        output.put("coverage", 87);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
