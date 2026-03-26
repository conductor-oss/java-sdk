package agentswarm.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decompose worker — takes a research topic and breaks it into 4 subtasks,
 * each assigned to a different swarm agent with a specific research area.
 */
public class DecomposeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "as_decompose";
    }

    @Override
    public TaskResult execute(Task task) {
        String researchTopic = (String) task.getInputData().get("researchTopic");
        if (researchTopic == null || researchTopic.isBlank()) {
            researchTopic = "unspecified topic";
        }

        System.out.println("  [as_decompose] Decomposing research topic: " + researchTopic);

        List<Map<String, Object>> subtasks = new ArrayList<>();

        Map<String, Object> subtask1 = new LinkedHashMap<>();
        subtask1.put("id", "subtask-1");
        subtask1.put("area", "Market Analysis");
        subtask1.put("instruction", "Analyze market trends, competitive landscape, and adoption rates for: " + researchTopic);
        subtasks.add(subtask1);

        Map<String, Object> subtask2 = new LinkedHashMap<>();
        subtask2.put("id", "subtask-2");
        subtask2.put("area", "Technical Landscape");
        subtask2.put("instruction", "Survey technical approaches, architectures, and key innovations for: " + researchTopic);
        subtasks.add(subtask2);

        Map<String, Object> subtask3 = new LinkedHashMap<>();
        subtask3.put("id", "subtask-3");
        subtask3.put("area", "Use Cases");
        subtask3.put("instruction", "Identify and evaluate real-world use cases, deployments, and case studies for: " + researchTopic);
        subtasks.add(subtask3);

        Map<String, Object> subtask4 = new LinkedHashMap<>();
        subtask4.put("id", "subtask-4");
        subtask4.put("area", "Future Trends");
        subtask4.put("instruction", "Forecast future developments, emerging trends, and potential disruptions for: " + researchTopic);
        subtasks.add(subtask4);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("subtasks", subtasks);
        result.getOutputData().put("subtaskCount", 4);
        return result;
    }
}
