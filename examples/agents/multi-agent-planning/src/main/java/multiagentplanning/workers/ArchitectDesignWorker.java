package multiagentplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Architect agent — takes projectName and requirements, produces a system
 * architecture with frontend components, backend components, infrastructure
 * items, complexity levels, and an architecture summary.
 */
public class ArchitectDesignWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pp_architect_design";
    }

    @Override
    public TaskResult execute(Task task) {
        String projectName = (String) task.getInputData().get("projectName");
        if (projectName == null || projectName.isBlank()) {
            projectName = "Unnamed Project";
        }
        String requirements = (String) task.getInputData().get("requirements");
        if (requirements == null || requirements.isBlank()) {
            requirements = "general requirements";
        }

        System.out.println("  [pp_architect_design] Designing architecture for: " + projectName);

        List<String> frontendComponents = List.of(
                "User Dashboard",
                "Authentication UI",
                "Interactive Data Visualization"
        );

        List<String> backendComponents = List.of(
                "REST API Gateway",
                "Core Business Logic Service",
                "Data Processing Pipeline",
                "Notification Service"
        );

        List<String> infrastructure = List.of(
                "Kubernetes Cluster",
                "PostgreSQL Database",
                "Redis Cache",
                "CI/CD Pipeline"
        );

        String architectureSummary = "Microservices architecture for " + projectName
                + " with React frontend, Spring Boot backend services, "
                + "and cloud-native infrastructure. Requirements: " + requirements;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("frontendComponents", frontendComponents);
        result.getOutputData().put("backendComponents", backendComponents);
        result.getOutputData().put("infrastructure", infrastructure);
        result.getOutputData().put("architectureSummary", architectureSummary);
        result.getOutputData().put("frontendComplexity", "medium");
        result.getOutputData().put("backendComplexity", "high");
        result.getOutputData().put("infraComplexity", "medium");
        return result;
    }
}
