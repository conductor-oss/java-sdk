package aipromptengineering;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import aipromptengineering.workers.DefineTaskWorker;
import aipromptengineering.workers.GeneratePromptsWorker;
import aipromptengineering.workers.TestVariantsWorker;
import aipromptengineering.workers.EvaluateWorker;
import aipromptengineering.workers.SelectBestWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 806: AI Prompt Engineering — Define Task, Generate Prompts, Test Variants, Evaluate, Select Best
 */
public class AiPromptEngineeringExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 806: AI Prompt Engineering — Define Task, Generate Prompts, Test Variants, Evaluate, Select Best ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("ape_define_task", "ape_generate_prompts", "ape_test_variants", "ape_evaluate", "ape_select_best"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new DefineTaskWorker(), new GeneratePromptsWorker(), new TestVariantsWorker(), new EvaluateWorker(), new SelectBestWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("ape_prompt_engineering", 1, Map.of("taskDescription", "Summarize technical articles", "modelId", "gpt-4", "evaluationCriteria", "ROUGE-L"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("bestPromptId: %s%n", workflow.getOutput().get("bestPromptId"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
