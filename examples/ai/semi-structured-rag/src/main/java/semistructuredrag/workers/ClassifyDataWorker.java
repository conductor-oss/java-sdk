package semistructuredrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that classifies input data into structured fields and unstructured
 * text chunks. Returns structuredFields (field/type/source), textChunks
 * (id/type/content), and a dataTypes summary list.
 */
public class ClassifyDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ss_classify_data";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }
        String dataContext = (String) task.getInputData().get("dataContext");
        if (dataContext == null) {
            dataContext = "";
        }

        System.out.println("  [classify] Classifying data for question: " + question);

        List<Map<String, String>> structuredFields = List.of(
                Map.of("field", "revenue", "type", "numeric", "source", "financials_db"),
                Map.of("field", "employee_count", "type", "numeric", "source", "hr_db"),
                Map.of("field", "department", "type", "categorical", "source", "org_db"),
                Map.of("field", "quarter", "type", "temporal", "source", "financials_db")
        );

        List<Map<String, String>> textChunks = List.of(
                Map.of("id", "chunk-001", "type", "report", "content", "Q3 earnings exceeded expectations with 15% growth."),
                Map.of("id", "chunk-002", "type", "memo", "content", "Engineering team expanded to support new product line."),
                Map.of("id", "chunk-003", "type", "analysis", "content", "Market trends indicate strong demand in cloud services.")
        );

        List<String> dataTypes = List.of("numeric", "categorical", "temporal", "report", "memo", "analysis");

        System.out.println("  [classify] Found " + structuredFields.size() + " structured fields, "
                + textChunks.size() + " text chunks");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("structuredFields", structuredFields);
        result.getOutputData().put("textChunks", textChunks);
        result.getOutputData().put("dataTypes", dataTypes);
        return result;
    }
}
