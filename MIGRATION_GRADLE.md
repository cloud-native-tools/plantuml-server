# Maven to Gradle Migration - PlantUML Server

## Summary of Changes

This guide describes the migration of the PlantUML Server project from Maven to Gradle, including:
- ✅ Removal of JDK8 configuration (`pom.jdk8.xml`)
- ✅ Single configuration for Java 11+
- ✅ Conversion of all dependencies
- ✅ Conversion of Maven plugins to Gradle
- ✅ Kotlin DSL for Gradle configuration (.kts files)

## Files Created

1. **`settings.gradle.kts`** - Project configuration
2. **`build.gradle.kts`** - Main build configuration (replaces `pom.xml` and `pom.parent.xml`)
3. **`gradle.properties`** - Configurable properties

## Files to Remove

After validating the migration, you can delete:
- `pom.xml`
- `pom.jdk8.xml`
- `pom.parent.xml`

## Installing the Gradle Wrapper

Run the following commands to initialize Gradle:

```bash
# Generate Gradle wrapper
gradle wrapper --gradle-version 8.5

# Make script executable (Linux/Mac)
chmod +x gradlew
```

This will create:
- `gradlew` (Linux/Mac)
- `gradlew.bat` (Windows)
- `gradle/wrapper/` (wrapper files)

## Gradle Equivalent Commands

| Maven | Gradle |
|-------|--------|
| `mvn clean` | `./gradlew clean` |
| `mvn compile` | `./gradlew compileJava` |
| `mvn test` | `./gradlew test` |
| `mvn test -DskipTests=false` | `./gradlew test -PskipTests=false` |
| `mvn package` | `./gradlew war` |
| `mvn install` | `./gradlew build` |
| `mvn jetty:run` | `./gradlew appRun` |
| `mvn checkstyle:check` | `./gradlew checkstyleMain` |
| `mvn versions:display-dependency-updates` | `./gradlew dependencyUpdates` |
| `mvn dependency:tree` | `./gradlew dependencies` |

## Main Gradle Commands

### Build and Packaging
```bash
# Complete build (compile, test, war)
./gradlew build

# Build without tests
./gradlew build -x test

# Create WAR only
./gradlew war

# Clean + build
./gradlew clean build
```

### Tests
```bash
# Run tests
./gradlew test -PskipTests=false

# Tests with custom parameter
./gradlew test -PskipTests=false -Dsystem.test.server=http://localhost:8080/plantuml
```

### Local Jetty Server
```bash
# Start Jetty (equivalent to mvn jetty:run)
./gradlew appRun

# Stop Jetty
./gradlew appStop
```

### Code Quality
```bash
# Check code style (Checkstyle)
./gradlew checkstyleMain checkstyleTest

# Generate Javadoc
./gradlew javadoc

# Check dependency updates
./gradlew dependencyUpdates
```

### Dependency Analysis
```bash
# Dependency tree
./gradlew dependencies

# View dependency conflicts
./gradlew dependencyInsight --dependency <name>

# Example:
./gradlew dependencyInsight --dependency jetty-server
```

## Important Differences

### 1. Java Configuration
- **Before (Maven)**: `pom.xml` with Java 11 + `pom.jdk8.xml` with Java 8
- **After (Gradle)**: Single Java 11 configuration in `build.gradle.kts`

### 2. Dependency Scopes
Maven → Gradle:
- `compile` → `implementation`
- `provided` → `providedCompile` (custom configuration)
- `runtime` → `runtimeOnly`
- `test` → `testImplementation`

### 3. CSS/JS Minification
The Maven plugin `resources-optimizer-maven-plugin` has no direct Gradle equivalent.

**Recommended options:**
- Use a frontend build tool (Webpack, Vite, Rollup)
- Use the plugin `com.github.eirslett:frontend-gradle-plugin`
- Or create a custom Gradle task with Node.js tools

For now, a placeholder task `optimizeWebResources` is included.

### 4. Watcher for Development
The `fizzed-watcher-maven-plugin` can be replaced by:
- Gradle `--continuous`: `./gradlew appRun --continuous`
- External tools: `nodemon`, `watchexec`, etc.

## Configuration for Different Environments

