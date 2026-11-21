# Взаимное рецензирование кода

## 1. Кого рецензировали

- **Автор проверенного кода** — Пестряков Максим Александрович, ИКБО-31-24. [Сведения об авторе](https://github.com/kokuyouu/practice/blob/04f56a4eb05058dab43b197d4d0966e34397cd6d/README.md)
- **Рецензент** — Котельников Дмитрий Владимирович, ИКБО-31-24.
- **Код для проверки** — Java-версия проекта «Контроль готовности этапа иммерсивной кампании». [Репозиторий](https://github.com/kokuyouu/practice) · [Проверенный коммит `04f56a4eb05058dab43b197d4d0966e34397cd6d`](https://github.com/kokuyouu/practice/commit/04f56a4eb05058dab43b197d4d0966e34397cd6d)

## 2. Что проверяли

| Критерий | Результат |
| --- | --- |
| Соответствие диаграмме классов | В дереве проверенного коммита есть ER-диаграмма и схемы алгоритмов. Диаграмму классов для этой версии в репозитории не нашёл, поэтому соответствие UML осталось непроверенным. |
| Наследование и интерфейсы | `ReworkTask` расширяет `Task` и переопределяет `validateContext()`, сохраняя базовую проверку и добавляя проверку этапа замечания. `BriefRule` объединяет три правила валидации брифа, а `ResultPresenter` используется для текстового и JSON-вывода. |
| Коллекции | `CampaignData` хранит списки через `List.copyOf`. Проверил поиск связанных записей по ID и выявил повторные линейные обходы при загрузке (замечание № 3). |
| Читаемость | Проследил обработку от разбора аргументов в `CommandLine` до вызова операции в `Main`. Разделение чтения данных, бизнес-правил и вывода помогает проследить поток. Допустимость параметров отдельных команд проверяется непоследовательно (замечание № 1). |
| Запуск по README | Собрал исходники с `javac --release 17` и Gson 2.13.2. Команда `Main demo --order O1 --as-of 2026-10-31` дала ожидаемый вывод из README. При дополнительной проверке `readiness` обнаружилось расхождение с процедурной версией (замечание № 1). |

## 3. Замечания к проверенному коду

Ниже приведены тексты опубликованных замечаний. В каждом указаны место в коде, проблема и предлагаемая правка.

### [№ 1. `readiness` молча игнорирует `--order`](https://github.com/kokuyouu/practice/issues/1)

> На штатных данных запустил `readiness --stage S1 --order O2`. Java завершилась с кодом `0` и сообщила, что этап `S1` готов к согласованию, хотя он относится к заказу `O1`. Python-версия отклонила ту же команду с кодом `2`.
>
> Предлагаю проверять допустимые параметры для каждой команды. Для `readiness` параметр `--order` должен приводить к понятной ошибке и коду `2`. [CommandLine.java, строки 27–55](https://github.com/kokuyouu/practice/blob/04f56a4eb05058dab43b197d4d0966e34397cd6d/src/java/main/CommandLine.java#L27-L55) · [Main.java, строка 58](https://github.com/kokuyouu/practice/blob/04f56a4eb05058dab43b197d4d0966e34397cd6d/src/java/Main.java#L58)

### [№ 2. Повторяющееся поле `status` в JSON меняет результат проверки](https://github.com/kokuyouu/practice/issues/2)

> В копии `sample_data.json` указал у заказа `O1` два поля `status`: сначала `completed`, затем `in_progress`. Команда `readiness --stage S1` приняла файл и сообщила, что этап готов. Первое значение было потеряно при построении JSON-объекта.
>
> Предлагаю отклонять повторяющиеся имена полей при чтении JSON. Для этого примера ожидается ошибка с указанием поля `status` и кодом `2`, без вывода о готовности этапа. [JsonDataSource.java, строки 38–53](https://github.com/kokuyouu/practice/blob/04f56a4eb05058dab43b197d4d0966e34397cd6d/src/java/main/JsonDataSource.java#L38-L53)

### [№ 3. Проверка связей при загрузке повторно обходит списки](https://github.com/kokuyouu/practice/issues/3)

> Для каждой связи «задача — материал» `validateContext()` линейно ищет обе записи по ID. Для каждой задачи `Task.validateContext()` также ищет этап и просматривает участников заказа. При росте этих списков проверка может занимать квадратичное время. Она выполняется при любой команде, включая `brief`.
>
> Предлагаю один раз построить `Map` сущностей по ID и `Set` пар «заказ — сотрудник», как в процедурной версии. После правки штатные результаты и диагностика неверных связей должны сохраниться. [CampaignData.java, строки 116–135](https://github.com/kokuyouu/practice/blob/04f56a4eb05058dab43b197d4d0966e34397cd6d/src/java/core/CampaignData.java#L116-L135) · [Task.java, строки 63–74](https://github.com/kokuyouu/practice/blob/04f56a4eb05058dab43b197d4d0966e34397cd6d/src/java/core/Task.java#L63-L74)

## 4. Что исправили по замечаниям к своему коду

Рецензент — Пестряков Максим Александрович (GitHub: `kokuyouu`). Исправления опубликованы в ветке [`review-fixes`](https://github.com/kotel24/ar-trainer/tree/review-fixes). Ссылки на исходные строки приведены в текстах замечаний.

### [№ 1. criticalSteps() стоит переименовать в criticalStepIds()](https://github.com/kotel24/ar-trainer/issues/1)

> https://github.com/kotel24/ar-trainer/blob/994846f36509bf8bf74992fe481fd2a76dc72f08/src/java/ScenarioGraph.java#L32
> Метод возвращает набор строковых ID, а соседний steps() возвращает объекты Step

**Исправлено** в [`e093330`](https://github.com/kotel24/ar-trainer/commit/e0933309d258dcefc0291b1cb8c81793d43497bc). Переименовал метод и обновил его вызов в `DemoCommand` и обозначение в UML.

[Ответ на замечание](https://github.com/kotel24/ar-trainer/issues/1#issuecomment-7061917791)

> Переименовал `criticalSteps()` в `criticalStepIds()` и обновил вызовы. Исправление в ветке `review-fixes`: https://github.com/kotel24/ar-trainer/commit/e0933309d258dcefc0291b1cb8c81793d43497bc

### [№ 2. Параметры CheckRule.matches() скрывают условия проверки](https://github.com/kotel24/ar-trainer/issues/2)

> https://github.com/kotel24/ar-trainer/blob/994846f36509bf8bf74992fe481fd2a76dc72f08/src/java/CheckRule.java#L8
> Предлагаю заменить missing на hasUnperformedPrerequisite, dangerous на hasUnmetCriticalPrerequisite, а firstExecution на firstOccurrenceOfStep

**Исправлено** в [`2c78895`](https://github.com/kotel24/ar-trainer/commit/2c7889594b4576cdc21c1aaefe0900161f145839). Уточнил имена параметров в базовом классе и реализациях, а также имена переменных в `AttemptChecker`.

[Ответ на замечание](https://github.com/kotel24/ar-trainer/issues/2#issuecomment-7061923726)

> Уточнил параметры `CheckRule.matches()` и обновил места вызова. [Коммит с исправлением](https://github.com/kotel24/ar-trainer/commit/2c7889594b4576cdc21c1aaefe0900161f145839).

### [№ 3. Опечатка в ID шага молча отключает обязательную зависимость](https://github.com/kotel24/ar-trainer/issues/3)

> https://github.com/kotel24/ar-trainer/blob/1426dd20ceb94b141ac5abcde0348d84ce4e2d4f/src/java/CsvStore.java#L56
> CsvStore отбрасывает зависимость с неизвестным required_step_id. В проверенном сценарии попытка вместо остановки получает «сдано».

**Исправлено** в [`d606d9c`](https://github.com/kotel24/ar-trainer/commit/d606d9c132789fc3c0be5f9590379b8611ce6771). Теперь оба шага должны существовать и принадлежать одному сценарию. Проверил `demo` и `check` на неизвестных ID и межсценарной связи. Обе команды завершились с ошибкой, файлы `attempts.csv` и `errors.csv` сохранились без изменений.

[Ответ на замечание](https://github.com/kotel24/ar-trainer/issues/3#issuecomment-7061925428)

> Добавил проверку идентификаторов шагов при загрузке `dependencies.csv`. [Коммит с исправлением](https://github.com/kotel24/ar-trainer/commit/d606d9c132789fc3c0be5f9590379b8611ce6771).

### [№ 4. Уточнить имена полей в Dependency](https://github.com/kotel24/ar-trainer/issues/4)

> https://github.com/kotel24/ar-trainer/blob/994846f36509bf8bf74992fe481fd2a76dc72f08/src/java/Dependency.java#L1
> stepId – шаг, который зависит от другого, а requiredId – шаг, который должен быть выполнен раньше. Предлагаю dependentStepId и requiredStepId, чтобы направление зависимости читалось из объявления

**Исправлено** в [`d0d5572`](https://github.com/kotel24/ar-trainer/commit/d0d5572ca4d45f7b7cf47b21bcf02272385e5582). Применил предложенные имена и обновил обращения к полям. Заголовки CSV сохранены.

[Ответ на замечание](https://github.com/kotel24/ar-trainer/issues/4#issuecomment-7061925750)

> Уточнил имена полей `Dependency` и обновил их использования. [Коммит с исправлением](https://github.com/kotel24/ar-trainer/commit/d0d5572ca4d45f7b7cf47b21bcf02272385e5582).

### [№ 5. Разнести реализации CheckRule по отдельным файлам](https://github.com/kotel24/ar-trainer/issues/5)

> https://github.com/kotel24/ar-trainer/blob/994846f36509bf8bf74992fe481fd2a76dc72f08/src/java/CheckRule.java#L15
> DangerousDependencyRule и OrderRule объявлены в CheckRule.java, а создаются в AttemptChecker.java.

**Исправлено** в [`b1e5d0c`](https://github.com/kotel24/ar-trainer/commit/b1e5d0ce63391243b1969eca9d63433ec7ac1de6). Перенёс классы в `DangerousDependencyRule.java` и `OrderRule.java`. Правило опасной зависимости по-прежнему применяется первым.

[Ответ на замечание](https://github.com/kotel24/ar-trainer/issues/5#issuecomment-7061926132)

> Вынес `DangerousDependencyRule` и `OrderRule` в отдельные файлы. [Коммит с исправлением](https://github.com/kotel24/ar-trainer/commit/b1e5d0ce63391243b1969eca9d63433ec7ac1de6).

### Проверка после исправлений

После исправлений обе версии собраны на Java 21. На штатных данных совпали вывод `demo`, результаты попыток 1–3 и содержимое CSV-протоколов. Итоги попыток — 0 («остановлена»), 75 («не сдано») и 100 («сдано») баллов.
