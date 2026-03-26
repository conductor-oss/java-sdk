package automatedtesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs unit tests.
 */
public class RunUnit implements Worker {

    @Override
    public String getTaskDefName() {
        return "at_run_unit";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("[at_run_unit] 247 tests passed");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("passed", 247);
        output.put("failed", 0);
        output.put("duration", 12);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
