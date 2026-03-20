package ragaccesscontrol.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Worker that retrieves documents filtered by the user's allowed collections.
 * Contains a corpus of 5 documents across different collections and returns
 * only those the user is permitted to access.
 */
public class FilteredRetrieveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ac_filtered_retrieve";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        List<String> allowedCollections = (List<String>) task.getInputData().get("allowedCollections");
        String clearanceLevel = (String) task.getInputData().get("clearanceLevel");

        List<Map<String, Object>> allDocuments = List.of(
                Map.of("id", "doc1", "collection", "public-docs",
                        "content", "Company overview and mission statement.",
                        "classification", "public"),
                Map.of("id", "doc2", "collection", "engineering-wiki",
                        "content", "Engineering team guidelines: code review process, deployment procedures, and on-call rotations.",
                        "classification", "internal"),
                Map.of("id", "doc3", "collection", "hr-policies",
                        "content", "Employee benefits include health insurance. Contact HR for SSN updates. Average salary range: $95,000-$145,000.",
                        "classification", "confidential"),
                Map.of("id", "doc4", "collection", "finance-reports",
                        "content", "Q3 revenue: $12.5M. Executive compensation: CEO salary $450,000. SSN on file: 123-45-6789.",
                        "classification", "top-secret"),
                Map.of("id", "doc5", "collection", "executive-memos",
                        "content", "Board meeting notes: upcoming acquisition target valued at $50M.",
                        "classification", "top-secret")
        );

        int totalRetrieved = allDocuments.size();
        List<Map<String, Object>> filteredDocuments = new ArrayList<>();

        for (Map<String, Object> doc : allDocuments) {
            if (allowedCollections.contains(doc.get("collection"))) {
                filteredDocuments.add(doc);
            }
        }

        int afterFilter = filteredDocuments.size();

        System.out.println("  [retrieve] Question: '" + question + "' — retrieved " + totalRetrieved
                + " docs, " + afterFilter + " after access filter");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documents", filteredDocuments);
        result.getOutputData().put("totalRetrieved", totalRetrieved);
        result.getOutputData().put("afterFilter", afterFilter);
        return result;
    }
}
