package sequentialtasks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Load phase of the ETL pipeline.
 * Takes transformed data, prints each record, and returns load summary.
 */
public class LoadWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "seq_load";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> transformedData =
                (List<Map<String, Object>>) task.getInputData().get("transformedData");

        System.out.println("  [seq_load] Loading " + transformedData.size() + " records:");
        for (Map<String, Object> record : transformedData) {
            System.out.println("    -> " + record.get("name")
                    + " | score=" + record.get("score")
                    + " | grade=" + record.get("grade")
                    + " | normalized=" + record.get("normalizedScore"));
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("loaded", true);
        result.getOutputData().put("totalRecords", transformedData.size());
        return result;
    }
}
