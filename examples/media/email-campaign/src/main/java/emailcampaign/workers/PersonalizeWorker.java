package emailcampaign.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class PersonalizeWorker implements Worker {
    @Override public String getTaskDefName() { return "eml_personalize"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [personalize] Processing " + task.getInputData().getOrDefault("personalizedCount", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("personalizedCount", task.getInputData().get("recipientCount"));
        r.getOutputData().put("variantsCreated", 3);
        r.getOutputData().put("mergeFieldsUsed", List.of("firstName"));
        return r;
    }
}
