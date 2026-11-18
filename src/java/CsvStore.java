import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

final class CsvStore {
    private static final String SEPARATOR = ";";
    private final Path dataDir;

    CsvStore(Path dataDir) {
        this.dataDir = dataDir;
    }

    private <T> List<T> load(String file, Function<Map<String, String>, T> mapper) throws IOException {
        return readTable(dataDir.resolve(file)).stream().map(mapper).toList();
    }

    Scenario scenario(String id) throws IOException {
        return load("scenarios.csv", r -> new Scenario(r.get("scenario_id"), r.get("title"),
                r.get("machine_model"), r.get("time_norm_sec"), r.get("pass_score")))
                .stream().filter(s -> s.id().equals(id)).reduce((first, last) -> last).orElse(null);
    }

    Attempt attempt(String id) throws IOException {
        return load("attempts.csv", r -> new Attempt(r.get("attempt_id"), r.get("order_no"),
                r.get("started_at"), number(r, "duration_sec"), r.get("score"), r.get("result")))
                .stream().filter(a -> a.id().equals(id)).reduce((first, last) -> last).orElse(null);
    }

    Internship internship(String orderNo) throws IOException {
        return load("internships.csv", r -> new Internship(r.get("order_no"), r.get("order_date"),
                r.get("trainee_id"), r.get("mentor_id"), r.get("master_id"), r.get("scenario_id"),
                r.get("machine_no"), number(r, "shifts"), r.get("conclusion"),
                r.get("conclusion_date"), r.get("completion_date"), r.get("admission_order_no")))
                .stream().filter(i -> i.orderNo().equals(orderNo)).reduce((first, last) -> last).orElseThrow();
    }

    Worker worker(String id) throws IOException {
        return load("workers.csv", r -> new Worker(r.get("worker_id"), r.get("full_name"),
                r.get("profession"))).stream().filter(w -> w.id().equals(id))
                .reduce((first, last) -> last).orElseThrow();
    }

    ScenarioGraph graph(Scenario scenario) throws IOException {
        List<Step> steps = load("steps.csv", r -> new Step(r.get("step_id"), r.get("scenario_id"),
                number(r, "step_no"), r.get("action"))).stream()
                .filter(step -> step.scenarioId().equals(scenario.id())).toList();
        Set<String> ids = new HashSet<>();
        for (Step step : steps) {
            ids.add(step.id());
        }
        List<Dependency> dependencies = load("dependencies.csv", r -> new Dependency(r.get("step_id"),
                r.get("required_step_id"), "1".equals(r.get("dangerous")))).stream()
                .filter(d -> ids.contains(d.stepId()) && ids.contains(d.requiredId())).toList();
        return new ScenarioGraph(steps, dependencies);
    }

    List<AttemptAction> actions(String id) throws IOException {
        List<AttemptAction> actions = load("actions.csv", r -> new AttemptAction(r.get("attempt_id"),
                r.get("seq_no"), r.get("step_id"), number(r, "time_sec"))).stream()
                .filter(a -> a.attemptId().equals(id)).toList();
        List<AttemptAction> ordered = new ArrayList<>(Collections.nCopies(actions.size(), null));
        for (AttemptAction action : actions) {
            ordered.set(action.sequenceValue() - 1, action);
        }
        return ordered;
    }

    Map<String, ErrorKind> errorKinds() throws IOException {
        Map<String, ErrorKind> kinds = new HashMap<>();
        for (ErrorKind kind : load("error_kinds.csv", r -> new ErrorKind(r.get("kind_code"),
                r.get("title"), r.get("penalty"), "1".equals(r.get("stops_attempt"))))) {
            kinds.put(kind.code(), kind);
        }
        return kinds;
    }

    private static int number(Map<String, String> row, String key) {
        return Integer.parseInt(row.get(key));
    }

    private static List<String> readColumns(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        return List.of(lines.get(0).split(SEPARATOR, -1));
    }

    private static List<Map<String, String>> readTable(Path file) throws IOException {
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

    private static void writeTable(Path file, List<String> columns, List<Map<String, String>> rows) throws IOException {
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

    void save(Attempt attempt, Evaluation evaluation,
                            List<AttemptError> errors) throws IOException {
        String attemptId = attempt.id();
        Path attemptsFile = dataDir.resolve("attempts.csv");
        List<Map<String, String>> attempts = readTable(attemptsFile);
        for (Map<String, String> row : attempts) {
            if (row.get("attempt_id").equals(attemptId)) {
                row.put("score", Integer.toString(evaluation.score()));
                row.put("result", evaluation.result());
            }
        }
        writeTable(attemptsFile, readColumns(attemptsFile), attempts);

        Path errorsFile = dataDir.resolve("errors.csv");
        List<Map<String, String>> allErrors = new ArrayList<>();
        for (Map<String, String> row : readTable(errorsFile)) {
            if (!row.get("attempt_id").equals(attemptId)) {
                allErrors.add(row);
            }
        }
        for (AttemptError error : errors) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("attempt_id", attemptId);
            row.put("step_id", error.stepId());
            row.put("kind_code", error.kindCode());
            allErrors.add(row);
        }
        allErrors.sort((a, b) -> Integer.compare(Integer.parseInt(a.get("attempt_id")), Integer.parseInt(b.get("attempt_id"))));
        writeTable(errorsFile, readColumns(errorsFile), allErrors);
    }

}
