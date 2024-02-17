package org.jetbrains.plugins.template.toolWindow

import MyWebSocketService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorSettings
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.plugins.template.MyBundle
import org.jetbrains.plugins.template.services.MyProjectService
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel

class MyToolWindow(toolWindow: ToolWindow, private val project: Project) :
    CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val service = toolWindow.project.service<MyProjectService>()
    private val socketService = project.service<MyWebSocketService>()

    private var snippetDocument: Document? = null

    init {
        subscribeToWebSocketEvents()
    }

    fun getContent() = JBPanel<JBPanel<*>>().apply {
        layout = BorderLayout()
        val codePanel = createSyntaxHighlightedEditorPanel()
        add(codePanel, BorderLayout.CENTER) // Add scrollPane to the center to take up most space

        val buttonPanel = JPanel() // A panel for buttons and other controls

        val label = JBLabel(MyBundle.message("randomLabel", "?"))

        buttonPanel.add(label)
        buttonPanel.add(JButton(MyBundle.message("shuffle")).apply {
            addActionListener {
                label.text = MyBundle.message("randomLabel", service.getRandomNumber())
            }
        })

        add(buttonPanel, BorderLayout.SOUTH)
    }

    private fun createSyntaxHighlightedEditorPanel(): JPanel {
        val panel = JPanel()
        val editorFactory = EditorFactory.getInstance()

        snippetDocument = editorFactory.createDocument("")

        // Create a read-only editor for the document
        val editor = editorFactory.createViewer(snippetDocument!!, project) as EditorEx
        val editorSettings: EditorSettings = editor.settings

        // Configure editor settings for better integration as a viewer
        editorSettings.apply {
            isLineMarkerAreaShown = false
            isLineNumbersShown = true
            isIndentGuidesShown = false
            isFoldingOutlineShown = false
            additionalLinesCount = 0
            additionalColumnsCount = 0
            isCaretRowShown = false
        }

        editor.highlighter = getSyntaxHighlighter()
        editor.colorsScheme = EditorColorsManager.getInstance().globalScheme

        // Ensure the editor component fits nicely in your tool window
        panel.add(editor.component, BorderLayout.CENTER)

        return panel
    }

    private fun getSyntaxHighlighter(): EditorHighlighter {
        // Set the appropriate file type for syntax highlighting
        // Create a light virtual file to associate a file type for syntax highlighting

        val currentFile = getCurrentFile()
        val fileType = if (currentFile != null) {
            FileTypeManager.getInstance().getFileTypeByFileName(currentFile.name)
        } else {
            // Fallback or default FileType if no file is open
            FileTypeManager.getInstance().getFileTypeByExtension("ts")
        }

        val virtualFile = LightVirtualFile("temp.java", fileType, "")
        return EditorHighlighterFactory.getInstance().createEditorHighlighter(project, virtualFile)
    }

    private fun getCurrentFile(): VirtualFile? {
        val fileEditorManager = FileEditorManager.getInstance(project)
        // Get the currently open file
        val currentFile = fileEditorManager.selectedFiles.firstOrNull()
        return currentFile
    }


    private fun subscribeToWebSocketEvents() {
        launch {
            socketService.codeSuggestion.collect { suggestion ->
                updateCodeSnippet(suggestion.code)
            }
        }
    }
    private fun updateCodeSnippet(newCodeSnippet: String) {
        println("Updating code: " + newCodeSnippet)
        // Ensure updates are performed on the UI thread.
        // IntelliJ Platform's invokeLater can be used to schedule UI updates from background threads or coroutines safely:
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                snippetDocument?.setText(newCodeSnippet)
            }
        }
    }

}