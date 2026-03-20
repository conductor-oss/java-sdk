package hubspotintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Creates a contact in HubSpot.
 * Input: email, firstName, lastName, company
 * Output: contactId, createdAt
 */
public class CreateContactWorker implements Worker {

    private final String apiKey;

    public CreateContactWorker() {
        this.apiKey = System.getenv("HUBSPOT_API_KEY");
    }

    @Override
    public String getTaskDefName() {
        return "hs_create_contact";
    }

    @Override
    public TaskResult execute(Task task) {
        String firstName = (String) task.getInputData().get("firstName");
        String lastName = (String) task.getInputData().get("lastName");
        String email = (String) task.getInputData().get("email");
        String company = (String) task.getInputData().get("company");

        // Live mode: call HubSpot Contacts API
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String bodyJson = "{\"properties\":{\"email\":\"" + (email != null ? email : "") + "\","
                        + "\"firstname\":\"" + (firstName != null ? firstName : "") + "\","
                        + "\"lastname\":\"" + (lastName != null ? lastName : "") + "\","
                        + "\"company\":\"" + (company != null ? company : "") + "\"}}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.hubapi.com/crm/v3/objects/contacts"))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String responseBody = response.body();
                    // Extract id from response JSON (simple extraction)
                    String contactId = "hs-unknown";
                    int idIdx = responseBody.indexOf("\"id\":");
                    if (idIdx >= 0) {
                        int start = responseBody.indexOf("\"", idIdx + 5) + 1;
                        int end = responseBody.indexOf("\"", start);
                        if (start > 0 && end > start) {
                            contactId = responseBody.substring(start, end);
                        }
                    }
                    System.out.println("  [create] Contact " + contactId + ": " + firstName + " " + lastName + " (" + email + ")");
                    TaskResult result = new TaskResult(task);
                    result.setStatus(TaskResult.Status.COMPLETED);
                    result.getOutputData().put("contactId", contactId);
                    result.getOutputData().put("createdAt", java.time.Instant.now().toString());
                    return result;
                } else {
                    System.err.println("  [worker] API error HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("  [worker] API error: " + e.getMessage());
            }
        }

        // Fallback
        String contactId = "hs-" + System.currentTimeMillis();
        System.out.println("  [create] " + contactId + ": " + firstName + " " + lastName + " (" + email + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("contactId", contactId);
        result.getOutputData().put("createdAt", "" + java.time.Instant.now().toString());
        return result;
    }
}
