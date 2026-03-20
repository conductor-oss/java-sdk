package tutoringmatch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MatchTutorWorker implements Worker {
    @Override public String getTaskDefName() { return "tut_match_tutor"; }
    @Override public TaskResult execute(Task task) {
        String tutorName = "Alex Rodriguez";
        System.out.println("  [match] Best match for " + task.getInputData().get("subject") + ": " + tutorName + " (rating: 4.8)");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("tutorId", "TUT-42");
        result.getOutputData().put("tutorName", tutorName);
        result.getOutputData().put("rating", 4.8);
        return result;
    }
}
