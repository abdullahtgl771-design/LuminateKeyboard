package com.abdullah.keyboard

import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout

class ImeService : InputMethodService(), KeyboardView.Listener, PanelHost {

    companion object {
        @Volatile
        var instance: ImeService? = null
    }

    private var root: LinearLayout? = null
    private var toolbar: ToolbarView? = null
    private var content: FrameLayout? = null
    private var kbd: KeyboardView? = null
    private var panelView: View? = null
    private var current: Panel? = null
    private var theme: Theme? = null

    private val clips = ArrayList<String>()
    private var clipMgr: ClipboardManager? = null
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { grabClip() }
    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key -> onPrefChanged(key) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        clipMgr = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        clipMgr?.addPrimaryClipChangedListener(clipListener)
        Prefs.sp(this).registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onDestroy() {
        clipMgr?.removePrimaryClipChangedListener(clipListener)
        Prefs.sp(this).unregisterOnSharedPreferenceChangeListener(prefListener)
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.VERTICAL
        r.clipChildren = false
        r.clipToPadding = false

        val tb = ToolbarView(this) { onTool(it) }
        val fl = FrameLayout(this)
        fl.clipChildren = false
        fl.clipToPadding = false
        val kv = KeyboardView(this, this)
        fl.addView(kv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        r.addView(tb, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        r.addView(fl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root = r
        toolbar = tb
        content = fl
        kbd = kv
        panelView = null
        current = null

        kv.setHeightScale(Prefs.scale(this))
        applyTheme()
        applyOneHand()
        return r
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        closePanel()
        applyTheme()
        applyOneHand()
        kbd?.setHeightScale(Prefs.scale(this))
        kbd?.reset()
        val act = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val noAct = info != null && (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        val plain = noAct || act == EditorInfo.IME_ACTION_NONE || act == EditorInfo.IME_ACTION_UNSPECIFIED
        kbd?.setEnterLabel(if (plain) "\u21B5" else "\u2192")
        refreshShift()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyTheme()
    }

    // ---------------- theme / prefs ----------------
    private fun applyTheme() {
        val t = Themes.resolve(this)
        theme = t
        root?.background =
            if (t.glass) GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(t.bgTop, t.bgBottom))
            else ColorDrawable(t.bgBottom)
        kbd?.applyTheme(t)
        toolbar?.theme = t
        applyWindowBlur(t)
    }

    private fun applyWindowBlur(t: Theme) {
        try {
            val w = window?.window ?: return
            if (t.glass) {
                w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                if (Build.VERSION.SDK_INT >= 31) w.setBackgroundBlurRadius(60)
            } else {
                if (Build.VERSION.SDK_INT >= 31) w.setBackgroundBlurRadius(0)
                w.setBackgroundDrawable(ColorDrawable(t.bgBottom))
            }
        } catch (_: Throwable) {
        }
    }

    private fun applyOneHand() {
        val gutter = (resources.displayMetrics.widthPixels * 0.22f).toInt()
        when (Prefs.oneHand(this)) {
            "right" -> root?.setPadding(gutter, 0, 0, 0)
            "left" -> root?.setPadding(0, 0, gutter, 0)
            else -> root?.setPadding(0, 0, 0, 0)
        }
    }

    private fun onPrefChanged(key: String?) {
        when (key) {
            Prefs.K_THEME, Prefs.K_GLASS -> {
                applyTheme()
                current?.let { showPanel(it) }
            }
            Prefs.K_ONEHAND -> {
                applyOneHand()
                current?.let { showPanel(it) }
            }
            Prefs.K_SCALE -> kbd?.setHeightScale(Prefs.scale(this))
        }
    }

    // ---------------- panels ----------------
    private fun showPanel(p: Panel) {
        val c = content ?: return
        val t = theme ?: Themes.resolve(this)
        panelView?.let { c.removeView(it) }
        val v = Panels.build(p, this, t, this)
        c.addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        panelView = v
        current = p
        kbd?.visibility = View.INVISIBLE
        toolbar?.closeMode = true
    }

    override fun closePanel() {
        panelView?.let { content?.removeView(it) }
        panelView = null
        current = null
        kbd?.visibility = View.VISIBLE
        toolbar?.closeMode = false
    }

    override fun openPanel(p: Panel) = showPanel(p)

    private fun onTool(t: ToolbarView.Tool) {
        when (t) {
            ToolbarView.Tool.TOOLBOX -> if (current != null) closePanel() else showPanel(Panel.TOOLBOX)
            ToolbarView.Tool.EMOJI -> showPanel(Panel.EMOJI)
            ToolbarView.Tool.CLIPBOARD -> showPanel(Panel.CLIPBOARD)
            ToolbarView.Tool.THEME -> showPanel(Panel.THEME)
            ToolbarView.Tool.SETTINGS -> {
                startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            ToolbarView.Tool.TRANSLATE -> {
                try {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://translate.google.com"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Throwable) {
                }
            }
            ToolbarView.Tool.MIC -> {
                startActivity(Intent(this, VoiceActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    // ---------------- PanelHost ----------------
    override fun commit(text: String) {
        currentInputConnection?.commitText(text, 1)
        refreshShift()
    }

    override fun sendKey(code: Int) = sendDownUpKeyEvents(code)

    override fun contextAction(id: Int) {
        currentInputConnection?.performContextMenuAction(id)
    }

    override fun clipItems(): List<String> = clips

    override fun clearClips() {
        clips.clear()
    }

    override fun toggleOneHand() {
        val next = when (Prefs.oneHand(this)) {
            "off" -> "right"
            "right" -> "left"
            else -> "off"
        }
        Prefs.setOneHand(this, next)
    }

    override fun setScale(f: Float) = Prefs.setScale(this, f)

    override fun share() {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, "Try " + getString(R.string.app_name) + "!")
        startActivity(Intent.createChooser(i, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun commitFromVoice(text: String) {
        Handler(Looper.getMainLooper()).postDelayed({
            currentInputConnection?.commitText(text, 1)
            refreshShift()
        }, 350)
    }

    private fun grabClip() {
        try {
            val cd = clipMgr?.primaryClip ?: return
            if (cd.itemCount == 0) return
            val extras = cd.description?.extras
            if (extras?.getBoolean("android.content.extra.IS_SENSITIVE", false) == true) return
            val s = cd.getItemAt(0).coerceToText(this)?.toString()?.trim() ?: return
            if (s.isEmpty()) return
            clips.remove(s)
            clips.add(0, s)
            while (clips.size > 30) clips.removeAt(clips.size - 1)
        } catch (_: Throwable) {
        }
    }

    // ---------------- KeyboardView.Listener ----------------
    override fun onText(text: String) {
        currentInputConnection?.commitText(text, 1)
        refreshShift()
    }

    override fun onDelete() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        refreshShift()
    }

    override fun onEnter() {
        val ei = currentInputEditorInfo
        val action = (ei?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val noEnterAction = ei != null && (ei.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (noEnterAction ||
            action == EditorInfo.IME_ACTION_NONE ||
            action == EditorInfo.IME_ACTION_UNSPECIFIED
        ) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        } else {
            currentInputConnection?.performEditorAction(action)
        }
        refreshShift()
    }

    override fun onGlobe() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    private fun refreshShift() {
        val ei = currentInputEditorInfo ?: return
        val caps = currentInputConnection?.getCursorCapsMode(ei.inputType) ?: 0
        kbd?.setAutoShift(caps != 0)
    }
}
