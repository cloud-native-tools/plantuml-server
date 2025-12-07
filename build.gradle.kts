plugins {
    `java`
    `war`
    id("io.github.ntoskrnl.gradle-minify-plugin") version "1.5.0"
}

group = "org.sourceforge.plantuml"
version = "1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

val plantumlVersion = "1.2025.10"
val monacoEditorVersion = "0.36.1"
val jettyVersion = "11.0.24"
val jlatexmathVersion = "1.0.7"
val batikVersion = "1.19"
val fopVersion = "2.11"
val junitVersion = "5.9.3"
val junitSuiteVersion = "1.9.3"
val seleniumVersion = "4.10.0"
val seleniumWebdrivermanagerVersion = "5.3.3"
val commonsIoVersion = "2.14.0"


dependencies {
    implementation("net.sourceforge.plantuml:plantuml:$plantumlVersion")
    implementation("com.google.code.gson:gson:2.10.1")

    // jstl, jlatexmath & PDF support
    runtimeOnly("org.webjars.npm:monaco-editor:$monacoEditorVersion")
    runtimeOnly("org.scilab.forge:jlatexmath:$jlatexmathVersion")
    runtimeOnly("org.scilab.forge:jlatexmath-font-greek:$jlatexmathVersion")
    runtimeOnly("org.scilab.forge:jlatexmath-font-cyrillic:$jlatexmathVersion")
    runtimeOnly("org.apache.xmlgraphics:batik-svgrasterizer:$batikVersion")
    runtimeOnly("org.apache.xmlgraphics:batik-svggen:$batikVersion")
    runtimeOnly("org.apache.xmlgraphics:fop-core:$fopVersion")

    // servlet & web container pieces
    compileOnly("org.eclipse.jetty:jetty-annotations:$jettyVersion")
    compileOnly("org.eclipse.jetty:apache-jsp:$jettyVersion")

    // ELK (graph layout engine)
    implementation("org.eclipse.elk:org.eclipse.elk.core:0.9.1")
    implementation("org.eclipse.elk:org.eclipse.elk.alg.layered:0.9.1")
    implementation("org.eclipse.elk:org.eclipse.elk.alg.mrtree:0.9.1")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("org.junit.platform:junit-platform-suite-api:$junitSuiteVersion")
    testImplementation("org.seleniumhq.selenium:selenium-java:$seleniumVersion")
    testImplementation("io.github.bonigarcia:webdrivermanager:$seleniumWebdrivermanagerVersion")
    testImplementation("org.eclipse.jetty:jetty-server:$jettyVersion")
    testImplementation("commons-io:commons-io:$commonsIoVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

minify {
    js {
        source = fileTree("src/main/webapp/components") {
            include("**/*.js")
            exclude("**/*.min.js")
        } + fileTree("src/main/webapp/js") {
            include("**/*.js")
            exclude("**/*.min.js")
        }
        destinationDir = file("src/main/webapp/min")
        suffix = ".min"
    }
    css {
        source = fileTree("src/main/webapp/components") {
            include("**/*.css")
            exclude("**/*.min.css")
        } + fileTree("src/main/webapp/js") {
            include("**/*.css")
            exclude("**/*.min.css")
        }
        destinationDir = file("src/main/webapp/min")
        suffix = ".min"
    }
}

tasks.named("processResources") {
    dependsOn("minify")
}

war {
    archiveBaseName.set("plantuml")
}
