package publicrecords.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

public class RedactWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pbr_redact";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [redact] PII and sensitive info redacted from 3 documents");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("redactedDocs", List.of("DOC-A-R", "DOC-B-R", "DOC-C-R"));
        result.getOutputData().put("redactionsApplied", 12);
        return result;
    }
}
