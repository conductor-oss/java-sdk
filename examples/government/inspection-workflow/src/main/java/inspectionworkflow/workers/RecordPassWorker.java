package inspectionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RecordPassWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "inw_record_pass";
    }

    @Override
    public TaskResult execute(Task task) {
        String propertyId = (String) task.getInputData().get("propertyId");
        System.out.printf("  [pass] Property %s passed inspection%n", propertyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("certificate", "CERT-inspection-workflow-001");
        return result;
    }
}
