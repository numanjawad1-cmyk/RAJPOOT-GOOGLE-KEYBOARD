package com.example.clipboardkeyboard

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import kotlinx.coroutines.*

class KeyboardIMEService : InputMethodService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: ClipboardRepository
    private lateinit var keyboardView: KeyboardView
    private var clipboardManager: ClipboardManager? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        val db = ClipboardDatabase.getDatabase(applicationContext)
        repository = ClipboardRepository(db.clipboardDao())
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        setupClipboardListener()
    }

    private fun setupClipboardListener() {
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener {
            val text = clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString()
            if (!text.isNullOrBlank()) {
                serviceScope.launch {
                    repository.addItem(text)
                }
            }
        }
    }

    override fun onCreateInputView(): View {
        val themedContext = ContextThemeWrapper(this, R.style.Theme_ClipboardKeyboard)
        keyboardView = KeyboardView(themedContext, repository, serviceScope)
        keyboardView.onKeyPress = { key -> handleKey(key) }
        keyboardView.onPasteFromClipboard = { pasteNextFromQueue() }
        return keyboardView.rootView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardView.reset()
    }

    private fun handleKey(key: KeyAction) {
        vibrate()
        val ic = currentInputConnection ?: return
        when (key) {
            is KeyAction.Character -> ic.commitText(key.char, 1)
            is KeyAction.Delete -> {
                val selected = ic.getSelectedText(0)
                if (selected.isNullOrEmpty()) {
                    ic.deleteSurroundingText(1, 0)
                } else {
                    ic.commitText("", 1)
                }
            }
            is KeyAction.Enter -> {
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                    ?: EditorInfo.IME_ACTION_NONE
                if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(action)
                } else {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
            }
            is KeyAction.Space -> ic.commitText(" ", 1)
            is KeyAction.Paste -> pasteNextFromQueue()
        }
    }

    private fun pasteNextFromQueue() {
        serviceScope.launch {
            val item = repository.consumeNextQueueItem()
            if (item != null) {
                val ic = currentInputConnection ?: return@launch
                ic.commitText(item.text, 1)
            }
        }
    }

    private fun vibrate() {
        vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

sealed class KeyAction {
    data class Character(val char: String) : KeyAction()
    object Delete : KeyAction()
    object Enter : KeyAction()
    object Space : KeyAction()
    object Paste : KeyAction()
}
