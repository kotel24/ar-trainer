record ErrorKind(String code, String title, String penalty, boolean stopsAttempt) {
    int penaltyValue() {
        return Integer.parseInt(penalty);
    }
}
