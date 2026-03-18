package agentcollaboration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executor agent — translates strategy and priorities into concrete action items
 * with owners, deadlines, and an 8-week phased timeline.
 */
public class ExecutorWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ac_executor";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [ac_executor] Building execution plan...");

        List<Map<String, Object>> actionItems = new ArrayList<>();

        Map<String, Object> a1 = new LinkedHashMap<>();
        a1.put("id", "ACT-001");
        a1.put("task", "Redesign onboarding email sequence with personalized milestones");
        a1.put("owner", "Marketing");
        a1.put("deadline", "Week 2");
        a1.put("priority", "critical");
        actionItems.add(a1);

        Map<String, Object> a2 = new LinkedHashMap<>();
        a2.put("id", "ACT-002");
        a2.put("task", "Implement 4-hour support SLA with escalation triggers");
        a2.put("owner", "Customer Support");
        a2.put("deadline", "Week 3");
        a2.put("priority", "high");
        actionItems.add(a2);

        Map<String, Object> a3 = new LinkedHashMap<>();
        a3.put("id", "ACT-003");
        a3.put("task", "Launch tiered loyalty rewards with early-access perks");
        a3.put("owner", "Product");
        a3.put("deadline", "Week 4");
        a3.put("priority", "high");
        actionItems.add(a3);

        Map<String, Object> a4 = new LinkedHashMap<>();
        a4.put("id", "ACT-004");
        a4.put("task", "Deploy churn-prediction model for proactive outreach");
        a4.put("owner", "Data Science");
        a4.put("deadline", "Week 5");
        a4.put("priority", "medium");
        actionItems.add(a4);

        Map<String, Object> a5 = new LinkedHashMap<>();
        a5.put("id", "ACT-005");
        a5.put("task", "Create win-back campaign for recently churned customers");
        a5.put("owner", "Marketing");
        a5.put("deadline", "Week 6");
        a5.put("priority", "medium");
        actionItems.add(a5);

        Map<String, Object> a6 = new LinkedHashMap<>();
        a6.put("id", "ACT-006");
        a6.put("task", "Establish monthly retention review cadence with exec stakeholders");
        a6.put("owner", "Operations");
        a6.put("deadline", "Week 8");
        a6.put("priority", "low");
        actionItems.add(a6);

        Map<String, Object> timeline = new LinkedHashMap<>();
        timeline.put("totalWeeks", 8);

        List<Map<String, Object>> phases = new ArrayList<>();

        Map<String, Object> phase1 = new LinkedHashMap<>();
        phase1.put("name", "Quick Wins");
        phase1.put("weeks", "1-3");
        phase1.put("focus", "Onboarding redesign and support SLA");
        phases.add(phase1);

        Map<String, Object> phase2 = new LinkedHashMap<>();
        phase2.put("name", "Foundation");
        phase2.put("weeks", "4-5");
        phase2.put("focus", "Loyalty launch and churn prediction");
        phases.add(phase2);

        Map<String, Object> phase3 = new LinkedHashMap<>();
        phase3.put("name", "Scale & Sustain");
        phase3.put("weeks", "6-8");
        phase3.put("focus", "Win-back campaigns and governance");
        phases.add(phase3);

        timeline.put("phases", phases);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("actionItems", actionItems);
        result.getOutputData().put("timeline", timeline);
        result.getOutputData().put("actionItemCount", 6);
        return result;
    }
}
