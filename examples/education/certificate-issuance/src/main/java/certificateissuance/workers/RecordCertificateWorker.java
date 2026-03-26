package certificateissuance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RecordCertificateWorker implements Worker {
    @Override public String getTaskDefName() { return "cer_record"; }

    @Override
    public TaskResult execute(Task task) {
        String certId = (String) task.getInputData().get("certificateId");
        System.out.println("  [record] " + certId + " recorded in transcript");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recorded", true);
        result.getOutputData().put("blockchain", true);
        return result;
    }
}
