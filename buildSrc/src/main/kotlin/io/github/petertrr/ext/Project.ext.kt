package io.github.petertrr.ext

import org.gradle.api.Project

/**
 * Returns a project property as a boolean (`"true" == true`, else `false`).
 */
fun Project.booleanProperty(name: String, default: Boolean = false): Boolean {
    val property = findProperty(name) ?: return default
    val propertyStr = property as? String ?: return default
    return propertyStr.trim().lowercase() == "true"
}
