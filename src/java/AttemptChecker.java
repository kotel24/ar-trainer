import java.util.*;

final class AttemptChecker {
    private final List<CheckRule> rules = List.of(new DangerousDependencyRule(), new OrderRule());

    List<AttemptError> check(Attempt attempt, ScenarioGraph graph, List<AttemptAction> actions,
                             Map<String, ErrorKind> kinds) {
        Set<String> performed = new HashSet<>();
        Set<String> valid = new HashSet<>();
        List<AttemptError> errors = new ArrayList<>();
        for (AttemptAction action : actions) {
            String stepId = action.stepId();
            if (valid.contains(stepId)) {
                continue;
            }
            boolean missing = false;
            boolean dangerous = false;
            for (Dependency dependency : graph.requiredFor(stepId)) {
                missing |= !performed.contains(dependency.requiredId());
                dangerous |= dependency.dangerous() && !valid.contains(dependency.requiredId());
            }
            for (CheckRule rule : rules) {
                if (rule.matches(dangerous, missing, !performed.contains(stepId))) {
                    AttemptError error = rule.error(attempt, action);
                    errors.add(error);
                    if ("DANGER".equals(error.kindCode()) && kinds.get("DANGER").stopsAttempt()) {
                        return errors;
                    }
                    break;
                }
            }
            performed.add(stepId);
            if (!missing) {
                valid.add(stepId);
            }
        }
        for (Step step : graph.steps()) {
            if (!performed.contains(step.id())) {
                errors.add(new AttemptError(attempt.id(), step.id(), "SKIP"));
            }
        }
        return errors;
    }

    Evaluation evaluate(Attempt attempt, Scenario scenario, List<AttemptError> errors,
                        Map<String, ErrorKind> kinds) {
        int score = 100;
        boolean stopped = false;
        for (AttemptError error : errors) {
            ErrorKind kind = kinds.get(error.kindCode());
            score -= kind.penalty();
            stopped |= kind.stopsAttempt();
        }
        score = Math.max(score, 0);
        if (stopped) {
            return new Evaluation(score, "остановлена");
        }
        boolean passed = score >= scenario.passScore() && attempt.duration() <= scenario.timeLimit();
        return new Evaluation(score, passed ? "сдано" : "не сдано");
    }
}
