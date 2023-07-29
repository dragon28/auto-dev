package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.actions.EnterAction
import com.intellij.openapi.editor.actions.IncrementalFindAction
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.util.EventDispatcher
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke


enum class AutoDevInputTrigger {
    Button,
    Key
}

interface AutoDevInputListener : EventListener {
    fun editorAdded(editor: EditorEx) {}
    fun onSubmit(component: AutoDevInputField, trigger: AutoDevInputTrigger) {}
}

class AutoDevInputField(
    project: Project,
    private val listeners: List<DocumentListener>,
) : EditorTextField(project, FileTypes.PLAIN_TEXT), Disposable {
    private val editorListeners: EventDispatcher<AutoDevInputListener> =
        EventDispatcher.create(AutoDevInputListener::class.java)

    init {
        isOneLineMode = false
        updatePlaceholderText()
        setFontInheritedFromLAF(true)
        addSettingsProvider {
            it.putUserData(IncrementalFindAction.SEARCH_DISABLED, true)
            it.colorsScheme.lineSpacing = 1.0f
            it.settings.isUseSoftWraps = true
            it.settings.isPaintSoftWraps = false
            it.isEmbeddedIntoDialogWrapper = true
            it.contentComponent.setOpaque(false)
        }

        DumbAwareAction.create {
            object : AnAction() {
                override fun actionPerformed(e1: AnActionEvent) {
                    val editor = this@AutoDevInputField.editor ?: return

                    CommandProcessor.getInstance().executeCommand(project, {
                        val eol = "\n"
                        val caretOffset = editor.caretModel.offset
                        editor.document.insertString(caretOffset, eol)
                        editor.caretModel.moveToOffset(caretOffset + eol.length)
                        EditorModificationUtil.scrollToCaret(editor)
                    }, null, null)
                }
            }
        }.registerCustomShortcutSet(
            CustomShortcutSet(
                KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), null),
                KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.META_DOWN_MASK), null)
            ), this
        )


        val connect: MessageBusConnection = project.messageBus.connect(this)
        val topic = AnActionListener.TOPIC

        connect.subscribe(topic, object : AnActionListener {
            override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
                if (event.dataContext.getData(CommonDataKeys.EDITOR) === this@AutoDevInputField.editor && action is EnterAction) {
                    editorListeners.multicaster.onSubmit(this@AutoDevInputField, AutoDevInputTrigger.Key)
                }
            }
        })

        listeners.forEach { listener ->
            document.addDocumentListener(listener)
        }
    }

    override fun onEditorAdded(editor: Editor) {
        editorListeners.multicaster.editorAdded((editor as EditorEx))
    }

    private fun updatePlaceholderText() {
        setPlaceholder(AutoDevBundle.message("chat.label.initial.text"))
        repaint()
    }

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.setVerticalScrollbarVisible(true)
        setBorder(JBUI.Borders.empty())
        editor.setShowPlaceholderWhenFocused(true)
        editor.caretModel.moveToOffset(0)
        editor.scrollPane.setBorder(border)
        editor.contentComponent.setOpaque(false)
        return editor
    }

    override fun getBackground(): Color {
        val editor = editor
        if (editor != null) {
            val colorsScheme = editor.colorsScheme
            return colorsScheme.defaultBackground
        }
        return super.getBackground()
    }

    override fun dispose() {
        listeners.forEach { editor?.document?.removeDocumentListener(it) }
    }

    fun addListener(listener: AutoDevInputListener) {
        editorListeners.addListener(listener)
    }
}