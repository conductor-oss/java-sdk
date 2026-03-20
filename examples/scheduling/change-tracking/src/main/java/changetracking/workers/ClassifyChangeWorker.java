package changetracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class ClassifyChangeWorker implements Worker {
    @Override public String getTaskDefName() { return "chg_classify"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        int files = 0; try { files = Integer.parseInt(String.valueOf(task.getInputData().get("filesChanged"))); } catch (Exception ignored) {}
        int added = 0; try { added = Integer.parseInt(String.valueOf(task.getInputData().get("linesAdded"))); } catch (Exception ignored) {}
        int removed = 0; try { removed = Integer.parseInt(String.valueOf(task.getInputData().get("linesRemoved"))); } catch (Exception ignored) {}
        int lines = added + removed;
        String riskLevel = "low"; String changeType = "minor";
        if (files > 10 || lines > 500) { riskLevel = "high"; changeType = "major"; }
        else if (files > 5 || lines > 100) { riskLevel = "medium"; changeType = "moderate"; }
        System.out.println("  [classify] " + files + " files, " + lines + " lines -> " + changeType + " (" + riskLevel + ")");
        r.getOutputData().put("changeType", changeType);
        r.getOutputData().put("riskLevel", riskLevel);
        return r;
    }
}
