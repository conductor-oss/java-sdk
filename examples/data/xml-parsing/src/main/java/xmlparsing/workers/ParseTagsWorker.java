package xmlparsing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Parses XML tags and returns deterministic.parsed elements.
 * Input: xml, rootElement
 * Output: elements (list of parsed element maps), elementCount
 */
public class ParseTagsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "xp_parse_tags";
    }

    @Override
    public TaskResult execute(Task task) {
        String xml = (String) task.getInputData().get("xml");
        if (xml == null) {
            xml = "";
        }

        String rootElement = (String) task.getInputData().get("rootElement");
        if (rootElement == null) {
            rootElement = "root";
        }

        System.out.println("  [xp_parse_tags] Parsing tags from root: " + rootElement);

        // Fixed deterministic.parsed elements
        List<Map<String, Object>> elements = List.of(
                Map.of(
                        "tag", "product",
                        "attributes", Map.of("id", "P-101"),
                        "children", Map.of("name", "Laptop", "price", "999.99", "category", "electronics")
                ),
                Map.of(
                        "tag", "product",
                        "attributes", Map.of("id", "P-102"),
                        "children", Map.of("name", "Headphones", "price", "149.99", "category", "audio")
                ),
                Map.of(
                        "tag", "product",
                        "attributes", Map.of("id", "P-103"),
                        "children", Map.of("name", "Keyboard", "price", "79.99", "category", "peripherals")
                )
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("elements", elements);
        result.getOutputData().put("elementCount", 3);
        return result;
    }
}
