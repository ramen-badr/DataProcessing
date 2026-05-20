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
