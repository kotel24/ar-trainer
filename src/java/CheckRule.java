abstract class CheckRule {
    private final String code;

    CheckRule(String code) {
        this.code = code;
    }

    abstract boolean matches(boolean dangerous, boolean missing, boolean firstExecution);

    AttemptError error(Attempt attempt, AttemptAction action) {
        return new AttemptError(attempt.id(), action.stepId(), code);
    }
}

final class DangerousDependencyRule extends CheckRule {
    DangerousDependencyRule() {
        super("DANGER");
    }

    @Override
    boolean matches(boolean dangerous, boolean missing, boolean firstExecution) {
        return dangerous;
    }
}

final class OrderRule extends CheckRule {
    OrderRule() {
        super("ORDER");
    }

    @Override
    boolean matches(boolean dangerous, boolean missing, boolean firstExecution) {
        return missing && firstExecution;
    }
}
