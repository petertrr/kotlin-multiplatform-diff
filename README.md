# kotlin-multiplatform-diff

![Build and test](https://github.com/petertrr/kotlin-multiplatform-diff/workflows/Build%20and%20test/badge.svg)
[![License](https://img.shields.io/github/license/petertrr/kotlin-multiplatform-diff)](https://github.com/petertrr/kotlin-multiplatform-diff/blob/main/LICENSE)
[![codecov](https://codecov.io/gh/petertrr/kotlin-multiplatform-diff/branch/main/graph/badge.svg)](https://codecov.io/gh/petertrr/kotlin-multiplatform-diff)

[![Releases](https://img.shields.io/github/v/release/petertrr/kotlin-multiplatform-diff)](https://github.com/petertrr/kotlin-multiplatform-diff/releases)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.petertrr/kotlin-multiplatform-diff)](https://mvnrepository.com/artifact/io.github.petertrr)
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/KotlinBy/awesome-kotlin)

This is a port of [java-diff-utils](https://github.com/java-diff-utils/java-diff-utils) to Kotlin with multiplatform support.  
All credit for the implementation goes to original authors.

## Features

All features from version `4.12` of the original library are present, except for:

- fuzzy patches
- unified diff, which heavily uses file read/write and therefore needs a more complicated rewrite
- diff-utils-jgit, which uses JVM-only JGit

Please refer to the original guides for more information.

## Supported Platforms

All the platforms are supported. (following the Kotlin/Native [target support guidelines](https://kotlinlang.org/docs/native-target-support.html)):
