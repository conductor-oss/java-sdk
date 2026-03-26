package certificateissuance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SignCertificateWorker implements Worker {
    @Override public String getTaskDefName() { return "cer_sign"; }

    @Override
    public TaskResult execute(Task task) {
        String certId = (String) task.getInputData().get("certificateId");
        System.out.println("  [sign] " + certId + " digitally signed by registrar");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("signed", true);
        result.getOutputData().put("signedBy", "Dr. Academic Dean");
        return result;
    }
}
