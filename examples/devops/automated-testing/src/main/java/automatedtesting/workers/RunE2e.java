package automatedtesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs end-to-end tests.
 */
public class RunE2e implements Worker {

    @Override
    public String getTaskDefName() {
        return "at_run_e2e";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("[at_run_e2e] 12 tests passed");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("passed", 12);
        output.put("failed", 0);
        output.put("duration", 120);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
