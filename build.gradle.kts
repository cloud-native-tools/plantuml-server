import java.text.SimpleDateFormat
import java.util.Date
import org.gradle.api.artifacts.Configuration


plugins {
    java
    war
    id("org.gretty") version "4.1.10"
    id("com.github.ben-manes.versions") version "0.51.0"
    eclipse
}

group = "org.sourceforge.plantuml"
version = "1-SNAPSHOT"
description = "PlantUML Servlet"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

// Project properties
val plantumlVersion = "1.2025.10"
val jettyVersion = "11.0.15"
val monacoEditorVersion = "0.36.1"
val jlatexmathVersion = "1.0.7"
val batikVersion = "1.19"
val fopVersion = "2.11"
val junitVersion = "5.9.3"
val seleniumVersion = "4.10.0"

val apacheJspScope: String by project
val skipTests: String by project
val withoutCSSJSCompress: String by project
val jettyHttpPort: String by project
val jettyContextPath: String by project
val wtpContextName: String by project


dependencies {
  // PlantUML core
  implementation("net.sourceforge.plantuml:plantuml:$plantumlVersion")

  // Éditeur Monaco (webjar)
  runtimeOnly("org.webjars.npm:monaco-editor:$monacoEditorVersion")

  // Servlet API fournie par le conteneur (Jetty/Gretty) au runtime
  providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")

  // jlatexmath
  runtimeOnly("org.scilab.forge:jlatexmath:$jlatexmathVersion")
  runtimeOnly("org.scilab.forge:jlatexmath-font-greek:$jlatexmathVersion")
  runtimeOnly("org.scilab.forge:jlatexmath-font-cyrillic:$jlatexmathVersion")

  // PDF / SVG : Batik & FOP
  runtimeOnly("org.apache.xmlgraphics:batik-svgrasterizer:$batikVersion")
  runtimeOnly("org.apache.xmlgraphics:batik-svggen:$batikVersion")
  runtimeOnly("org.apache.xmlgraphics:fop-core:$fopVersion")

  // JSON pour MCP
  implementation("com.google.code.gson:gson:2.10.1")

  // Eclipse ELK
  implementation("org.eclipse.elk:org.eclipse.elk.core:0.9.1")
  implementation("org.eclipse.elk:org.eclipse.elk.alg.layered:0.9.1")
  implementation("org.eclipse.elk:org.eclipse.elk.alg.mrtree:0.9.1")

  // Jetty pour le serveur embarqué des tests (EmbeddedJettyServer)
  testImplementation("org.eclipse.jetty:jetty-server:$jettyVersion")
  testImplementation("org.eclipse.jetty:jetty-webapp:$jettyVersion")
  testImplementation("org.eclipse.jetty:jetty-servlet:$jettyVersion")
  testImplementation("org.eclipse.jetty:jetty-http:$jettyVersion")
  testImplementation("org.eclipse.jetty:jetty-io:$jettyVersion")
  testImplementation("org.eclipse.jetty:jetty-util:$jettyVersion")

  // Tests JUnit 5
  testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
  testImplementation("org.junit.platform:junit-platform-suite-api:1.9.3")

  // Tests Selenium
  testImplementation("org.seleniumhq.selenium:selenium-java:$seleniumVersion")
  testImplementation("io.github.bonigarcia:webdrivermanager:5.3.3")
}

// WAR configuration
tasks.war {
    archiveFileName.set("plantuml.war")

    // Configure web resources with filtering
    from("src/main/webapp") {
        include("*.jspf")
        filter { line: String ->
            line.replace("\${project.version}", version.toString())
                .replace("\${timestamp}", SimpleDateFormat("yyyyMMdd-HHmm").format(Date()))
        }
    }
}

// Test configuration
tasks.test {
    useJUnitPlatform()
    enabled = skipTests.toBoolean().not()

    systemProperty(
        "system.test.server",
        System.getProperty("system.test.server", "http://localhost:8080/plantuml")
    )
}

// Javadoc configuration
tasks.javadoc {
    options {
        encoding = "UTF-8"
        (this as StandardJavadocDocletOptions).apply {
            charSet = "UTF-8"
            docEncoding = "UTF-8"
            source = "11"
            isFailOnError = true
            addBooleanOption("html5", true)
        }
    }
}

// Gretty configuration for Jetty server
gretty {
    httpPort = jettyHttpPort.toInt()
    contextPath = jettyContextPath
    servletContainer = "jetty11"
    scanInterval = 5

    // Configure jetty.xml for AMBIGUOUS_EMPTY_SEGMENT support
    serverConfigFile = file("src/main/config/jetty.xml")
}

// Eclipse configuration
eclipse {
    project {
        name = "plantuml"
    }
}

// Task to unpack WebJars (Monaco Editor)
// Extract to classes directory so it's available at runtime and in the WAR
val unpackWebJars by tasks.registering(Copy::class) {
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.contains("monaco-editor") }
            .map { zipTree(it) }
    })
    include("**/min/vs/loader.js", "**/min/vs/**/*", "**/min-maps/vs/**/*")
    into(layout.buildDirectory.dir("classes/java/main"))
}

// Ensure WebJars are unpacked before processing resources
tasks.processResources {
    dependsOn(unpackWebJars)
}

tasks.processTestResources {
    dependsOn(unpackWebJars)
}

// Ensure WebJars are unpacked before creating the WAR
tasks.war {
    dependsOn(unpackWebJars)
}

// Versions plugin configuration
tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates") {
    rejectVersionIf {
        // Optional: reject certain version patterns
        false
    }
    outputFormatter = "plain,html"
    outputDir = layout.buildDirectory.dir("reports/versions").get().asFile.path
}

// Clean minified resources before build
// NOTE: Minified resources should be generated manually with Maven for now:
//   mvn -f pom.xml generate-resources
// Uncomment the next lines if you want to regenerate them each time
/*
val cleanMinifiedResources by tasks.registering(Delete::class) {
    delete(fileTree("src/main/webapp/min"))
}

tasks.clean {
    dependsOn(cleanMinifiedResources)
}
*/

// Task to optimize/minify web resources (CSS, JS)
val optimizeWebResources by tasks.registering {
    description = "Optimize and minify web resources (CSS, JS)"
    group = "build"

    doLast {
        if (withoutCSSJSCompress.toBoolean()) {
            println("Skipping CSS/JS compression (withoutCSSJSCompress=true)")
            return@doLast
        }

        println("Web resource optimization should be done via a separate tool")
        println("Consider using:")
        println("  - Webpack/Vite for JS bundling and minification")
        println("  - PostCSS/cssnano for CSS minification")
        println("  - Gradle plugins like com.github.eirslett:frontend-gradle-plugin")
    }
}

// Build process
// Note: Minified resources in src/main/webapp/min should be generated manually with Maven

// Note: For CSS/JS minification, generate files manually with Maven:
//   mvn -f pom.xml generate-resources
// Or integrate a frontend build tool like Webpack/Vite

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.compileTestJava {
    options.encoding = "UTF-8"
}

// Task to show dependency insight
tasks.register<DependencyInsightReportTask>("showDependencyInsight") {
    description = "Show dependency insight for a specific dependency"
}

// Wrapper configuration
tasks.wrapper {
    gradleVersion = "8.5"
    distributionType = Wrapper.DistributionType.BIN
}
