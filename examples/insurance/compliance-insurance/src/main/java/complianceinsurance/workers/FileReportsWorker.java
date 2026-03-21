package complianceinsurance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class FileReportsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cpi_file_reports";
    }

    @Override
    public TaskResult execute(Task task) {

        String regulatoryBody = (String) task.getInputData().get("regulatoryBody");
        System.out.printf("  [file] Reports filed with %s%n", regulatoryBody);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("filingId", "FIL-compliance-insurance-001");
        result.getOutputData().put("filed", true);
        return result;
    }
}
