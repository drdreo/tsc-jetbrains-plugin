package drdreo.plugins.tcs.services

import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import drdreo.plugins.tcs.MyBundle
import javax.swing.JComponent


@Service(Service.Level.PROJECT)
class MyProjectService(private val project: Project) {

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    fun insertCode(code: String) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return

        ApplicationManager.getApplication().runWriteAction {
            WriteCommandAction.runWriteCommandAction(project) {
                val document = editor.document
                val caretModel = editor.caretModel
                val offset = caretModel.offset

                document.insertString(offset, code)

                // Optionally, move the caret to the end of the inserted text
                caretModel.moveToOffset(offset + code.length)
            }
        }
    }
}
