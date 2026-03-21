package inspectionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RecordFailWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "inw_record_fail";
    }

    @Override
    public TaskResult execute(Task task) {
        String propertyId = (String) task.getInputData().get("propertyId");
        System.out.printf("  [fail] Property %s failed inspection%n", propertyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reinspectionRequired", true);
        return result;
    }
}
