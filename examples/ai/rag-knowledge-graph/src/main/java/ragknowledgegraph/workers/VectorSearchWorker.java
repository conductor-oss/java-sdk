package ragknowledgegraph.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that performs vector similarity search using the question and
 * entity hints. Returns 4 documents with id, text, and score fields.
 * In production this would query a vector database like Pinecone or Qdrant.
 */
public class VectorSearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "kg_vector_search";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        System.out.println("  [vector_search] Searching for: " + question);

        List<Map<String, Object>> documents = List.of(
                Map.of("id", "doc-1",
                        "text", "Netflix created Conductor in 2016 to orchestrate microservices workflows at scale.",
                        "score", 0.94),
                Map.of("id", "doc-2",
                        "text", "Conductor provides a JSON-based DSL for defining complex workflows with conditional branching and parallel execution.",
                        "score", 0.91),
                Map.of("id", "doc-3",
                        "text", "Workflow orchestration engines like Conductor decouple task execution from workflow logic, improving reliability.",
                        "score", 0.87),
                Map.of("id", "doc-4",
                        "text", "Conductor was open-sourced by Netflix and later adopted by Orkes for enterprise cloud offerings.",
                        "score", 0.85)
        );

        System.out.println("  [vector_search] Found " + documents.size() + " documents");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documents", documents);
        return result;
    }
}
