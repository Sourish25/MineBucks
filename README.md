# MineBucks 🟩💰

**The Ultimate Revenue & Stats Tracker for Minecraft Mod Creators.**

> *Built by Modders, for Modders.*

MineBucks is an open-source Android utility designed to solve the "Black Box" problem of modding revenue. It allows creators on Modrinth to track their earnings, observing trends, and get notified of payouts without constantly refreshing a webpage.

---

## 🛑 The Engineering Challenges (Read This First)

Building MineBucks wasn't just about calling an API. We faced multiple platform limitations that required creative (and sometimes hacky) engineering solutions. If you are a developer looking to contribute, understanding these is crucial.

### 1. The "Missing Data" Dilemma 🕵️‍♂️

**Problem**: The official Modrinth API is fantastic for *public* stats (downloads, followers), but it **DOES NOT** provide an endpoint for "Current Account Balance" or "Pending Payouts".

* We checked the docs: `payouts` endpoint relates to *transactions*, not the live "wallet" balance.
* We asked the community: "It's hidden in the dashboard HTML."

**Solution: The Hybrid Scraper**
We had to build a custom scraper logic.

1. **Auth**: We cannot plain-text scrape because the dashboard is behind a login wall.
2. **Implementation**: We use a background `OkHttp` call that mimics the browser's headers, injecting the User's Auth Token (intercepted during login) to fetch the raw HTML of the dashboard.
3. **Parsing**: We use **Jsoup** to hunt for specific DIV classes (e.g., `.stat-value`, `.revenue-box`) to extracting the string `"$124.50"`.

**Limitation**: This is **fragile**. If Modrinth changes their CSS class names tomorrow, the "Balance" feature breaks.

* **Contribution Needed**: A robust, server-side parser or official API advocacy.

### 2. The Authentication Heist 🔐

**Problem**: Modrinth uses strict OAuth2 or PATs (Personal Access Tokens). Asking a mobile user to "Go to PC > Settings > Developer > Generate Token > Select Scopes > Copy > Email to Phone > Paste" is a UX nightmare.
**Solution**: **WebView Token Interception**.

* We load `modrinth.com/login` in a sandboxed, invisible WebView.
* The user logs in with GitHub/Discord as normal.
* We attach a `CookieManager` listener. As soon as the browser receives the session cookie `connect.sid` or the LocalStorage key `auth_token`, we **intercept it**.
* We then encrypt this token into Android's `EncryptedSharedPreferences`.

### 3. Battery Life vs. Real-Time Anxiety 🔋

**Problem**: Users want to know *instantly* when they make money.
**Solution**: We compromised for **Standardization**.

* Android's **Doze Mode** kills background apps that network too often.
* We use **WorkManager** with a `PeriodicWorkRequest` of **6 Hours**.
* **Optimization**: The worker has a `CONSTRAINT_NETWORK_CONNECTED`. If you are in a subway (offline), the app won't wake up, saving battery. It only wakes when data flow is possible.

---

## 📚 User Guide: How to Get Started

### Option A: The "Lazy" Login (Recommended)

1. Open MineBucks.
2. Click **"Login with Modrinth"**.
3. Enter your credentials in the official login page.
4. **Done**. The app automates the rest.

### Option B: Manual PAT (For the Paranoid)

If you prefer total control, you can feed the app a PAT.

1. Go to **Modrinth.com > Settings > API Keys**.
2. Create a Key named `MineBucks`.
3. **REQUIRED SCOPES**:
    * `USER_READ`: To know who you are.
    * `PROJECTS_READ`: To list your mods.
    * `PAYOUTS_READ`: To see your transaction history.
    * `ANALYTICS_READ`: To graph your downloads.
4. Copy the key and paste it into the "Manual Token" field in the app.

---

## ✨ Feature Deep Dive

### 1. Revenue History Graph 📈

We don't just show "Total". We realized that accurate trend analysis matters.

* The app takes a snapshot of your revenue every day at 00:00.
* It stores this in a local **Room Database (SQLite)**.
* We use the **Vico Graphing Library** to render this history.
* **Smoothing**: We apply a Bezier curve variance so the graph looks organic, not jagged.

### 2. Passive Widget 📱

* Most widgets drain battery because they run their own update timers.
* **Ours does not**. It connects to the main App Database. It only updates when the *Master Worker* (the 6-hour job) finishes.
* **Cost**: 0% extra battery.

### 3. Smart Notifications 🔔

* The app compares `LastKnownBalance` vs `CurrentBalance`.
* If `Current > LastKnown`, it triggers a notification.
* **Icon**: We designed a custom 8-bit Pixel Art Dollar sign to match the Minecraft aesthetic.

---

## 🤝 How to Contribute (We Need You!)

This project is open source because maintaining a scraper is a community effort.

### Top Priority Issues

1. **Add CurseForge Support**: Their API is strictly hashed. We need someone with CFCore knowledge to reverse-engineer a safe read-only access method.
2. **Backup/Restore**: Currently, if you uninstall, you lose your "History" (Graph data). We need a "Export to JSON" feature.
3. **Localization**: Translating the UI into Spanish, Russian, and Chinese (huge modding communities).

### Tech Stack for Devs

* **Language**: Kotlin (100%).
* **Architecture**: MVVM + Clean Architecture.
* **UI**: Jetpack Compose.
* **DI**: Hilt (Dagger).
* **Async**: Coroutines + Flow.

---

## 📄 License & Legal

* **License**: MIT (Free to fork, modify, and resell).
* **Disclaimer**: This app is not affiliated with Modrinth or Mojang. It is a third-party tool. Use at your own risk.

**Built with ❤️ (and too much coffee) by Sourish Maity.**
