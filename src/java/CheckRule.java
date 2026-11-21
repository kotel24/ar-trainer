abstract class CheckRule {
    private final String code;

    CheckRule(String code) {
        this.code = code;
    }

    abstract boolean matches(boolean hasUnmetCriticalPrerequisite, boolean hasUnperformedPrerequisite, boolean firstOccurrenceOfStep);

    AttemptError error(Attempt attempt, AttemptAction action) {
        return new AttemptError(attempt.id(), action.stepId(), code);
    }
}

final class DangerousDependencyRule extends CheckRule {
    DangerousDependencyRule() {
        super("DANGER");
    }

    @Override
    boolean matches(boolean hasUnmetCriticalPrerequisite, boolean hasUnperformedPrerequisite, boolean firstOccurrenceOfStep) {
        return hasUnmetCriticalPrerequisite;
    }
}

final class OrderRule extends CheckRule {
    OrderRule() {
        super("ORDER");
    }

    @Override
    boolean matches(boolean hasUnmetCriticalPrerequisite, boolean hasUnperformedPrerequisite, boolean firstOccurrenceOfStep) {
        return hasUnperformedPrerequisite && firstOccurrenceOfStep;
    }
}
