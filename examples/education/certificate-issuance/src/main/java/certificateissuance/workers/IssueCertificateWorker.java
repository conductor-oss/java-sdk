package certificateissuance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class IssueCertificateWorker implements Worker {
    @Override public String getTaskDefName() { return "cer_issue"; }

    @Override
    public TaskResult execute(Task task) {
        String certId = (String) task.getInputData().get("certificateId");
        String studentId = (String) task.getInputData().get("studentId");
        System.out.println("  [issue] " + certId + " issued to " + studentId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("issued", true);
        result.getOutputData().put("deliveryMethod", "email");
        return result;
    }
}
