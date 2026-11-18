record AttemptAction(String attemptId, String sequence, String stepId, int time) {
    int sequenceValue() {
        return Integer.parseInt(sequence);
    }
}
