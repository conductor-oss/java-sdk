package pdfprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw text into sections based on chapter headings.
 * Input: rawText, pageCount
 * Output: sections (list of maps with title/content), sectionCount
 */
public class ParseSectionsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pd_parse_sections";
    }

    @Override
    public TaskResult execute(Task task) {
        String raw = (String) task.getInputData().get("rawText");
        if (raw == null) {
            raw = "";
        }

        Pattern sectionRegex = Pattern.compile("Chapter \\d+: ([^\\n]+)\\n([\\s\\S]*?)(?=Chapter \\d+:|$)");
        Matcher matcher = sectionRegex.matcher(raw);

        List<Map<String, String>> sections = new ArrayList<>();
        while (matcher.find()) {
            Map<String, String> section = new LinkedHashMap<>();
            section.put("title", matcher.group(1).trim());
            section.put("content", matcher.group(2).trim());
            sections.add(section);
        }

        System.out.println("  [parse] Parsed " + sections.size() + " sections: "
                + sections.stream().map(s -> s.get("title")).reduce((a, b) -> a + ", " + b).orElse(""));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sections", sections);
        result.getOutputData().put("sectionCount", sections.size());
        return result;
    }
}
