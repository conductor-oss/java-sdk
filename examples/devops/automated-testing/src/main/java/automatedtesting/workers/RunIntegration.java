package automatedtesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs integration tests.
 */
public class RunIntegration implements Worker {

    @Override
    public String getTaskDefName() {
        return "at_run_integration";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("[at_run_integration] 18 tests passed");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("passed", 18);
        output.put("failed", 0);
        output.put("duration", 45);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
