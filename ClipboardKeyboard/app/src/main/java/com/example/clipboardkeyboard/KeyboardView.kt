package com.example.clipboardkeyboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.example.clipboardkeyboard.databinding.KeyboardViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class KeyboardMode { QWERTY, SYMBOLS, EMOJI, CLIPBOARD }
enum class ShiftState { OFF, ON, CAPS_LOCK }

class KeyboardView(
    private val context: Context,
    private val repository: ClipboardRepository,
    private val scope: CoroutineScope
) {
    val rootView: View
    private val binding: KeyboardViewBinding

    var onKeyPress: ((KeyAction) -> Unit)? = null
    var onPasteFromClipboard: (() -> Unit)? = null

    private var currentMode = KeyboardMode.QWERTY
    private var shiftState = ShiftState.OFF

    private val qwertyRows = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("SHIFT","z","x","c","v","b","n","m","DEL"),
        listOf("123","EMOJI","SPACE","ENTER")
    )

    private val symbolRows = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0"),
        listOf("!","@","#","$","%","^","&","*","(", ")"),
        listOf("-","_","=","+","[","]","{","}","|","\\"),
        listOf("ABC","/",",",".","?","\"","'",";",":","DEL"),
        listOf("EMOJI","SPACE","ENTER")
    )

    private val emojiRows = listOf(
        listOf("😀","😂","😍","🥰","😎","🤔","😅","😭","🙄","😤"),
        listOf("👍","👎","❤️","🔥","✨","💯","🎉","🤝","💪","🙏"),
        listOf("😮","😱","🥳","🤩","😈","💀","🤣","😊","😔","😡"),
        listOf("ABC","123","SPACE","ENTER")
    )

    init {
        binding = KeyboardViewBinding.inflate(LayoutInflater.from(context))
        rootView = binding.root
        setupKeyboard()
        setupClipboardPanel()
        observeClipboardBadge()
    }

    private fun setupKeyboard() {
        renderRows(qwertyRows)

        binding.btnClipboard.setOnClickListener {
            toggleMode(KeyboardMode.CLIPBOARD)
        }
        binding.btnPaste.setOnClickListener {
            onPasteFromClipboard?.invoke()
        }
    }

    private fun renderRows(rows: List<List<String>>) {
        binding.keyboardRows.removeAllViews()
        val inflater = LayoutInflater.from(context)
        for (row in rows) {
            val rowView = inflater.inflate(R.layout.keyboard_row, binding.keyboardRows, false) as android.widget.LinearLayout
            for (key in row) {
                val btn = buildKey(key, inflater, rowView)
                rowView.addView(btn)
            }
            binding.keyboardRows.addView(rowView)
        }
    }

    private fun buildKey(key: String, inflater: LayoutInflater, parent: android.widget.LinearLayout): View {
        val btn = inflater.inflate(R.layout.key_button, parent, false) as com.google.android.material.button.MaterialButton

        val displayText = when (key) {
            "DEL" -> "⌫"
            "SHIFT" -> if (shiftState == ShiftState.CAPS_LOCK) "⇪" else "⇧"
            "ENTER" -> "↵"
            "SPACE" -> "space"
            "123" -> "?123"
            "ABC" -> "ABC"
            "EMOJI" -> "☺"
            else -> if (shiftState != ShiftState.OFF) key.uppercase() else key
        }
        btn.text = displayText

        val layoutParams = btn.layoutParams as android.widget.LinearLayout.LayoutParams
        when (key) {
            "SPACE" -> layoutParams.weight = 4f
            "SHIFT", "DEL", "ENTER" -> layoutParams.weight = 1.5f
            "ABC", "123", "EMOJI" -> layoutParams.weight = 1.2f
            else -> layoutParams.weight = 1f
        }
        btn.layoutParams = layoutParams

        when (key) {
            "DEL" -> {
                btn.setBackgroundColor(context.getColor(R.color.key_special_bg))
                btn.setOnClickListener { onKeyPress?.invoke(KeyAction.Delete) }
                btn.setOnLongClickListener {
                    repeat(5) { onKeyPress?.invoke(KeyAction.Delete) }
                    true
                }
            }
            "SHIFT" -> {
                btn.setBackgroundColor(context.getColor(R.color.key_special_bg))
                btn.setOnClickListener {
                    shiftState = when (shiftState) {
                        ShiftState.OFF -> ShiftState.ON
                        ShiftState.ON -> ShiftState.CAPS_LOCK
                        ShiftState.CAPS_LOCK -> ShiftState.OFF
                    }
                    renderRows(qwertyRows)
                }
            }
            "ENTER" -> {
                btn.setBackgroundColor(context.getColor(R.color.key_action_bg))
                btn.setTextColor(context.getColor(android.R.color.white))
                btn.setOnClickListener { onKeyPress?.invoke(KeyAction.Enter) }
            }
            "SPACE" -> {
                btn.setOnClickListener { onKeyPress?.invoke(KeyAction.Space) }
            }
            "123" -> {
                btn.setBackgroundColor(context.getColor(R.color.key_special_bg))
                btn.setOnClickListener { switchMode(KeyboardMode.SYMBOLS) }
            }
            "ABC" -> {
                btn.setBackgroundColor(context.getColor(R.color.key_special_bg))
                btn.setOnClickListener { switchMode(KeyboardMode.QWERTY) }
            }
            "EMOJI" -> {
                btn.setBackgroundColor(context.getColor(R.color.key_special_bg))
                btn.setOnClickListener { switchMode(KeyboardMode.EMOJI) }
            }
            else -> {
                btn.setOnClickListener {
                    val text = if (shiftState != ShiftState.OFF) key.uppercase() else key
                    onKeyPress?.invoke(KeyAction.Character(text))
                    if (shiftState == ShiftState.ON) {
                        shiftState = ShiftState.OFF
                        renderRows(qwertyRows)
                    }
                }
            }
        }
        return btn
    }

    private fun switchMode(mode: KeyboardMode) {
        currentMode = mode
        when (mode) {
            KeyboardMode.QWERTY -> {
                binding.clipboardPanel.visibility = View.GONE
                binding.keyboardRows.visibility = View.VISIBLE
                renderRows(qwertyRows)
            }
            KeyboardMode.SYMBOLS -> {
                binding.clipboardPanel.visibility = View.GONE
                binding.keyboardRows.visibility = View.VISIBLE
                renderRows(symbolRows)
            }
            KeyboardMode.EMOJI -> {
                binding.clipboardPanel.visibility = View.GONE
                binding.keyboardRows.visibility = View.VISIBLE
                renderRows(emojiRows)
            }
            KeyboardMode.CLIPBOARD -> {
                binding.keyboardRows.visibility = View.GONE
                binding.clipboardPanel.visibility = View.VISIBLE
            }
        }
    }

    private fun toggleMode(mode: KeyboardMode) {
        if (currentMode == mode) {
            switchMode(KeyboardMode.QWERTY)
        } else {
            switchMode(mode)
        }
    }

    private fun setupClipboardPanel() {
        val panel = ClipboardPanelView(context, repository, scope)
        panel.onItemPaste = { text ->
            onKeyPress?.invoke(KeyAction.Character(text))
            switchMode(KeyboardMode.QWERTY)
        }
        binding.clipboardPanel.addView(panel.rootView)
    }

    private fun observeClipboardBadge() {
        scope.launch {
            repository.getAllItems().collectLatest { items ->
                val count = items.filter { !it.isPinned }.size
                binding.clipboardBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
                binding.clipboardBadge.text = if (count > 99) "99+" else count.toString()
            }
        }
    }

    fun reset() {
        switchMode(KeyboardMode.QWERTY)
    }
}
