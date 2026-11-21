final class OrderRule extends CheckRule {
    OrderRule() {
        super("ORDER");
    }

    @Override
    boolean matches(boolean hasUnmetCriticalPrerequisite, boolean hasUnperformedPrerequisite, boolean firstOccurrenceOfStep) {
        return hasUnperformedPrerequisite && firstOccurrenceOfStep;
    }
}
