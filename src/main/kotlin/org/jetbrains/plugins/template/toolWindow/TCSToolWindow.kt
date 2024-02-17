package org.jetbrains.plugins.template.toolWindow

import CodeSuggestion
import MyWebSocketService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorSettings
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.plugins.template.MyBundle
import org.jetbrains.plugins.template.services.MyProjectService
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants


class TCSToolWindow(private val toolWindow: ToolWindow) :
    CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val service = toolWindow.project.service<MyProjectService>()
    private val socketService = toolWindow.project.service<MyWebSocketService>()

    private var snippetDocument: Document? = null
    private var snippetAuthor: JBLabel? = null

    private val contentPanel = JPanel()

    init {
        subscribeToWebSocketEvents()
        setupContentPanel()
    }

    private fun setupContentPanel(){
        contentPanel.layout = BorderLayout(0, 20)
        contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));

        contentPanel.add(createSyntaxHighlightedEditorPanel(), BorderLayout.NORTH)
        contentPanel.add(createControlsPanel(toolWindow), BorderLayout.CENTER)

        val buttonPanel = JPanel()
        val label = JBLabel(MyBundle.message("randomLabel", "?"))
        buttonPanel.add(label)
        buttonPanel.add(JButton(MyBundle.message("shuffle")).apply {
            addActionListener {
                label.text = MyBundle.message("randomLabel", service.getRandomNumber())
            }
        })

        contentPanel.add(buttonPanel, BorderLayout.SOUTH)
    }

    fun getContent(): JPanel {
       return contentPanel
    }

    private fun createControlsPanel(toolWindow: ToolWindow): JPanel {
        val controlsPanel = JPanel()
        val insertButton = JButton("Insert")
        insertButton.addActionListener { e: ActionEvent? -> insertCode() }
        controlsPanel.add(insertButton)
        return controlsPanel
    }

    private fun createSyntaxHighlightedEditorPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BorderLayout(0, 10)
        val editorFactory = EditorFactory.getInstance()
        snippetDocument = editorFactory.createDocument("")

        // Create a read-only editor for the document via createViewer
        val editor = editorFactory.createEditor(snippetDocument!!, toolWindow.project) as EditorEx
        val editorSettings: EditorSettings = editor.settings

        // Configure editor settings for better integration as a viewer
        editorSettings.apply {
            isLineMarkerAreaShown = true
            isLineNumbersShown = true
            isIndentGuidesShown = true
            isFoldingOutlineShown = false
            additionalLinesCount = 1
            additionalColumnsCount = 0
            isShowIntentionBulb = true
            isCaretRowShown = false
        }

        editor.highlighter = getSyntaxHighlighter()
        editor.colorsScheme = EditorColorsManager.getInstance().globalScheme

        panel.add(editor.component, BorderLayout.NORTH)

        snippetAuthor = JBLabel("", SwingConstants.CENTER)
        panel.add(snippetAuthor, BorderLayout.SOUTH)

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
            FileTypeManager.getInstance().getFileTypeByExtension("java")
        }

        println(fileType)

        val virtualFile = LightVirtualFile("temp.ts", fileType, "")
        return EditorHighlighterFactory.getInstance().createEditorHighlighter(toolWindow.project, virtualFile)
    }

    private fun getCurrentFile(): VirtualFile? {
        val fileEditorManager = FileEditorManager.getInstance(toolWindow.project)
        // Get the currently open file
        val currentFile = fileEditorManager.selectedFiles.firstOrNull()
        return currentFile
    }

    private fun insertCode() {
        TODO("Insert not yet implemented")
    }

    private fun subscribeToWebSocketEvents() {
        launch {
            socketService.codeSuggestion.collect { suggestion ->
                renderCodeSuggestion(suggestion)
            }
        }
    }
    private fun renderCodeSuggestion(newCodeSuggestion: CodeSuggestion) {
        println("Updating code: " + newCodeSuggestion)
        // Ensure updates are performed on the UI thread.
        // IntelliJ Platform's invokeLater can be used to schedule UI updates from background threads or coroutines safely:
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(toolWindow.project) {
                snippetDocument?.setText(newCodeSuggestion.code)
                snippetAuthor?.text = MyBundle.message("suggestionBy",  newCodeSuggestion.user)
            }
        }
    }

}