# kotlin-multiplatform-diff

![Build and test](https://github.com/petertrr/kotlin-multiplatform-diff/workflows/Build%20and%20test/badge.svg)
[![Codecov](https://codecov.io/gh/petertrr/kotlin-multiplatform-diff/branch/main/graph/badge.svg)](https://codecov.io/gh/petertrr/kotlin-multiplatform-diff)
[![License](https://img.shields.io/github/license/petertrr/kotlin-multiplatform-diff)](https://github.com/petertrr/kotlin-multiplatform-diff/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.petertrr/kotlin-multiplatform-diff)](https://mvnrepository.com/artifact/io.github.petertrr)
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/KotlinBy/awesome-kotlin)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.24-blue.svg?logo=kotlin)](http://kotlinlang.org)

This is a port of [java-diff-utils](https://github.com/java-diff-utils/java-diff-utils) to Kotlin with multiplatform support.  
All credit for the implementation goes to the original authors.

## Features

All features from version `4.15` of the original library are present, except for:

- fuzzy patches
- unified diff, which heavily uses file read/write and therefore needs a more complicated rewrite
- diff-utils-jgit, which uses JVM-only JGit

Refer to the [original wiki][1] for more information.

## Supported platforms

- JVM
- JS (browser and Node.js)
- WebAssembly (WASM/JS and WASM/WASI)
- Native

Supported Native targets are (following the Kotlin/Native [target support guidelines][2]):

| Tier 1     | Tier 2     | Tier 3   |
|:-----------|:-----------|:---------|
| macosX64   | linuxX64   | mingwX64 |
| macosArm64 | linuxArm64 |          |

[1]: https://github.com/java-diff-utils/java-diff-utils/wiki
[2]: https://kotlinlang.org/docs/native-target-support.html
