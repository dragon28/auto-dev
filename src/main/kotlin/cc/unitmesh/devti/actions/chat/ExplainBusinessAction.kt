package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.getElementToAction
import cc.unitmesh.devti.gui.chat.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator

class ExplainBusinessAction : ChatBaseAction() {
    override fun getActionType(): ChatActionType {
        return ChatActionType.EXPLAIN_BUSINESS
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val caretModel = event.getData(CommonDataKeys.EDITOR)?.caretModel
        val file = event.getData(CommonDataKeys.PSI_FILE)

        val actionType = getActionType()
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val elementToChat = getElementToAction(project, editor) ?: return
        val offset = caretModel?.offset ?: 0
        val elementText = elementToChat.text

        sendToChatWindow(project, actionType) { panel, service ->
            val prompter = ContextPrompter.prompter(file?.language?.displayName ?: "")
            prompter.initContext(actionType, elementText, file, project, offset, elementToChat)

            service.handlePromptAndResponse(panel, prompter)
        }
    }
}
