/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.Project
import org.gradle.api.tasks.Exec

fun Project.configureSyntheaTask() {
  tasks.register("generateSynthea", Exec::class.java) {
    val assetsDirPath = "${projectDir.path}/src/main/assets/bulk_data"
    val populationSize = providers.gradleProperty("population")
    val scriptPath = "${rootDir.path}/generate_synthea.sh"

    doFirst {
      val scriptArgs = arrayOf("sh", scriptPath, populationSize.orNull ?: "50", assetsDirPath)
      commandLine(*scriptArgs)
    }
  }
}
