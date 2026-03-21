package usersurvey.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.UUID;

public class CreateSurveyWorker implements Worker {
    @Override public String getTaskDefName() { return "usv_create"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        String surveyId = "SRV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String title = (String) task.getInputData().get("title");
        List<String> questions = (List<String>) task.getInputData().get("questions");
        System.out.println("  [create] Survey \"" + title + "\" created -> " + surveyId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("surveyId", surveyId);
        r.getOutputData().put("questionCount", questions != null ? questions.size() : 0);
        return r;
    }
}
