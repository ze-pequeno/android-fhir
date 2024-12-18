import Dependencies.removeIncompatibleDependencies
import java.net.URL
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude

plugins {
  id(Plugins.BuildPlugins.androidLib)
  id(Plugins.BuildPlugins.kotlinAndroid)
  id(Plugins.BuildPlugins.mavenPublish)
  jacoco
  id(Plugins.BuildPlugins.dokka).version(Plugins.Versions.dokka)
}

publishArtifact(Releases.Workflow)

createJacocoTestReportTask()

android {
  namespace = "com.google.android.fhir.workflow"
  compileSdk = Sdk.COMPILE_SDK
  defaultConfig {
    minSdk = Sdk.MIN_SDK
    testInstrumentationRunner = Dependencies.androidJunitRunner
    // Need to specify this to prevent junit runner from going deep into our dependencies
    testInstrumentationRunnerArguments["package"] = "com.google.android.fhir.workflow"
  }

  sourceSets {
    getByName("androidTest").apply { resources.setSrcDirs(listOf("sampledata")) }

    getByName("test").apply { resources.setSrcDirs(listOf("sampledata")) }
  }

  // Added this for fixing out of memory issue in running test cases
  tasks.withType<Test>().configureEach {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() - 1).takeIf { it > 0 } ?: 1
    setForkEvery(100)
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
    }
  }

  packaging {
    resources.excludes.addAll(
      listOf(
        "license.html",
        "META-INF/ASL2.0",
        "META-INF/ASL-2.0.txt",
        "META-INF/DEPENDENCIES",
        "META-INF/INDEX.LIST",
        "META-INF/LGPL-3.0.txt",
        "META-INF/LICENSE",
        "META-INF/LICENSE.txt",
        "META-INF/license.txt",
        "META-INF/license.html",
        "META-INF/LICENSE.md",
        "META-INF/NOTICE",
        "META-INF/NOTICE.txt",
        "META-INF/NOTICE.md",
        "META-INF/notice.txt",
        "META-INF/LGPL-3.0.txt",
        "META-INF/sun-jaxb.episode",
        "META-INF/*.kotlin_module",
        "readme.html",
      ),
    )
  }
  configureJacocoTestOptions()
  kotlin { jvmToolchain(11) }
  compileOptions { isCoreLibraryDesugaringEnabled = true }
}

afterEvaluate { configureFirebaseTestLabForLibraries() }

configurations { all { removeIncompatibleDependencies() } }

dependencies {
  coreLibraryDesugaring(Dependencies.desugarJdkLibs)

  androidTestImplementation(Dependencies.jsonAssert)
  androidTestImplementation(Dependencies.xmlUnit)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.ext.junit.ktx)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.work.testing)
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.logback.android)
  androidTestImplementation(libs.truth)
  androidTestImplementation(project(":workflow-testing"))

  api(Dependencies.HapiFhir.structuresR4) { exclude(module = "junit") }
  api(Dependencies.HapiFhir.guavaCaching)

  implementation(Dependencies.HapiFhir.guavaCaching)
  implementation(Dependencies.timber)
  implementation(Dependencies.xerces)
  implementation(libs.android.fhir.engine) { exclude(module = "truth") }
  implementation(libs.android.fhir.knowledge)
  implementation(libs.androidx.core)
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.opencds.cqf.fhir.cr)
  implementation(libs.opencds.cqf.fhir.jackson)

  testImplementation(Dependencies.jsonAssert)
  testImplementation(Dependencies.robolectric)
  testImplementation(Dependencies.xmlUnit)
  testImplementation(libs.androidx.room.room)
  testImplementation(libs.androidx.room.runtime)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
  testImplementation(project(":workflow-testing"))
  testImplementation(project(":knowledge"))

  configurations.all {
    if (name.contains("test", ignoreCase = true)) {
      resolutionStrategy.dependencySubstitution {
        // To test the workflow library against the latest Knowledge Manager APIs, substitute the
        // dependency on the released Knowledge Manager library with the current build.
        substitute(module("com.google.android.fhir:knowledge:0.1.0-beta01"))
          .using(project(":knowledge"))
      }
    }
  }

  constraints {
    Dependencies.hapiFhirConstraints().forEach { (libName, constraints) ->
      api(libName, constraints)
      implementation(libName, constraints)
    }
  }
}

tasks.dokkaHtml.configure {
  outputDirectory.set(
    file("../docs/use/api/${Releases.Workflow.artifactId}/${Releases.Workflow.version}"),
  )
  suppressInheritedMembers.set(true)
  dokkaSourceSets {
    named("main") {
      moduleName.set(Releases.Workflow.name)
      moduleVersion.set(Releases.Workflow.version)
      includes.from("Module.md")
      sourceLink {
        localDirectory.set(file("src/main/java"))
        remoteUrl.set(
          URL("https://github.com/google/android-fhir/tree/master/workflow/src/main/java"),
        )
        remoteLineSuffix.set("#L")
      }
      externalDocumentationLink {
        url.set(URL("https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-structures-r4/"))
        packageListUrl.set(
          URL("https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-structures-r4/element-list"),
        )
      }
      externalDocumentationLink {
        url.set(URL("https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-base/"))
        packageListUrl.set(URL("https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-base/element-list"))
      }
    }
  }
}
