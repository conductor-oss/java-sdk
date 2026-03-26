package aivideogeneration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import aivideogeneration.workers.ScriptWorker;
import aivideogeneration.workers.StoryboardWorker;
import aivideogeneration.workers.GenerateWorker;
import aivideogeneration.workers.EditWorker;
import aivideogeneration.workers.RenderWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 807: AI Video Generation — Script, Storyboard, Generate, Edit, Render
 */
public class AiVideoGenerationExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 807: AI Video Generation — Script, Storyboard, Generate, Edit, Render ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("avg_script", "avg_storyboard", "avg_generate", "avg_edit", "avg_render"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ScriptWorker(), new StoryboardWorker(), new GenerateWorker(), new EditWorker(), new RenderWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("avg_video_generation", 1, Map.of("topic", "Introduction to quantum computing", "duration", 30, "style", "animated"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("videoId: %s%n", workflow.getOutput().get("videoId"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
