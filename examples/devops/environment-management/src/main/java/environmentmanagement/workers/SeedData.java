package environmentmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds test data into the environment.
 */
public class SeedData implements Worker {

    @Override
    public String getTaskDefName() {
        return "em_seed_data";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("[em_seed_data] Seeded 1000 test records");

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("seeded", true);
        output.put("records", 1000);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
