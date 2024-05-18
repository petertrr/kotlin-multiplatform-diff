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

All features from version `4.10` of the original library are present, except for:

- unified diff, which heavily uses file read/write and therefore needs a more complicated rewrite
- diff-utils-jgit, which uses JVM-only jgit library

Please refer to the original guides for more information.

## Supported Platforms

Currently, artifacts for the following platforms are supported:

- JVM
- JS (both browser and Node.js)
- WebAssembly (JS and WASI)
- Native

The supported Native targets are (following the Kotlin/Native [target support guidelines](https://kotlinlang.org/docs/native-target-support.html)):

| Tier 1   | Tier 2   | Tier 3   |
|:---------|:---------|:---------|
| macosX64 | linuxX64 | mingwX64 |
