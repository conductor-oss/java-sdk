package medicalimaging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class StoreWorker implements Worker {

    @Override
    public String getTaskDefName() { return "img_store"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [store] Archiving study " + task.getInputData().get("studyId") + " to PACS");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("archived", true);
        output.put("pacsId", "PACS-STD-88201");
        output.put("archivedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
