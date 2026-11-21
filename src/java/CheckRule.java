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
