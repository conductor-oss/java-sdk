package accountopening.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates and sends a welcome package to a new account holder.
 * Real logic: determines package contents based on account type.
 */
public class WelcomeWorker implements Worker {

    @Override
    public String getTaskDefName() { return "acc_welcome"; }

    @Override
    public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task);

        String applicantName = (String) task.getInputData().get("applicantName");
        if (applicantName == null || applicantName.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing required input: applicantName");
            return r;
        }

        String accountNumber = (String) task.getInputData().get("accountNumber");
        if (accountNumber == null || accountNumber.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing required input: accountNumber");
            return r;
        }

        String accountType = (String) task.getInputData().get("accountType");
        if (accountType == null || accountType.isBlank()) {
            r.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            r.setReasonForIncompletion("Missing required input: accountType");
            return r;
        }

        // Real welcome package contents based on account type
        List<String> includes = new ArrayList<>();
        includes.add("online_banking_enrollment");
        includes.add("mobile_app");

        switch (accountType) {
            case "checking" -> { includes.add("debit_card"); includes.add("checks"); }
            case "savings" -> includes.add("savings_goal_tools");
            case "money_market" -> { includes.add("debit_card"); includes.add("investment_overview"); }
            case "cd" -> includes.add("maturity_tracker");
        }

        // Generate welcome email content
        String emailSubject = "Welcome to Your New " + capitalize(accountType) + " Account!";
        String emailBody = "Dear " + applicantName + ",\n\n"
                + "Your new " + accountType + " account (" + accountNumber + ") is now active.\n"
                + "Your welcome package includes: " + String.join(", ", includes) + ".\n\n"
                + "Thank you for banking with us!";

        System.out.println("  [welcome] Welcome package for " + applicantName
                + " - account " + accountNumber + " (" + includes.size() + " items)");

        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("welcomeSent", true);
        r.getOutputData().put("includes", includes);
        r.getOutputData().put("emailSubject", emailSubject);
        r.getOutputData().put("emailBody", emailBody);
        r.getOutputData().put("sentAt", Instant.now().toString());
        return r;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).replace("_", " ");
    }
}
