# Data Processing and Storage Labs

## X2 demo (xml-parser)

**Goal:** parse `people.xml`, merge person records, and write `unified_people.xml` using JAXB with XSD validation.

### Steps

1. Open a terminal in the repository root.
2. Build the `xml-parser` module:
   ```bash
   cd xml-parser
   ./gradlew test
   ```
3. Run the demo:
    - **IDE (recommended):** run `org.example.Main` with working directory `xml-parser`.
    - The program reads `people.xml` and produces `unified_people.xml`.
    - JAXB validates the output against `src/main/resources/people.xsd`.

### What to check

- `unified_people.xml` appears in `xml-parser/`.
- Console output lists any consistency issues found during parsing (children/siblings counts).

### X2: как устроено решение

**Что добавилось:**
- JAXB-модель для целевого формата: `org.example.jaxb` (`People`, `PersonRecord`, `PersonReference`, `ChildReference`, `Siblings`).
- XSD-схема результата: `src/main/resources/people.xsd`.
- Сборка унифицированной модели в `XMLParser.buildPeopleModel()`:
  - нормализация `id` до валидного `xs:ID` и обеспечение уникальности (`normalizeXmlId`);
  - построение карт `id`/полное имя для связывания сущностей;
  - перевод связей (spouse/parents/children/siblings) в `IDREF` при наличии совпадения, иначе — сохранение имени;
  - сохранение типа ребенка (`child/son/daughter`).
- Маршаллинг через JAXB с привязкой схемы (`writeUnifiedXML`), чтобы выходной файл проходил XSD-валидацию.

**Зачем это нужно:**
- Получить единый, формально описанный формат `unified_people.xml`.
- Гарантировать корректные ссылки между людьми и валидные XML-идентификаторы.
- Автоматически валидировать результат и быстрее находить проблемы в исходных данных.

### Построчное описание кода (xml-parser)

#### `src/main/java/org/example/Main.java`
- **1–4**: объявление пакета и импортов.
- **6–7**: класс `Main` и точка входа `main`.
- **8–14**: создание `XMLParser` и запуск парсинга `people.xml`.
- **15–28**: вывод статистики и найденных проблем согласованности.
- **30–35**: запись `unified_people.xml` через JAXB с XSD-валидацией.
- **36–48**: пример вывода информации о первых 30 людях.
- **50–52**: обёртка ошибок в `RuntimeException`.

#### `src/main/java/org/example/XMLParser.java`
- **1–18**: импорты (JAXB, StAX, XSD, коллекции, regex).
- **20–23**: поля: карта людей, список проблем, regex для `xs:ID`.
- **25–89**: `parse()` — потоковый разбор XML, создание `Person`, обработка атрибутов и текста.
- **91–187**: `processAttributes()` — чтение атрибутов (siblings, counts, child id, id/firstname/surname/gender/spouse/parent).
- **189–276**: `processElementText()` — чтение текстовых значений, нормализация пола, фильтрация `NONE/UNKNOWN`.
- **278–303**: `savePerson()` — выбор ключа, merge-дубликатов, запуск валидации.
- **305–351**: `mergePersons()` — объединение полей и списков.
- **353–360**: `writeUnifiedXML()` — сбор модели и JAXB-маршаллинг с XSD.
- **362–368**: геттеры `persons` и `validationIssues`.
- **370–377**: `loadSchema()` — загрузка `people.xsd` из ресурсов.
- **379–433**: `buildPeopleModel()` — создание `PersonRecord`, индексы по id/имени, связка ссылок.
- **435–448**: `buildFullName()` — безопасная сборка полного имени.
- **450–467**: `buildReference()` — ссылка по id/имени или сохранение имени.
- **469–479**: `buildReferences()` — массовое построение ссылок.
- **481–507**: `buildChildReferences()` — ссылки на детей с учётом `id/name/type`.
- **509–522**: `buildSiblings()` — группа братьев/сестёр/общих.
- **524–530**: `trimToNull()` — trim и преобразование пустых строк в `null`.
- **532–542**: `normalizeXmlId()` — валидный `xs:ID` и устранение коллизий.

#### `src/main/java/org/example/Person.java`
- **1–16**: поля данных человека (id, имена, супруг, связи).
- **18–21**: числовые поля для валидации.
- **23–28**: конструкторы.
- **30–59**: методы добавления связей с проверками.
- **62–82**: `validate()` — контроль количества детей/братьев/сестёр.
- **84–179**: геттеры и сеттеры.

#### `src/main/java/org/example/Child.java`
- **1–7**: поля `id/name/type`.
- **8–15**: конструкторы.
- **17–39**: геттеры и сеттеры.

#### `src/main/java/org/example/jaxb/People.java`
- **1–16**: JAXB-аннотации и поля `count/persons`.
- **17–22**: конструкторы.
- **24–39**: геттеры/сеттеры, установка `count` от размера списка.

#### `src/main/java/org/example/jaxb/PersonRecord.java`
- **1–18**: JAXB-аннотации и порядок элементов.
- **20–52**: описание JAXB-полей (id, имя, связи, числа).
- **53–134**: геттеры и сеттеры.

#### `src/main/java/org/example/jaxb/PersonReference.java`
- **1–16**: JAXB-аннотации для `ref` (IDREF) и `name`.
- **17–23**: конструкторы.
- **25–37**: геттеры и сеттеры.

#### `src/main/java/org/example/jaxb/ChildReference.java`
- **1–18**: JAXB-аннотации для `ref/name/type`.
- **20–22**: конструктор.
- **23–45**: геттеры и сеттеры.

#### `src/main/java/org/example/jaxb/Siblings.java`
- **1–19**: JAXB-поля для братьев/сестёр/общих.
- **21–22**: конструктор.
- **24–45**: геттеры и сеттеры.

### ER-диаграмма (X2)

```mermaid
erDiagram
    PEOPLE ||--o{ PERSON : contains
    PEOPLE {
        xs:int count
    }

    PERSON ||--o| PERSON_REF : spouse
    PERSON ||--o{ PERSON_REF : parent
    PERSON ||--o{ CHILD_REF : child
    PERSON ||--o{ PERSON_REF : brother
    PERSON ||--o{ PERSON_REF : sister
    PERSON ||--o{ PERSON_REF : sibling

    PERSON {
        xs:ID id PK
        xs:string firstname
        xs:string lastname
        xs:string gender
        xs:int children_number
        xs:int siblings_number
    }

    PERSON_REF {
        xs:IDREF ref
        xs:string name
    }

    CHILD_REF {
        xs:IDREF ref
        xs:string name
        enum(child|son|daughter) type
    }

    PERSON_REF o|--|| PERSON : ref
    CHILD_REF o|--|| PERSON : ref
```

**Ограничения и кардинальности:**
- `people.count` — обязательный атрибут.
- `person.id` — обязательный `xs:ID` (уникальный в документе).
- `spouse` — 0..1 ссылка на человека.
- `parents/parent` — 0..* ссылок.
- `children/child` — 0..* ссылок, `type` обязателен и ограничен `child|son|daughter`.
- `siblings` включает отдельные множества `brother/sister/sibling` (каждое 0..*).
- Каждая ссылка (`PersonRef`) хранит либо `ref` (`xs:IDREF`), либо `name` (оба поля опциональны по XSD).

### Изменения
- Добавлена ER-диаграмма X2 и перечень ограничений схемы.
