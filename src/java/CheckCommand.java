import java.io.IOException;
import java.util.List;
import java.util.Map;

final class CheckCommand implements TrainerCommand {
    private final AttemptChecker checker = new AttemptChecker();

    @Override
    public int run(CsvStore store, String id) throws IOException {
        Attempt attempt = store.attempt(id);
        if (attempt == null) {
            System.out.println("Попытка " + id + " не найдена");
            return 1;
        }
        Internship internship = store.internship(attempt.orderNo());
        Worker trainee = store.worker(internship.traineeId());
        Scenario scenario = store.scenario(internship.scenarioId());
        ScenarioGraph graph = store.graph(scenario);
        List<AttemptAction> actions = store.actions(id);
        Map<String, ErrorKind> kinds = store.errorKinds();
        ErrorKind danger = kinds.get("DANGER");
        if (danger == null || !danger.stopsAttempt()) {
            System.err.println("Ошибка в error_kinds.csv: для вида DANGER требуется stops_attempt=1.");
            return 1;
        }
        List<AttemptError> errors = checker.check(attempt, graph, actions, kinds);
        Evaluation evaluation = checker.evaluate(attempt, scenario, errors, kinds);
        store.save(attempt, evaluation, errors);

        System.out.println("Попытка " + id + " от " + attempt.startedAt() + ", стажировка № "
                + internship.orderNo() + ", стажёр " + trainee.fullName());
        System.out.println("Сценарий: " + scenario.title());
        System.out.println("Действия стажёра:");
        for (AttemptAction action : actions) {
            System.out.println("  " + action.sequence() + ". " + graph.step(action.stepId()).action());
        }
        if (errors.isEmpty()) {
            System.out.println("Ошибок нет");
        } else {
            System.out.println("Ошибки:");
            for (AttemptError error : errors) {
                Step step = graph.step(error.stepId());
                ErrorKind kind = kinds.get(error.kindCode());
                System.out.println("  шаг " + step.number() + " «" + step.action() + "» – "
                        + kind.title() + " (−" + kind.penalty() + ")");
            }
        }
        System.out.println("Время: " + attempt.duration() + " с при норме " + scenario.timeLimit() + " с");
        System.out.println("Балл: " + evaluation.score() + " из 100, проходной " + scenario.passScore());
        System.out.println("Результат: " + evaluation.result());
        System.out.println("Протокол записан в attempts.csv и errors.csv");
        return 0;
    }
}