### Local Development
```bash
# Start with auto-reload
./gradlew appRun --continuous

# Disable CSS/JS minification
./gradlew build -PwithoutCSSJSCompress=true
```

### Build for Tomcat
```bash
# Change apache-jsp scope
./gradlew build -PapacheJspScope=implementation
```

### Production Build
```bash
# Complete build with tests and optimizations
./gradlew clean build -PskipTests=false -PwithoutCSSJSCompress=false
```

## File Structure

```
plantuml-server/
├── build.gradle.kts          # Main build configuration
├── settings.gradle.kts        # Project configuration
├── gradle.properties          # Configurable properties
├── gradlew                    # Gradle script (Linux/Mac)
├── gradlew.bat               # Gradle script (Windows)
├── gradle/
│   └── wrapper/              # Gradle wrapper files
├── src/
│   ├── main/
│   │   ├── java/
│   │   ├── webapp/
│   │   └── config/
│   └── test/
├── build/                    # Build directory (equivalent to target/)
│   ├── classes/
│   ├── libs/                 # Contains generated WAR
│   └── reports/
└── (remove pom.xml files)
```

## Step-by-Step Migration

### Step 1: Create Gradle Files
1. Copy `settings.gradle.kts` to root
2. Copy `build.gradle.kts` to root
3. Copy `gradle.properties` to root

### Step 2: Initialize Wrapper
```bash
gradle wrapper --gradle-version 8.5
```

### Step 3: First Build
```bash
./gradlew clean build -x test
```

### Step 4: Verify Generated WAR
```bash
# WAR is in build/libs/plantuml.war
ls -lh build/libs/
```

### Step 5: Test Jetty Server
```bash
./gradlew appRun
# Access http://localhost:8080/plantuml
```

### Step 6: Run Tests
```bash
./gradlew test -PskipTests=false
```

### Step 7: Remove Maven Files
Once everything works:
```bash
rm pom.xml pom.jdk8.xml pom.parent.xml
rm -rf target/
```

## Points of Attention

### ⚠️ CSS/JS Minification
Web resource minification is not yet configured. You need to:
1. Either integrate a frontend tool (recommended)
2. Or create a custom Gradle task
3. Or minify resources manually

### ⚠️ Eclipse WTP
The Gradle WTP plugin is not included by default. If you need Eclipse WTP support, you may need to:
1. Add the `eclipse-wtp` plugin
2. Run `./gradlew eclipse`
3. Refresh the project in Eclipse

### ⚠️ Monaco Editor
Monaco Editor unpacking is handled by the `unpackWebJars` task. Verify that files are properly extracted to `build/resources/main/`.

## Migration Verification

Checklist:
- [ ] `./gradlew build` works without errors
- [ ] WAR is generated in `build/libs/plantuml.war`
- [ ] `./gradlew appRun` starts Jetty correctly
- [ ] Application is accessible at http://localhost:8080/plantuml
- [ ] Tests pass with `./gradlew test -PskipTests=false`
- [ ] Checkstyle works with `./gradlew checkstyleMain`
- [ ] Dependencies are correct: `./gradlew dependencies`

## Support and Documentation

- Gradle Documentation: https://docs.gradle.org
- Gretty Plugin (Jetty): https://github.com/gretty-gradle-plugin/gretty
- Maven to Gradle Migration: https://docs.gradle.org/current/userguide/migrating_from_maven.html
- Kotlin DSL Primer: https://docs.gradle.org/current/userguide/kotlin_dsl.html

## Frequently Asked Questions

**Q: How to add a new dependency?**
```kotlin
dependencies {
    implementation("group:artifact:version")
}
```

**Q: How to change Java version?**
Modify in `build.gradle.kts`:
```kotlin
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```

**Q: How to enable tests by default?**
In `gradle.properties`:
```properties
skipTests=false
```

**Q: How to debug the build?**
```bash
./gradlew build --debug
./gradlew build --info
./gradlew build --stacktrace
```

**Q: What are the benefits of Kotlin DSL (.kts)?**
- Type-safe configuration
- IDE auto-completion and navigation
- Compile-time error checking
- Better refactoring support
- Modern Kotlin syntax
