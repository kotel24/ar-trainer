import java.io.IOException;
import java.util.List;
import java.util.Set;

final class DemoCommand implements TrainerCommand {
    @Override
    public int run(CsvStore store, String id) throws IOException {
        Scenario scenario = store.scenario(id);
        if (scenario == null) {
            System.out.println("Сценарий " + id + " не найден");
            return 1;
        }
        ScenarioGraph graph = store.graph(scenario);
        List<Step> order = graph.demoOrder().orElse(null);
        if (order == null) {
            System.out.println("В сценарии есть циклическая зависимость шагов, показать порядок нельзя");
            return 1;
        }
        Set<String> critical = graph.criticalStepIds();
        System.out.println("Режим «Обучение». Сценарий " + id + ": " + scenario.title());
        System.out.println("Станок: " + scenario.machine() + ", норма времени: " + scenario.timeLimit() + " с");
        for (int i = 0; i < order.size(); i++) {
            Step step = order.get(i);
            String mark = critical.contains(step.id()) ? " (!)" : "";
            System.out.println("  " + (i + 1) + ". " + step.action() + mark);
        }
        System.out.println("(!) – критичный шаг: опасное действие без него останавливает попытку");
        return 0;
    }
}
