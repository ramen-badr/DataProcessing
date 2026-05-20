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
