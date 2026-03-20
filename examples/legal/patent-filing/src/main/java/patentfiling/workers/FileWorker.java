package patentfiling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class FileWorker implements Worker {
    @Override public String getTaskDefName() { return "ptf_file"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [file] Filing patent application for draft " + task.getInputData().get("draftId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("applicationNumber", "US-2024-" + (int)(Math.random() * 900000 + 100000));
        result.getOutputData().put("filingDate", java.time.LocalDate.now().toString());
        return result;
    }
}
