# HyAI - Multi-Model AI Chat Assistant

HyAI is a premium Android chat application created by **Hyaxcu**, providing access to multiple cutting-edge AI models through a single, stunning interface. Powered by the Puter AI platform, it gives you access to GPT, Claude, Gemini, Grok, DeepSeek and more.

## Features

- **Multiple AI Models**: Switch between GPT-5.4 Nano, Claude Sonnet 5, Gemini 3.1 Flash, Grok 4.1 Fast, and DeepSeek V4 Pro
- **Personalization**: Custom display name, 6 accent colors, font size control (small/medium/large), chat bubble style (rounded/classic)
- **Professional Vector Icons**: All icons are custom vector drawables, no emojis used anywhere in the UI
- **Beautiful UI**: Dark theme with Material Design 3, gradient accents, smooth animations
- **Smart AI**: Enhanced system prompt ensures the AI knows its identity as HyAI (not ChatGPT/Gemini/Claude) and provides thorough, intelligent responses
- **Chat History**: Maintains conversation context across messages
- **Quick Actions**: One-tap example prompts to get started
- **Typing Indicator**: Real-time animated feedback while AI generates responses
- **Error Handling**: Graceful network and API error messages

## Requirements

- Android 8.0+ (API 26+)
- Internet connection

## Technical Stack

- **Language**: Kotlin
- **UI**: Material Design 3 (Material You)
- **Networking**: OkHttp
- **JSON**: Gson + org.json
- **Storage**: SharedPreferences for personalization settings
- **Architecture**: Single-Activity with inner RecyclerView Adapter

## Setup & Build

1. Open the project in Android Studio or Code on the Go
2. Sync Gradle
3. Build and run on your device

The API key is already pre-configured — no additional setup needed.

## How to Use

1. Launch HyAI
2. Type your question or tap a quick action card
3. Tap the send button (or press Enter on keyboard)
4. Switch AI models anytime by tapping the model selector at the top
5. Start a new conversation using the "+" button
6. Tap the gear icon for personalization settings

## Personalization

- **Display Name**: Customize how your name appears in chat
- **Accent Color**: Choose from Purple, Blue, Green, Pink, Orange, or Cyan
- **Font Size**: Small, Medium (default), or Large
- **Bubble Style**: Rounded (default) or Classic

## API

This app uses the [Puter AI](https://puter.com) OpenAI-compatible API endpoint:
- **Endpoint**: `https://api.puter.com/puterai/openai/v1/chat/completions`
- **Authentication**: Bearer token
- **Format**: Fully OpenAI-compatible

## License

Created by Hyaxcu. Free for personal and educational use.
