import java.io.File
import java.security.MessageDigest

fun main(args: Array<String>) {

    //val args = arrayOf<String>("commit", "first commit2")
    configureStartingFiles()

    val command = try {
        args.first()
    } catch (ex: Exception) {
        "--help"
    }

    when (command) {
        "config" -> {
            executeConfigModule(args)
        }

        "--help" -> {
            executeHelpModule()
        }

        "add" -> {
            executeAddModule(args)
        }

        "log" -> {
            executeLogModule(args)
        }

        "commit" -> {
            executeCommitModule(args)
        }

        "checkout" -> {
            executeCheckOutModule(args)
        }

        else -> {
            executeWrongCommandModule(command)
        }
    }
}

fun configureStartingFiles() {
    val vcsFolder = File(VCS_FOLDER_NAME)
    if (!vcsFolder.exists())
        vcsFolder.mkdir()

    val configFile = vcsFolder.resolve(CONFIG_FILE_NAME)
    if (!configFile.exists())
        configFile.createNewFile()

    val indexFile = vcsFolder.resolve(INDEX_FILE_NAME)
    if (!indexFile.exists())
        indexFile.createNewFile()

    val logFile = vcsFolder.resolve(LOG_FILE_NAME)
    if (!logFile.exists())
        logFile.createNewFile()

    val commitFolder = vcsFolder.resolve(COMMITS_FOLDER_NAME)
    if (!commitFolder.exists())
        commitFolder.mkdir()
}

private fun executeHelpModule() {
    println(
        "These are SVCS commands:\n" +
                "config     Get and set a username.\n" +
                "add        Add a file to the index.\n" +
                "log        Show commit logs.\n" +
                "commit     Save changes.\n" +
                "checkout   Restore a file."
    )
}

private fun executeConfigModule(args: Array<String>) {
    val configFile = File("$VCS_FOLDER_NAME${File.separator}$CONFIG_FILE_NAME")
    val configContent = configFile.readText()
    when {
        args.size == 1 && configContent.isEmpty() -> println("Please, tell me who you are.")
        args.size == 1 && configContent.isNotEmpty() -> println("The username is $configContent.")
        else -> {
            configFile.writeText(args.copyOfRange(1, args.size).joinToString(" "))
            val configContentAfterUpdate = configFile.readText()
            println("The username is $configContentAfterUpdate.")
        }
    }
}

private fun executeAddModule(args: Array<String>) {

    val indexFile = File("$VCS_FOLDER_NAME${File.separator}$INDEX_FILE_NAME")
    val indexFileContent = indexFile.readLines()
    when {
        args.size == 1 && indexFileContent.isEmpty() -> println("Add a file to the index.")
        args.size == 1 -> {
            println("Tracked files:")
            indexFileContent.forEach {
                println(it)
            }
        }

        args.size == 2 -> {
            val trackingFile = File(args[1])
            if (!trackingFile.exists()) {
                println("Can't find '${args[1]}'.")
            } else {
                indexFile.appendText("${args[1]}\n")
                println("The file '${args[1]}' is tracked.")
            }
        }
    }


}

private fun executeLogModule(args: Array<String>) {

    val logFile = File("$VCS_FOLDER_NAME${File.separator}$LOG_FILE_NAME")
    val logFileContent = logFile.readText()

    when {
        logFileContent.isEmpty() -> println("No commits yet.\n")
        else -> {
            println(logFileContent)
        }
    }
}

private fun executeCommitModule(args: Array<String>) {

    when (args.size) {
        1 ->
            println("Message was not passed.")

        2 -> {
            createAndConfigureCommitFile(args[1])
        }
    }

}

