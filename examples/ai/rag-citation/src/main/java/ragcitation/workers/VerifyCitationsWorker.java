package ragcitation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Worker that verifies citation references against the retrieved document set.
 * Checks that each citation's docId exists in the documents, and reports
 * how many citations were verified and whether all passed verification.
 */
public class VerifyCitationsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cr_verify_citations";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> citations = (List<Map<String, Object>>) task.getInputData().get("citations");
        List<Map<String, Object>> documents = (List<Map<String, Object>>) task.getInputData().get("documents");

        if (citations == null) {
            citations = List.of();
        }
        if (documents == null) {
            documents = List.of();
        }

        System.out.println("  [verify_citations] Verifying " + citations.size() + " citations against " + documents.size() + " documents");

        Set<String> docIds = new HashSet<>();
        for (Map<String, Object> doc : documents) {
            docIds.add((String) doc.get("id"));
        }

        List<Map<String, Object>> results = new ArrayList<>();
        int verifiedCount = 0;

        for (Map<String, Object> citation : citations) {
            String docId = (String) citation.get("docId");
            boolean verified = docIds.contains(docId);
            if (verified) {
                verifiedCount++;
            }

            Map<String, Object> verificationResult = new HashMap<>();
            verificationResult.put("marker", citation.get("marker"));
            verificationResult.put("docId", docId);
            verificationResult.put("verified", verified);
            results.add(verificationResult);
        }

        boolean allVerified = verifiedCount == citations.size();

        System.out.println("  [verify_citations] Verified " + verifiedCount + "/" + citations.size() + " citations");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("results", results);
        result.getOutputData().put("verifiedCount", verifiedCount);
        result.getOutputData().put("allVerified", allVerified);
        return result;
    }
}
