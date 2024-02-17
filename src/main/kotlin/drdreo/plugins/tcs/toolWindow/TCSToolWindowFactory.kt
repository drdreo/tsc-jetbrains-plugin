package drdreo.plugins.tcs.toolWindow

import com.intellij.openapi.diagnostic.LogLevel
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory


class TCSToolWindowFactory : ToolWindowFactory {

    init {
        thisLogger().setLevel(LogLevel.DEBUG)
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tcsToolWindow = TCSToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(tcsToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

}
