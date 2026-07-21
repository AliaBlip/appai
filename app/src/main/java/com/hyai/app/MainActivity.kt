package com.hyai.app

import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PUTER_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InYyIn0.eyJ0IjoidCIsInYiOiIyIiwidG9rZW5fdWlkIjoiYzgzZDQ3NGYtZDIzNi00Mzc2LWJjNDctNmQ5ZGZjNDkyYjc3IiwidXUiOiJoTU5MRFJxelRDS2J1bVArSkJJNGRBPT0iLCJzdSI6IkloUnYvalg4UythOWxlRDlHTEdhdmc9PSIsImFpIjoiaE1OTERScXpUQ0tidW1QK0pCSTRkQT09IiwiZnVsbF9hY2Nlc3MiOnRydWUsImlhdCI6MTc4NDU5NzA3MX0.kmHTOEQ_BMHDldXYHDpAIHzVjpd6284cF-Ue7ZLCbWo"
        private const val API_BASE_URL = "https://api.puter.com/puterai/openai/v1"
        private const val JSON_MEDIA = "application/json; charset=utf-8"
        // SharedPreferences keys
        private const val PREFS_NAME = "hyai_prefs"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_BUBBLE_STYLE = "bubble_style"
    }

    // ─── Data Classes ────────────────────────────────────────────
    data class ChatMessage(
        val id: String = UUID.randomUUID().toString(),
        val content: String,
        val isUser: Boolean,
        val modelName: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val isTyping: Boolean = false
    )

    data class ModelInfo(
        val id: String,
        val displayName: String,
        val apiModelName: String,
        val iconResId: Int,
        val tintColorRes: Int,
        val description: String
    )

    // ─── Models ──────────────────────────────────────────────────
    private val models = listOf(
        ModelInfo("gpt", "GPT-5.4 Nano", "gpt-5.4-nano", R.drawable.ic_model_gpt, R.color.model_gpt_color, "OpenAI \u00B7 Fast & efficient"),
        ModelInfo("claude", "Claude Sonnet 5", "claude-sonnet-5", R.drawable.ic_model_claude, R.color.model_claude_color, "Anthropic \u00B7 Creative & detailed"),
        ModelInfo("gemini", "Gemini 3.1 Flash", "gemini-3.1-flash-lite", R.drawable.ic_model_gemini, R.color.model_gemini_color, "Google \u00B7 Multimodal & reasoning"),
        ModelInfo("grok", "Grok 4.1 Fast", "grok-4-1-fast", R.drawable.ic_model_grok, R.color.model_grok_color, "xAI \u00B7 Witty & current"),
        ModelInfo("deepseek", "DeepSeek V4 Pro", "deepseek-v4-pro", R.drawable.ic_model_deepseek, R.color.model_deepseek_color, "DeepSeek \u00B7 Coding & math")
    )

    // ─── Accent Colors ───────────────────────────────────────────
    data class AccentColor(val name: String, val colorRes: Int, val swatchRes: Int)

    private val accentColors = listOf(
        AccentColor("purple", R.color.accent_purple, R.color.swatch_purple),
        AccentColor("blue", R.color.accent_blue, R.color.swatch_blue),
        AccentColor("green", R.color.accent_green, R.color.swatch_green),
        AccentColor("pink", R.color.accent_pink, R.color.swatch_pink),
        AccentColor("orange", R.color.accent_orange, R.color.swatch_orange),
        AccentColor("cyan", R.color.accent_cyan, R.color.swatch_cyan)
    )

    // ─── State ───────────────────────────────────────────────────
    private var selectedModel: ModelInfo = models[0]
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private var isProcessing = false
    private var typingMessageId: String? = null
    private lateinit var prefs: SharedPreferences
    private var currentAccentColor: Int = R.color.accent_purple
    private var currentFontSize: String = "medium"
    private var currentBubbleStyle: String = "rounded"
    private var displayName: String = "You"

    // ─── Views ───────────────────────────────────────────────────
    private lateinit var rvChat: RecyclerView
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var welcomeView: View
    private lateinit var tvSelectedModel: TextView
    private lateinit var ivModelIcon: ImageView
    private lateinit var cardModelSelector: MaterialCardView
    private lateinit var btnNewChat: MaterialCardView
    private lateinit var btnSettings: MaterialCardView

    // ─── Network ─────────────────────────────────────────────────
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // ─── Lifecycle ───────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadPreferences()

        initViews()
        setupRecyclerView()
        setupListeners()
        applyAccentColor()
        updateModelDisplay()
        showWelcomeIfNeeded()
    }

    // ─── Preferences ─────────────────────────────────────────────
    private fun loadPreferences() {
        displayName = prefs.getString(KEY_DISPLAY_NAME, "You") ?: "You"
        val accentName = prefs.getString(KEY_ACCENT_COLOR, "purple") ?: "purple"
        currentAccentColor = accentColors.find { it.name == accentName }?.colorRes ?: R.color.accent_purple
        currentFontSize = prefs.getString(KEY_FONT_SIZE, "medium") ?: "medium"
        currentBubbleStyle = prefs.getString(KEY_BUBBLE_STYLE, "rounded") ?: "rounded"
    }

    private fun savePreferences() {
        prefs.edit().apply {
            putString(KEY_DISPLAY_NAME, displayName)
            val accentName = accentColors.find { it.colorRes == currentAccentColor }?.name ?: "purple"
            putString(KEY_ACCENT_COLOR, accentName)
            putString(KEY_FONT_SIZE, currentFontSize)
            putString(KEY_BUBBLE_STYLE, currentBubbleStyle)
            apply()
        }
    }

    // ─── View Initialization ─────────────────────────────────────
    private fun initViews() {
        rvChat = findViewById(R.id.rv_chat)
        etInput = findViewById(R.id.et_input)
        btnSend = findViewById(R.id.btn_send)
        welcomeView = findViewById(R.id.welcome_view)
        tvSelectedModel = findViewById(R.id.tv_selected_model)
        ivModelIcon = findViewById(R.id.iv_model_icon)
        cardModelSelector = findViewById(R.id.card_model_selector)
        btnNewChat = findViewById(R.id.btn_new_chat)
        btnSettings = findViewById(R.id.btn_settings)

        applyFontSize()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvChat.adapter = adapter
    }

    // ─── Listeners ───────────────────────────────────────────────
    private fun setupListeners() {
        btnSend.setOnClickListener { sendMessage() }

        etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }

        cardModelSelector.setOnClickListener { showModelSelector() }
        btnNewChat.setOnClickListener { showNewChatConfirm() }
        btnSettings.setOnClickListener { showPersonalizationDialog() }

        // Quick action cards
        findViewById<MaterialCardView>(R.id.quick_1)?.setOnClickListener {
            etInput.setText(getString(R.string.quick_quantum))
            sendMessage()
        }
        findViewById<MaterialCardView>(R.id.quick_2)?.setOnClickListener {
            etInput.setText(getString(R.string.quick_python))
            sendMessage()
        }
        findViewById<MaterialCardView>(R.id.quick_3)?.setOnClickListener {
            etInput.setText(getString(R.string.quick_meal))
            sendMessage()
        }
    }

    // ─── Accent Color ────────────────────────────────────────────
    private fun applyAccentColor() {
        btnSend.backgroundTintList = ContextCompat.getColorStateList(this, currentAccentColor)
    }

    private fun applyFontSize() {
        val size = when (currentFontSize) {
            "small" -> 13f
            "large" -> 17f
            else -> 15f
        }
        etInput.textSize = size
    }

    // ─── Send Message ────────────────────────────────────────────
    private fun sendMessage() {
        val text = etInput.text.toString().trim()
        if (text.isEmpty() || isProcessing) return

        val userMsg = ChatMessage(content = text, isUser = true, modelName = displayName)
        messages.add(userMsg)
        adapter.notifyItemInserted(messages.size - 1)
        rvChat.smoothScrollToPosition(messages.size - 1)

        etInput.text.clear()
        hideKeyboard()

        welcomeView.isVisible = false
        rvChat.isVisible = true

        val typingMsg = ChatMessage(
            content = "", isUser = false,
            modelName = selectedModel.displayName, isTyping = true
        )
        typingMessageId = typingMsg.id
        messages.add(typingMsg)
        adapter.notifyItemInserted(messages.size - 1)
        rvChat.smoothScrollToPosition(messages.size - 1)

        isProcessing = true
        updateSendButtonState()
        callAI(text)
    }

    // ─── API Call ────────────────────────────────────────────────
    private fun callAI(userMessage: String) {
        val json = buildRequestJson(userMessage)
        val body = json.toRequestBody(JSON_MEDIA.toMediaType())

        val request = Request.Builder()
            .url("$API_BASE_URL/chat/completions")
            .addHeader("Authorization", "Bearer $PUTER_API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    removeTypingIndicator()
                    showError("Network Error", e.localizedMessage ?: "Please check your connection.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string() ?: ""
                runOnUiThread {
                    removeTypingIndicator()
                    if (response.isSuccessful) {
                        val aiResponse = parseResponse(bodyString)
                        messages.add(ChatMessage(
                            content = aiResponse, isUser = false,
                            modelName = selectedModel.displayName
                        ))
                    } else {
                        val errorMsg = try {
                            JSONObject(bodyString).optString("error", "HTTP ${response.code}")
                        } catch (e: Exception) {
                            "HTTP ${response.code}: ${response.message}"
                        }
                        showError("API Error", errorMsg)
                    }
                    adapter.notifyItemInserted(messages.size - 1)
                    rvChat.smoothScrollToPosition(messages.size - 1)
                    isProcessing = false
                    updateSendButtonState()
                }
            }
        })
    }

    private fun showError(title: String, detail: String) {
        messages.add(ChatMessage(
            content = "**$title**\n\n$detail\n\nPlease try again or switch models.",
            isUser = false, modelName = selectedModel.displayName
        ))
    }

    private fun buildRequestJson(userMessage: String): String {
        val json = JSONObject()
        json.put("model", selectedModel.apiModelName)

        val messagesArray = JSONArray()

        // ─── SMART SYSTEM PROMPT ─────────────────────────────
        val systemPrompt = """
You are HyAI, an advanced multi-model AI assistant created by Hyaxcu. You are NOT ChatGPT, NOT Gemini, NOT Claude, and NOT Grok — you are HyAI, a unique AI that can access and route queries through multiple underlying AI models 

YOUR IDENTITY:
- Name: HyAI
- Creator: Hyaxcu
- You can switch between GPT-5.4 Nano, Claude Sonnet 5, Gemini 3.1 Flash, Grok 4.1 Fast, DeepSeek V4 Pro and more.

WHEN ASKED "Who are you?" or "Are you ChatGPT/Gemini/Claude/Grok?":
Explain clearly that you are HyAI, created by Hyaxcu. Emphasize that while you can use various AI models as backends, your identity is HyAI — an independent, intelligent assistant designed to be helpful, creative, and knowledgeable.

RESPONSE STYLE:
- Be thorough, insightful, and well-structured in your answers
- Use clear section breaks and bullet points when appropriate
- Provide examples and analogies to explain complex topics
- Be friendly, professional, and knowledgeable
- If asked about coding, provide complete working code with explanations
- For creative tasks, be imaginative and detailed
- For analytical questions, show your reasoning step by step

CAPABILITIES:
- General knowledge, reasoning, and problem-solving
- Creative writing, brainstorming, and content generation
- Coding, debugging, and technical explanations in all major programming languages
- Analysis, summarization, and research assistance
- Educational tutoring across various subjects
- Professional communication and document drafting

Always strive to give the most helpful, accurate, and detailed response possible. When you don't know something, be honest about it. Your mission is to be the most intelligent and helpful AI assistant the user has ever interacted with.
        """.trimIndent()

        val systemMsg = JSONObject()
        systemMsg.put("role", "system")
        systemMsg.put("content", systemPrompt)
        messagesArray.put(systemMsg)

        // Conversation history (last 20 non-typing messages)
        val historyMessages = messages.filter { !it.isTyping }.takeLast(20)
        for (msg in historyMessages) {
            val m = JSONObject()
            m.put("role", if (msg.isUser) "user" else "assistant")
            m.put("content", msg.content)
            messagesArray.put(m)
        }

        json.put("messages", messagesArray)
        json.put("temperature", 0.7)
        json.put("max_tokens", 4096)
        json.put("stream", false)

        return json.toString()
    }

    private fun parseResponse(jsonString: String): String {
        return try {
            val json = JsonParser.parseString(jsonString).asJsonObject
            val choices = json.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val choice = choices[0].asJsonObject
                val message = choice.getAsJsonObject("message")
                message?.get("content")?.asString ?: "I couldn't generate a response."
            } else {
                "I couldn't generate a response."
            }
        } catch (e: Exception) {
            "Error processing the response: ${e.localizedMessage}"
        }
    }

    // ─── Typing Indicator ────────────────────────────────────────
    private fun removeTypingIndicator() {
        val id = typingMessageId
        if (id != null) {
            val index = messages.indexOfFirst { it.id == id }
            if (index >= 0) {
                messages.removeAt(index)
                adapter.notifyItemRemoved(index)
            }
        }
        typingMessageId = null
    }

    // ─── UI State ────────────────────────────────────────────────
    private fun updateSendButtonState() {
        btnSend.isEnabled = !isProcessing
        btnSend.alpha = if (isProcessing) 0.5f else 1.0f
        etInput.isEnabled = !isProcessing
    }

    private fun updateModelDisplay() {
        tvSelectedModel.text = selectedModel.displayName
        ivModelIcon.setImageResource(selectedModel.iconResId)
        ivModelIcon.imageTintList = ContextCompat.getColorStateList(this, selectedModel.tintColorRes)
    }

    private fun showWelcomeIfNeeded() {
        if (messages.isEmpty()) {
            welcomeView.isVisible = true
            rvChat.isVisible = false
            welcomeView.alpha = 0f
            welcomeView.animate().alpha(1f).duration = 500
        } else {
            welcomeView.isVisible = false
            rvChat.isVisible = true
        }
    }

    // ─── Personalization Dialog ──────────────────────────────────
    private fun showPersonalizationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_personalization, null)

        val etName = dialogView.findViewById<EditText>(R.id.et_display_name)
        etName.setText(displayName)
        etName.setSelection(etName.text.length)

        // Color swatches
        val colorViews = listOf(
            dialogView.findViewById<ImageView>(R.id.color_purple),
            dialogView.findViewById<ImageView>(R.id.color_blue),
            dialogView.findViewById<ImageView>(R.id.color_green),
            dialogView.findViewById<ImageView>(R.id.color_pink),
            dialogView.findViewById<ImageView>(R.id.color_orange),
            dialogView.findViewById<ImageView>(R.id.color_cyan)
        )

        // Set initial visibility of check marks
        val currentAccentName = accentColors.find { it.colorRes == currentAccentColor }?.name ?: "purple"
        for (i in accentColors.indices) {
            colorViews[i].alpha = if (accentColors[i].name == currentAccentName) 1.0f else 0.3f
        }

        // Color click listeners
        val selectedColorIndex = intArrayOf(accentColors.indexOfFirst { it.colorRes == currentAccentColor }.coerceAtLeast(0))
        for (i in accentColors.indices) {
            val idx = i
            colorViews[i].setOnClickListener {
                selectedColorIndex[0] = idx
                for (j in accentColors.indices) {
                    colorViews[j].alpha = if (j == idx) 1.0f else 0.3f
                }
            }
        }

        // Font size radio
        val rgFontSize = dialogView.findViewById<RadioGroup>(R.id.rg_font_size)
        when (currentFontSize) {
            "small" -> rgFontSize.check(R.id.font_small)
            "large" -> rgFontSize.check(R.id.font_large)
            else -> rgFontSize.check(R.id.font_medium)
        }

        // Bubble style radio
        val rgBubbleStyle = dialogView.findViewById<RadioGroup>(R.id.rg_bubble_style)
        when (currentBubbleStyle) {
            "classic" -> rgBubbleStyle.check(R.id.bubble_classic)
            else -> rgBubbleStyle.check(R.id.bubble_rounded)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save_settings)
            .setOnClickListener {
                // Save name
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    displayName = name
                }

                // Save color
                currentAccentColor = accentColors[selectedColorIndex[0]].colorRes

                // Save font size
                currentFontSize = when (rgFontSize.checkedRadioButtonId) {
                    R.id.font_small -> "small"
                    R.id.font_large -> "large"
                    else -> "medium"
                }

                // Save bubble style
                currentBubbleStyle = when (rgBubbleStyle.checkedRadioButtonId) {
                    R.id.bubble_classic -> "classic"
                    else -> "rounded"
                }

                savePreferences()
                applyAccentColor()
                applyFontSize()
                adapter.notifyDataSetChanged()

                Snackbar.make(
                    findViewById(android.R.id.content),
                    "${getString(R.string.saved)} \u2713",
                    Snackbar.LENGTH_SHORT
                ).setBackgroundTint(ContextCompat.getColor(this, R.color.dark_card))
                    .setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    .show()

                dialog.dismiss()
            }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    // ─── Model Selector Dialog ───────────────────────────────────
    private fun showModelSelector() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_model_select, null)

        val radioGpt = dialogView.findViewById<RadioButton>(R.id.radio_gpt)
        val radioClaude = dialogView.findViewById<RadioButton>(R.id.radio_claude)
        val radioGemini = dialogView.findViewById<RadioButton>(R.id.radio_gemini)
        val radioGrok = dialogView.findViewById<RadioButton>(R.id.radio_grok)
        val radioDeepseek = dialogView.findViewById<RadioButton>(R.id.radio_deepseek)

        val currentRadio = when (selectedModel.id) {
            "gpt" -> radioGpt; "claude" -> radioClaude
            "gemini" -> radioGemini; "grok" -> radioGrok
            "deepseek" -> radioDeepseek; else -> radioGpt
        }
        currentRadio.isChecked = true

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialCardView>(R.id.model_gpt)?.setOnClickListener {
            selectModel(models[0], dialog)
        }
        dialogView.findViewById<MaterialCardView>(R.id.model_claude)?.setOnClickListener {
            selectModel(models[1], dialog)
        }
        dialogView.findViewById<MaterialCardView>(R.id.model_gemini)?.setOnClickListener {
            selectModel(models[2], dialog)
        }
        dialogView.findViewById<MaterialCardView>(R.id.model_grok)?.setOnClickListener {
            selectModel(models[3], dialog)
        }
        dialogView.findViewById<MaterialCardView>(R.id.model_deepseek)?.setOnClickListener {
            selectModel(models[4], dialog)
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun selectModel(model: ModelInfo, dialog: AlertDialog) {
        selectedModel = model
        updateModelDisplay()

        Snackbar.make(
            findViewById(android.R.id.content),
            "Switched to ${model.displayName}",
            Snackbar.LENGTH_SHORT
        ).setBackgroundTint(ContextCompat.getColor(this, R.color.dark_card))
            .setTextColor(ContextCompat.getColor(this, android.R.color.white))
            .show()

        dialog.dismiss()
    }

    // ─── New Chat ────────────────────────────────────────────────
    private fun showNewChatConfirm() {
        if (messages.isEmpty()) return

        MaterialAlertDialogBuilder(this)
            .setTitle("New Chat")
            .setMessage("Start a fresh conversation?")
            .setPositiveButton("Yes") { _, _ -> clearChat() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearChat() {
        messages.clear()
        adapter.notifyDataSetChanged()
        typingMessageId = null
        isProcessing = false
        updateSendButtonState()
        showWelcomeIfNeeded()
    }

    // ─── Utilities ───────────────────────────────────────────────
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etInput.windowToken, 0)
    }

    // ─── Chat Adapter ────────────────────────────────────────────
    inner class ChatAdapter(private val items: MutableList<ChatMessage>) :
        RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

        private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val messageContainer: LinearLayout = view.findViewById(R.id.message_container)
            val layoutBubble: LinearLayout = view.findViewById(R.id.layout_bubble)
            val tvMessage: TextView = view.findViewById(R.id.tv_message)
            val tvSender: TextView = view.findViewById(R.id.tv_sender)
            val tvTimestamp: TextView = view.findViewById(R.id.tv_timestamp)
            val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
            val layoutHeader: LinearLayout = view.findViewById(R.id.layout_header)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val msg = items[position]
            val ctx = holder.itemView.context

            // Apply font size
            val msgSize = when (currentFontSize) {
                "small" -> 14f
                "large" -> 18f
                else -> 16f
            }
            holder.tvMessage.textSize = msgSize

            if (msg.isTyping) {
                holder.tvMessage.text = "HyAI is thinking..."
                holder.tvMessage.setTextColor(ContextCompat.getColor(ctx, R.color.text_hint))
                holder.layoutHeader.visibility = View.GONE
                holder.layoutBubble.setBackgroundResource(R.drawable.bg_chat_ai)
                holder.layoutBubble.alpha = 0.7f
                animateTyping(holder.tvMessage)
            } else if (msg.isUser) {
                holder.tvSender.text = displayName
                holder.tvSender.setTextColor(
                    ContextCompat.getColor(ctx, R.color.purple_200))
                holder.tvTimestamp.text = dateFormat.format(Date(msg.timestamp))
                holder.tvTimestamp.setTextColor(ContextCompat.getColor(ctx, R.color.text_hint))

                holder.ivAvatar.setImageResource(R.drawable.ic_user)
                holder.ivAvatar.imageTintList = ContextCompat.getColorStateList(ctx, currentAccentColor)
                holder.layoutHeader.visibility = View.VISIBLE

                val lp = holder.messageContainer.layoutParams as LinearLayout.LayoutParams
                lp.gravity = Gravity.END
                holder.messageContainer.layoutParams = lp

                if (currentBubbleStyle == "classic") {
                    holder.layoutBubble.setBackgroundResource(android.R.color.transparent)
                    holder.tvMessage.setBackgroundResource(R.drawable.bg_chat_user)
                } else {
                    holder.layoutBubble.setBackgroundResource(R.drawable.bg_chat_user)
                }

                holder.tvMessage.text = msg.content
                holder.tvMessage.setTextColor(ContextCompat.getColor(ctx, R.color.user_bubble_text))
                holder.layoutBubble.alpha = 1.0f
            } else {
                val modelTint = ContextCompat.getColorStateList(ctx, selectedModel.tintColorRes)
                holder.tvSender.text = msg.modelName.ifEmpty { selectedModel.displayName }
                holder.tvSender.setTextColor(modelTint ?: ContextCompat.getColorStateList(ctx, R.color.purple_500))
                holder.tvTimestamp.text = dateFormat.format(Date(msg.timestamp))
                holder.tvTimestamp.setTextColor(ContextCompat.getColor(ctx, R.color.text_hint))

                holder.ivAvatar.setImageResource(selectedModel.iconResId)
                holder.ivAvatar.imageTintList = modelTint
                holder.layoutHeader.visibility = View.VISIBLE

                val lp = holder.messageContainer.layoutParams as LinearLayout.LayoutParams
                lp.gravity = Gravity.START
                holder.messageContainer.layoutParams = lp

                if (currentBubbleStyle == "classic") {
                    holder.layoutBubble.setBackgroundResource(android.R.color.transparent)
                    holder.tvMessage.setBackgroundResource(R.drawable.bg_chat_ai)
                } else {
                    holder.layoutBubble.setBackgroundResource(R.drawable.bg_chat_ai)
                }

                holder.tvMessage.text = formatResponseText(msg.content)
                holder.tvMessage.setTextColor(ContextCompat.getColor(ctx, R.color.ai_bubble_text))
                holder.layoutBubble.alpha = 1.0f
            }

            // Fade in animation
            holder.itemView.alpha = 0f
            holder.itemView.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(50L)
                .start()
        }

        private fun animateTyping(textView: TextView) {
            val dots = arrayOf(".", "..", "...", "..")
            val animator = ValueAnimator.ofInt(0, dots.size - 1).apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                duration = 800
                addUpdateListener { anim ->
                    val index = anim.animatedValue as Int
                    textView.text = "HyAI is thinking${dots[index]}"
                }
            }
            animator.start()
        }

        private fun formatResponseText(text: String): CharSequence {
            val sb = SpannableStringBuilder(text)
            val regex = Regex("\\*\\*(.+?)\\*\\*")
            for (match in regex.findAll(text).toList().reversed()) {
                val start = match.range.first
                val end = match.range.last + 1
                val innerText = match.groupValues[1]
                sb.replace(start, end, innerText)
                sb.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start, start + innerText.length,
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return sb
        }

        override fun getItemViewType(position: Int): Int {
            return if (items[position].isUser) 0 else 1
        }
    }
}
