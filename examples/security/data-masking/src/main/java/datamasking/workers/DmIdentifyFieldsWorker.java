package datamasking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Identifies sensitive fields in the data source.
 * Input: dataSource, purpose
 * Output: identify_fieldsId, success
 */
public class DmIdentifyFieldsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dm_identify_fields";
    }

    @Override
    public TaskResult execute(Task task) {
        String dataSource = (String) task.getInputData().get("dataSource");
        if (dataSource == null) dataSource = "unknown";

        System.out.println("  [identify] " + dataSource + ": 18 sensitive fields detected");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("identify_fieldsId", "IDENTIFY_FIELDS-1392");
        result.getOutputData().put("success", true);
        return result;
    }
}
