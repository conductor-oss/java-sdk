package gdprconsent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.Map;

public class RecordConsentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gdc_record_consent";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        Object consents = task.getInputData().get("consents");
        System.out.println("  [record] Consent recorded: " + consents);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recorded", true);
        result.getOutputData().put("consentRecord", Map.of(
                "userId", userId,
                "consents", consents != null ? consents : Map.of(),
                "timestamp", Instant.now().toString(),
                "version", "2.1"
        ));
        return result;
    }
}
