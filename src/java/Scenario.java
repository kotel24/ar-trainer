record Scenario(String id, String title, String machine, String timeLimit, String passScore) {
    int timeLimitValue() {
        return Integer.parseInt(timeLimit);
    }

    int passScoreValue() {
        return Integer.parseInt(passScore);
    }
}
