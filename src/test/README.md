The tests for Filament are mostly Gradle projects that are automatically tested using JUnit 5.
I couldn't get any of the tests to work due to not being able to apply loom correctly for some reason.
If someone wants to fix these in the future, be my guest, but for now I'll just be building with `-x test`.

## Structure

### `/projects/sharedData`

Data files shared between tests. This includes large files like a build of Yarn mappings.

### `/projects/javadocLint`

Test project for the `javadocLint` task (`JavadocLintTask`).

### `/projects/unpickDef`

Test project for the `combineUnpickDefinitions` and `remapUnpickDefinitionsIntermediary` tasks
(`CombineUnpickDefinitionsTask` and `RemapUnpickDefinitionsTask`).
