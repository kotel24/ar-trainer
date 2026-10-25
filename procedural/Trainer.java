import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class Trainer {

    static final String SEPARATOR = ";";

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            printUsage();
            return;
        }
        Path dataDir = Path.of(args[1]);
        switch (args[0]) {
            case "demo" -> runDemo(dataDir, args[2]);
            case "check" -> runCheck(dataDir, args[2]);
            default -> printUsage();
        }
    }

    static void printUsage() {
        System.out.println("Использование:");
        System.out.println("  java -cp out Trainer demo <каталог данных> <номер сценария>");
        System.out.println("  java -cp out Trainer check <каталог данных> <номер попытки>");
    }

    static List<String> readColumns(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        return List.of(lines.get(0).split(SEPARATOR, -1));
    }

    static List<Map<String, String>> readTable(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String[] columns = lines.get(0).split(SEPARATOR, -1);
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            String[] values = lines.get(i).split(SEPARATOR, -1);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < columns.length; j++) {
                row.put(columns[j], j < values.length ? values[j] : "");
            }
            rows.add(row);
        }
        return rows;
    }

    static void writeTable(Path file, List<String> columns, List<Map<String, String>> rows) throws IOException {
        StringBuilder text = new StringBuilder(String.join(SEPARATOR, columns)).append('\n');
        for (Map<String, String> row : rows) {
            List<String> values = new ArrayList<>();
            for (String column : columns) {
                values.add(row.getOrDefault(column, ""));
            }
            text.append(String.join(SEPARATOR, values)).append('\n');
        }
        Files.writeString(file, text.toString(), StandardCharsets.UTF_8);
    }

    static Map<String, Map<String, String>> indexBy(List<Map<String, String>> rows, String column) {
        Map<String, Map<String, String>> index = new HashMap<>();
        for (Map<String, String> row : rows) {
            index.put(row.get(column), row);
        }
        return index;
    }

    static List<Map<String, String>> filterBy(List<Map<String, String>> rows, String column, String value) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> row : rows) {
            if (row.get(column).equals(value)) {
                result.add(row);
            }
        }
        return result;
    }

    static List<Map<String, String>> dependenciesOf(List<Map<String, String>> steps, List<Map<String, String>> dependencies) {
        Set<String> stepIds = new HashSet<>();
        for (Map<String, String> step : steps) {
            stepIds.add(step.get("step_id"));
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> dependency : dependencies) {
            if (stepIds.contains(dependency.get("step_id")) && stepIds.contains(dependency.get("required_step_id"))) {
                result.add(dependency);
            }
        }
        return result;
    }

    static Map<String, List<String>> requiredSteps(List<Map<String, String>> dependencies) {
        Map<String, List<String>> required = new HashMap<>();
        for (Map<String, String> dependency : dependencies) {
            required.computeIfAbsent(dependency.get("step_id"), key -> new ArrayList<>())
                    .add(dependency.get("required_step_id"));
        }
        return required;
    }

    static List<Map<String, String>> orderBySeq(List<Map<String, String>> actions) {
        List<Map<String, String>> ordered = new ArrayList<>(Collections.nCopies(actions.size(), null));
        for (Map<String, String> action : actions) {
            ordered.set(Integer.parseInt(action.get("seq_no")) - 1, action);
        }
        return ordered;
    }

    static List<String> buildDemoOrder(List<Map<String, String>> steps, List<Map<String, String>> dependencies) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        Map<String, Integer> numberById = new HashMap<>();
        Map<Integer, String> idByNumber = new HashMap<>();
        for (Map<String, String> step : steps) {
            String id = step.get("step_id");
            int number = Integer.parseInt(step.get("step_no"));
            inDegree.put(id, 0);
            dependents.put(id, new ArrayList<>());
            numberById.put(id, number);
            idByNumber.put(number, id);
        }
        for (Map<String, String> dependency : dependencies) {
            String id = dependency.get("step_id");
            inDegree.put(id, inDegree.get(id) + 1);
            dependents.get(dependency.get("required_step_id")).add(id);
        }
        PriorityQueue<Integer> ready = new PriorityQueue<>();
        for (Map<String, String> step : steps) {
            if (inDegree.get(step.get("step_id")) == 0) {
                ready.add(numberById.get(step.get("step_id")));
            }
        }
        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String id = idByNumber.get(ready.poll());
            order.add(id);
            for (String next : dependents.get(id)) {
                int left = inDegree.get(next) - 1;
                inDegree.put(next, left);
                if (left == 0) {
                    ready.add(numberById.get(next));
                }
            }
        }
        if (order.size() < steps.size()) {
            return null;
        }
        return order;
    }

    static Map<String, String> newError(String stepId, String kindCode) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("step_id", stepId);
        error.put("kind_code", kindCode);
        return error;
    }

    static List<Map<String, String>> checkActions(List<Map<String, String>> steps, Map<String, List<String>> required,
                                                  List<Map<String, String>> actions,
                                                  Map<String, Map<String, String>> errorKinds) {
        Set<String> critical = new HashSet<>();
        for (Map<String, String> step : steps) {
            if (step.get("critical").equals("1")) {
                critical.add(step.get("step_id"));
            }
        }
        Set<String> done = new HashSet<>();
        List<Map<String, String>> errors = new ArrayList<>();
        for (Map<String, String> action : actions) {
            String stepId = action.get("step_id");
            if (done.contains(stepId)) {
                continue;
            }
            boolean missing = false;
            boolean dangerous = false;
            for (String requiredId : required.getOrDefault(stepId, List.of())) {
                if (!done.contains(requiredId)) {
                    missing = true;
                    if (critical.contains(requiredId)) {
                        dangerous = true;
                    }
                }
            }
            if (dangerous) {
                errors.add(newError(stepId, "DANGER"));
                if (errorKinds.get("DANGER").get("stops_attempt").equals("1")) {
                    return errors;
                }
            } else if (missing) {
                errors.add(newError(stepId, "ORDER"));
            }
            done.add(stepId);
        }
        for (Map<String, String> step : steps) {
            if (!done.contains(step.get("step_id"))) {
                errors.add(newError(step.get("step_id"), "SKIP"));
            }
        }
        return errors;
    }

    static Map<String, String> evaluateAttempt(List<Map<String, String>> errors, Map<String, Map<String, String>> errorKinds,
                                               int durationSec, Map<String, String> scenario) {
        int score = 100;
        boolean stopped = false;
        for (Map<String, String> error : errors) {
            Map<String, String> kind = errorKinds.get(error.get("kind_code"));
            score -= Integer.parseInt(kind.get("penalty"));
            if (kind.get("stops_attempt").equals("1")) {
                stopped = true;
            }
        }
        score = Math.max(score, 0);
        String result;
        if (stopped) {
            result = "остановлена";
        } else if (score >= Integer.parseInt(scenario.get("pass_score"))
                && durationSec <= Integer.parseInt(scenario.get("time_norm_sec"))) {
            result = "сдано";
        } else {
            result = "не сдано";
        }
        Map<String, String> evaluation = new LinkedHashMap<>();
        evaluation.put("score", String.valueOf(score));
        evaluation.put("result", result);
        return evaluation;
    }

    static void saveAttempt(Path dataDir, String attemptId, Map<String, String> evaluation,
                            List<Map<String, String>> errors) throws IOException {
        Path attemptsFile = dataDir.resolve("attempts.csv");
        List<Map<String, String>> attempts = readTable(attemptsFile);
        for (Map<String, String> attempt : attempts) {
            if (attempt.get("attempt_id").equals(attemptId)) {
                attempt.put("score", evaluation.get("score"));
                attempt.put("result", evaluation.get("result"));
            }
        }
        writeTable(attemptsFile, readColumns(attemptsFile), attempts);

        Path errorsFile = dataDir.resolve("errors.csv");
        List<Map<String, String>> allErrors = new ArrayList<>();
        List<String> oldIds = new ArrayList<>();
        int maxId = 0;
        for (Map<String, String> row : readTable(errorsFile)) {
            maxId = Math.max(maxId, Integer.parseInt(row.get("error_id")));
            if (row.get("attempt_id").equals(attemptId)) {
                oldIds.add(row.get("error_id"));
            } else {
                allErrors.add(row);
            }
        }
        for (int i = 0; i < errors.size(); i++) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("error_id", i < oldIds.size() ? oldIds.get(i) : String.valueOf(++maxId));
            row.put("attempt_id", attemptId);
            row.putAll(errors.get(i));
            allErrors.add(row);
        }
        allErrors.sort((a, b) -> Integer.compare(Integer.parseInt(a.get("error_id")), Integer.parseInt(b.get("error_id"))));
        writeTable(errorsFile, readColumns(errorsFile), allErrors);
    }

    static void runDemo(Path dataDir, String scenarioId) throws IOException {
        Map<String, String> scenario = indexBy(readTable(dataDir.resolve("scenarios.csv")), "scenario_id").get(scenarioId);
        if (scenario == null) {
            System.out.println("Сценарий " + scenarioId + " не найден");
            System.exit(1);
        }
        List<Map<String, String>> steps = filterBy(readTable(dataDir.resolve("steps.csv")), "scenario_id", scenarioId);
        List<Map<String, String>> dependencies = dependenciesOf(steps, readTable(dataDir.resolve("dependencies.csv")));
        List<String> order = buildDemoOrder(steps, dependencies);
        if (order == null) {
            System.out.println("В сценарии есть циклическая зависимость шагов, показать порядок нельзя");
            System.exit(1);
        }
        Map<String, Map<String, String>> stepsById = indexBy(steps, "step_id");
        System.out.println("Режим «Обучение». Сценарий " + scenarioId + ": " + scenario.get("title"));
        System.out.println("Станок: " + scenario.get("machine_model") + ", норма времени: "
                + scenario.get("time_norm_sec") + " с");
        for (int i = 0; i < order.size(); i++) {
            Map<String, String> step = stepsById.get(order.get(i));
            String mark = step.get("critical").equals("1") ? " (!)" : "";
            System.out.println("  " + (i + 1) + ". " + step.get("action") + mark);
        }
        System.out.println("(!) – критичный шаг: его пропуск перед опасным действием останавливает попытку");
    }

    static void runCheck(Path dataDir, String attemptId) throws IOException {
        Map<String, String> attempt = indexBy(readTable(dataDir.resolve("attempts.csv")), "attempt_id").get(attemptId);
        if (attempt == null) {
            System.out.println("Попытка " + attemptId + " не найдена");
            System.exit(1);
        }
        Map<String, String> internship = indexBy(readTable(dataDir.resolve("internships.csv")), "order_no")
                .get(attempt.get("order_no"));
        Map<String, String> trainee = indexBy(readTable(dataDir.resolve("workers.csv")), "worker_id")
                .get(internship.get("trainee_id"));
        String scenarioId = internship.get("scenario_id");
        Map<String, String> scenario = indexBy(readTable(dataDir.resolve("scenarios.csv")), "scenario_id").get(scenarioId);
        List<Map<String, String>> steps = filterBy(readTable(dataDir.resolve("steps.csv")), "scenario_id", scenarioId);
        Map<String, List<String>> required = requiredSteps(dependenciesOf(steps, readTable(dataDir.resolve("dependencies.csv"))));
        List<Map<String, String>> actions = orderBySeq(filterBy(readTable(dataDir.resolve("actions.csv")), "attempt_id", attemptId));
        Map<String, Map<String, String>> errorKinds = indexBy(readTable(dataDir.resolve("error_kinds.csv")), "kind_code");

        List<Map<String, String>> errors = checkActions(steps, required, actions, errorKinds);
        int durationSec = Integer.parseInt(attempt.get("duration_sec"));
        Map<String, String> evaluation = evaluateAttempt(errors, errorKinds, durationSec, scenario);
        saveAttempt(dataDir, attemptId, evaluation, errors);

        Map<String, Map<String, String>> stepsById = indexBy(steps, "step_id");
        System.out.println("Попытка " + attemptId + " от " + attempt.get("started_at") + ", стажировка № "
                + internship.get("order_no") + ", стажёр " + trainee.get("full_name"));
        System.out.println("Сценарий: " + scenario.get("title"));
        System.out.println("Действия стажёра:");
        for (Map<String, String> action : actions) {
            Map<String, String> step = stepsById.get(action.get("step_id"));
            System.out.println("  " + action.get("seq_no") + ". " + step.get("action"));
        }
        if (errors.isEmpty()) {
            System.out.println("Ошибок нет");
        } else {
            System.out.println("Ошибки:");
            for (Map<String, String> error : errors) {
                Map<String, String> step = stepsById.get(error.get("step_id"));
                Map<String, String> kind = errorKinds.get(error.get("kind_code"));
                System.out.println("  шаг " + step.get("step_no") + " «" + step.get("action") + "» – "
                        + kind.get("title") + " (−" + kind.get("penalty") + ")");
            }
        }
        System.out.println("Время: " + durationSec + " с при норме " + scenario.get("time_norm_sec") + " с");
        System.out.println("Балл: " + evaluation.get("score") + " из 100, проходной " + scenario.get("pass_score"));
        System.out.println("Результат: " + evaluation.get("result"));
        System.out.println("Протокол записан в attempts.csv и errors.csv");
    }
}
