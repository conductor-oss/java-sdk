package leadnurturing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class PersonalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nur_personalize";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String segment = (String) task.getInputData().get("segment");
        List<String> interests = (List<String>) task.getInputData().get("interests");
        if (interests == null) interests = List.of();

        String topic = interests.isEmpty() ? "our solutions" : interests.get(0);
        System.out.println("  [personalize] Content personalized for " + segment + " with interests: " + String.join(", ", interests));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("content", Map.of(
                "subject", "Explore " + topic,
                "template", "nurture-" + segment,
                "cta", "Learn More"
        ));
        return result;
    }
}
