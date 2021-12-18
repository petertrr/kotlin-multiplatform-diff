import io.github.petertrr.configurePublishing
import io.github.petertrr.configureVersioning

plugins {
    jacoco
}

configureVersioning()
group = "io.github.petertrr"
description = "A multiplatform Kotlin library for calculating text differences"

configurePublishing()
