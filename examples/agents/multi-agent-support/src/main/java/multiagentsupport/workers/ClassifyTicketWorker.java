package multiagentsupport.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Classifies a support ticket by keywords in subject/description.
 * Categories: bug, feature, general.
 */
public class ClassifyTicketWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cs_classify_ticket";
    }

    @Override
    public TaskResult execute(Task task) {
        String subject = (String) task.getInputData().get("subject");
        String description = (String) task.getInputData().get("description");

        if (subject == null) subject = "";
        if (description == null) description = "";

        System.out.println("  [cs_classify_ticket] Classifying ticket: " + subject);

        String combined = (subject + " " + description).toLowerCase();

        String category;
        String severity;
        List<String> keywords = new ArrayList<>();

        if (combined.contains("error") || combined.contains("crash") || combined.contains("bug")
                || combined.contains("broken") || combined.contains("fail")) {
            category = "bug";
            severity = "high";
            if (combined.contains("error")) keywords.add("error");
            if (combined.contains("crash")) keywords.add("crash");
            if (combined.contains("bug")) keywords.add("bug");
            if (combined.contains("broken")) keywords.add("broken");
            if (combined.contains("fail")) keywords.add("fail");
        } else if (combined.contains("feature") || combined.contains("request")
                || combined.contains("enhance") || combined.contains("add")) {
            category = "feature";
            severity = "medium";
            if (combined.contains("feature")) keywords.add("feature");
            if (combined.contains("request")) keywords.add("request");
            if (combined.contains("enhance")) keywords.add("enhance");
            if (combined.contains("add")) keywords.add("add");
        } else {
            category = "general";
            severity = "low";
            keywords.add("inquiry");
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("category", category);
        output.put("severity", severity);
        output.put("keywords", keywords);
        output.put("confidence", 0.94);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().putAll(output);
        return result;
    }
}