fun createAndConfigureCommitFile(commitMessage: String) {
    val commitHash = createHashFrom(commitMessage)
    val indexFile = File("$VCS_FOLDER_NAME${File.separator}$INDEX_FILE_NAME")
    val indexFileContent = indexFile.readLines()
    val logFile = File("$VCS_FOLDER_NAME${File.separator}$LOG_FILE_NAME")
    val configContent = File("$VCS_FOLDER_NAME${File.separator}$CONFIG_FILE_NAME").readText()
    val commitFile =
        File("$VCS_FOLDER_NAME${File.separator}$COMMITS_FOLDER_NAME${File.separator}$commitHash")
    val logFileContent = logFile.readLines()


    when {
        indexFileContent.isEmpty() -> {
            println("Nothing to commit.")
            return
        }

        logFileContent.isEmpty() -> {
            backUpIndexedFiles(
                commitFile,
                indexFileContent,
                logFile,
                commitHash,
                configContent,
                commitMessage
            )
            println("Changes are committed.")
            return
        }

        validateIfIsAbleToCommit(logFileContent, indexFileContent) -> {
            backUpIndexedFiles(
                commitFile,
                indexFileContent,
                logFile,
                commitHash,
                configContent,
                commitMessage
            )
            println("Changes are committed.")
            return
        }

        else -> {
            println("Nothing to commit.")
            return
        }
    }
}


fun backUpIndexedFiles(
    commitFile: File,
    indexFileContent: List<String>,
    logFile: File,
    commitHash: String,
    configContent: String,
    commitMessage: String
) {
    commitFile.mkdir()
    indexFileContent.forEach { indexedFile ->
        val currentFile = File(indexedFile)
        currentFile.copyTo(target = commitFile.resolve(currentFile.name), overwrite = true)
    }

    val outdatedLogContent = logFile.readText()

    val updatedLogContent = "commit $commitHash\n" +
            "Author: $configContent\n" +
            commitMessage + "\n" + "\n" + outdatedLogContent

    logFile.writeText(updatedLogContent)
}

fun validateIfIsAbleToCommit(logContent: List<String>, indexFileContent: List<String>): Boolean {
    if (logContent.isEmpty()) return true

    val lastCommitId = logContent.last {
        it.startsWith("commit")
    }.substringAfter("commit ")

    val repositoryFolder = File("$VCS_FOLDER_NAME${File.separator}$COMMITS_FOLDER_NAME")
    val lastCommittedFile = repositoryFolder.resolve(lastCommitId)

    val differences = mutableListOf<Boolean>()

    indexFileContent.forEach { trackedFile ->
        val currentTrackedFile = File(trackedFile)
        val fileInBackUp = lastCommittedFile.resolve(trackedFile)

        if (!fileInBackUp.exists()) {
            return true
        }

        differences.add(currentTrackedFile.readText() == fileInBackUp.readText())
    }

    if (differences.contains(false)) return true

    return false
}

fun createHashFrom(commitMessage: String): String {
    val result = MessageDigest.getInstance("SHA-256").digest(commitMessage.toByteArray())
    return result.fold("") { str, it -> str + "%02x".format(it) }
}

private fun executeCheckOutModule(args: Array<String>) {

    when (args.size) {
        1 -> println("Commit id was not passed.")
        2 -> {
            if (doesCommitExist(args[1])) {
                checkOut(args[1])
                println("Switched to commit ${args[1]}.")
            } else {
                println("Commit does not exist.")
            }
        }
    }
}

fun checkOut(commitId: String) {
    val commitFile =
        File("$VCS_FOLDER_NAME${File.separator}$COMMITS_FOLDER_NAME${File.separator}$commitId")
    commitFile.setReadable(true)
    val commitFilesTree = commitFile.listFiles()
    commitFilesTree?.forEach { currentFile ->
        val destination = File(currentFile.name)
        val currentFileContent = currentFile.readText()
        destination.writeText(
            currentFileContent
        )
    }
}

fun doesCommitExist(commitId: String): Boolean {
    File("$VCS_FOLDER_NAME${File.separator}$LOG_FILE_NAME").readLines().filter {
        it.startsWith("commit")
    }.forEach {
        if (it.contains(commitId)) return true
    }
    return false
}

private fun executeWrongCommandModule(command: String) {
    println("'${command}' is not a SVCS command.")
}

private const val VCS_FOLDER_NAME = "vcs"
private const val COMMITS_FOLDER_NAME = "commits"
private const val CONFIG_FILE_NAME = "config.txt"
private const val INDEX_FILE_NAME = "index.txt"
private const val LOG_FILE_NAME = "log.txt"