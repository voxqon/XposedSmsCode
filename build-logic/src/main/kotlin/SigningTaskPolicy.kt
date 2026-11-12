internal fun requiresReleaseSigning(
    requestedTasks: List<String>,
    projectPath: String,
): Boolean = requestedTasks.any { requestedTask ->
    targetsProject(requestedTask, projectPath) && createsInstallableArtifact(requestedTask)
}

internal fun requestsOnlyApplicationBundles(
    requestedTasks: List<String>,
    projectPath: String,
): Boolean {
    val projectTasks = requestedTasks.filter { requestedTask ->
        targetsProject(requestedTask, projectPath)
    }
    return projectTasks.isNotEmpty() && projectTasks.all { requestedTask ->
        requestedTask.substringAfterLast(':').startsWith("bundle", ignoreCase = true)
    }
}

internal fun requestsApplicationBundle(
    requestedTasks: List<String>,
    projectPath: String,
): Boolean = requestedTasks.any { requestedTask ->
    targetsProject(requestedTask, projectPath) &&
        requestedTask.substringAfterLast(':').startsWith("bundle", ignoreCase = true)
}

private fun targetsProject(requestedTask: String, projectPath: String): Boolean {
    val separatorIndex = requestedTask.lastIndexOf(':')
    if (separatorIndex < 0) {
        return true
    }
    val rawProjectPath = requestedTask.substring(0, separatorIndex)
    val requestedProjectPath = when {
        rawProjectPath.isEmpty() -> ":"
        rawProjectPath.startsWith(':') -> rawProjectPath
        else -> ":$rawProjectPath"
    }
    return requestedProjectPath == projectPath
}

private fun createsInstallableArtifact(requestedTask: String): Boolean {
    val taskName = requestedTask.substringAfterLast(':').lowercase()
    if (taskName.contains("debug")) return false
    return taskName == "build" ||
        taskName.startsWith("assemble") ||
        taskName.startsWith("bundle") ||
        taskName.startsWith("install") ||
        (
            taskName.startsWith("package") &&
                !taskName.contains("resources") &&
                !taskName.contains("unittest") &&
                !taskName.contains("androidtest")
            )
}
