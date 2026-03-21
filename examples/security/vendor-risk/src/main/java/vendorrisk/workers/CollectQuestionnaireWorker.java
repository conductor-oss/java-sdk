package vendorrisk.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectQuestionnaireWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vr_collect_questionnaire";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [questionnaire] CloudAnalytics Inc: security questionnaire completed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("collect_questionnaireId", "COLLECT_QUESTIONNAIRE-1362");
        result.addOutputData("success", true);
        return result;
    }
}
