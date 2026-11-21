import java.util.*;

final class ScenarioGraph {
    private final List<Step> steps;
    private final List<Dependency> dependencies;
    private final Map<String, Step> byId = new HashMap<>();
    private final Map<String, List<Dependency>> requirements = new HashMap<>();

    ScenarioGraph(List<Step> steps, List<Dependency> dependencies) {
        this.steps = List.copyOf(steps);
        this.dependencies = List.copyOf(dependencies);
        for (Step step : steps) {
            byId.put(step.id(), step);
        }
        for (Dependency dependency : dependencies) {
            requirements.computeIfAbsent(dependency.stepId(), id -> new ArrayList<>()).add(dependency);
        }
    }

    List<Step> steps() {
        return steps;
    }

    Step step(String id) {
        return byId.get(id);
    }

    List<Dependency> requiredFor(String id) {
        return requirements.getOrDefault(id, List.of());
    }

    Set<String> criticalStepIds() {
        Set<String> critical = new HashSet<>();
        for (Dependency dependency : dependencies) {
            if (dependency.dangerous()) {
                critical.add(dependency.requiredId());
            }
        }
        return critical;
    }

    Optional<List<Step>> demoOrder() {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        for (Step step : steps) {
            inDegree.put(step.id(), requiredFor(step.id()).size());
            dependents.put(step.id(), new ArrayList<>());
        }
        for (Dependency dependency : dependencies) {
            dependents.get(dependency.requiredId()).add(dependency.stepId());
        }
        PriorityQueue<Step> ready = new PriorityQueue<>(Comparator.comparingInt(Step::number));
        for (Step step : steps) {
            if (inDegree.get(step.id()) == 0) {
                ready.add(step);
            }
        }
        List<Step> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            Step step = ready.remove();
            order.add(step);
            for (String next : dependents.get(step.id())) {
                if (inDegree.merge(next, -1, Integer::sum) == 0) {
                    ready.add(step(next));
                }
            }
        }
        return order.size() == steps.size() ? Optional.of(order) : Optional.empty();
    }
}
