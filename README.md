# MineBucks 🟩💰

**The Ultimate Revenue Tracker for Minecraft Mod Creators.**

MineBucks is a sophisticated Android utility designed to help modders (specifically on Modrinth) track their revenue, download stats, and project health in real-time. Built with a "Cyberpunk Minecraft" aesthetic, it combines powerful background automation with battery-efficient monitoring.

---

## 🎯 The Problem

Mod creators often struggle to keep track of their earnings across multiple platforms. Constant refreshing of webpages is inefficient and impossible to do passively. MineBucks solves this by:

1. **Automating Data Fetching**: Checking Modrinth APIs securely in the background.
2. **Visualizing Growth**: Turning raw numbers into beautiful interactive graphs.
3. **Passive Monitoring**: Providing widgets and smart notifications so you never miss a payout.

---

## ✨ Key Features

### 1. 🤖 Background Automation (WorkManager)

- **Smart Scheduling**: We utilize Android's `WorkManager` to schedule reliable data fetches every 6 hours.
- **Battery Efficiency**: Zero battery drain. The app heavily leverages Android's "Doze" mode optimizations. It *only* wakes up when the OS permits and requires **network connectivity** to run, ensuring no wasted cycles offline.

### 2. 📊 Interactive Data Visualization

- **Revenue History**: Uses the **Vico** library to render standard deviation-smoothed graphs of your daily income.
- **Daily Breakdown**: A detailed list of every cent earned per day, stored locally in a SQL database.

### 3. 🕸️ Built-in Browser & Scraping

- To handle complex authentication flows (Modrinth Login), we integrated a secure, sandboxed **WebView Browser**.
- It intercepts auth tokens automatically, so you just log in once and the app handles the handshake.

### 4. 🔋 Zero-Drain Widget

- A home screen widget that displays your Total Revenue.
- **Tech**: It updates *passively*. It does not run its own service. It waits for the main background worker (every 6h) to broadcast an update, meaning having the widget costs you **$0.00** in battery life.

### 5. 🔔 Smart Notifications

- Get a "Ka-ching!" style notification only when your revenue **increases**.
- Icon: Custom Pixel-Art Dollar Sign (Monochrome).

### 6. 💸 Support Us (AdMob)

- Integrated **Rewarded Video Ads** to support the development.
- Voluntary model: You choose to watch an ad to support us. No forced interstitials.

---

## 🛠️ Tech Stack & Decisions

### Architecture: MVVM (Model-View-ViewModel)

We chose MVVM for clean separation of concerns.

- **UI**: Jetpack Compose (Modern, Reactive UI).
- **Logic**: Kotlin Coroutines & Flow.
- **Data**: Room Database (SQLite).

### Local Database (Room)

- We store your revenue history **locally** on your device.
- **Why?** Privacy. Your financial data never leaves your phone. It is cached so you can view graphs offline.

### API Integration (Modrinth)

- **Scope**: We verify user identity via the Modrinth User API.
- **Scopes Used**: `projects:read`, `payouts:read`.
- **Security**: Tokens are encrypted using Android `EncryptedSharedPreferences`.

---

## 🚀 How to Build & Run

1. **Clone the Repo**:

    ```bash
    git clone https://github.com/Sourish25/MineBucks.git
    ```

2. **Open in Android Studio**.
3. **Sync Gradle**: Ensure you have JDK 17+.
4. **Run**: Select `app` and click Run.

### AdMob Setup

The app comes with `app-ads.txt` support hosted via GitHub Pages.

- **App ID**: Configured in `AndroidManifest.xml`.
- **Ad Name**: Rewarded Video (in `AdManager.kt`).

---

## 📸 Screenshots

(Add screenshots here)

---

**Built with ❤️ by Sourish Maity.**
*Pixel Art Assets & Cyberpunk Theme inspired by the Modding Community.*
