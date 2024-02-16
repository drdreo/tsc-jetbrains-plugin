package org.jetbrains.plugins.template.toolWindow

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
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import org.jetbrains.plugins.template.MyBundle
import org.jetbrains.plugins.template.services.MyProjectService
import org.jetbrains.plugins.template.services.MyWebSocketClient
import java.awt.BorderLayout
import java.net.URI
import java.net.URISyntaxException
import javax.swing.JButton
import javax.swing.JPanel


class MyToolWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow, project)
        myToolWindow.initWebSocketConnection()
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    fun updateToolWindowContent(message: String?) {
        // Update your tool window content here
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow, private val project: Project) {
        private val service = toolWindow.project.service<MyProjectService>()
        private lateinit var socketClient: MyWebSocketClient

        private var snippetDocument: Document? = null

        fun initWebSocketConnection() {
            try {
                val serverUri = URI("ws://localhost:8088/ws")
                socketClient = MyWebSocketClient(serverUri) { message ->
                    displayCodeSnippet(message)
                }
                        .apply {
                    connect()
                }
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
        }

        fun closeWebSocketConnection() {
            if (::socketClient.isInitialized) {
                socketClient.close()
            }
        }

        // Method called when the tool window is shown or needs to establish a connection
        fun onToolWindowShown() {
            initWebSocketConnection()
        }

        // Method called when the tool window is hidden or closed
        fun onToolWindowHidden() {
            closeWebSocketConnection()
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

        fun displayCodeSnippet(newCodeSnippet: String) {

            // Ensure updates are made on the Event Dispatch Thread
            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project) {
                    snippetDocument?.setText("let test: number =123;")
                }
            }
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

    }
}
