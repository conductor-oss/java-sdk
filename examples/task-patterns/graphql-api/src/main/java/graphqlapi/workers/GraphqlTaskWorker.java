package graphqlapi.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * GraphQL task worker — performs a deployment operation.
 * Takes a project name and environment, returns a deployment result.
 *
 * Input:  { project, env }
 * Output: { project, result: "{project}-{env}-deployed" }
 */
public class GraphqlTaskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gql_task";
    }

    @Override
    public TaskResult execute(Task task) {
        String project = (String) task.getInputData().get("project");
        String env = (String) task.getInputData().get("env");

        if (project == null || project.isBlank()) {
            project = "default-project";
        }
        if (env == null || env.isBlank()) {
            env = "dev";
        }

        String deployResult = project + "-" + env + "-deployed";

        System.out.println("  [gql_task] Deploying project=" + project
                + " env=" + env + " => " + deployResult);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("project", project);
        result.getOutputData().put("result", deployResult);
        return result;
    }
}
