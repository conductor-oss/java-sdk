package certificateissuance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateCertificateWorker implements Worker {
    @Override public String getTaskDefName() { return "cer_generate"; }

    @Override
    public TaskResult execute(Task task) {
        String courseName = (String) task.getInputData().get("courseName");
        System.out.println("  [generate] Certificate for " + courseName);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("certificateId", "CERT-674-001");
        result.getOutputData().put("format", "PDF");
        return result;
    }
}
