final class DangerousDependencyRule extends CheckRule {
    DangerousDependencyRule() {
        super("DANGER");
    }

    @Override
    boolean matches(boolean hasUnmetCriticalPrerequisite, boolean hasUnperformedPrerequisite, boolean firstOccurrenceOfStep) {
        return hasUnmetCriticalPrerequisite;
    }
}
