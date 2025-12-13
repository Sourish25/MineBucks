# MineBucks 🟩

**A specialized automated revenue tracking utility for Modrinth creators.**

MineBucks solves a critical pain point for the modding community: the inability to passively track earnings and project statistics without manual intervention or aggressive polling.

---

## 🛠️ System Architecture

The application is built using **Native Android (Kotlin)**, strictly adhering to **MVVM Clean Architecture**.

* **UI Layer**: Jetpack Compose (Material You dynamic theming).
* **Domain Layer**: Kotlin Coroutines & Flow for reactive data streams.
* **Data Layer**: Room Database (SQLite) with encrypted preferences for token storage.
* **Networking**: Retrofit for API calls, Jsoup for HTML parsing (fallback), and WebKit for OAuth handling.

---

## 🚧 Challenges & Engineering Decisions

### 1. The Modrinth Authentication Problem

**Challenge**: Modrinth's API requires strict OAuth2 flows which are difficult to implement purely client-side without a callback server, or rely on Personal Access Tokens (PATs) which are bad UX for end-users.
**Solution**: We implemented a **Sandboxed WebView Flow**.

* The app launches a controlled browser instance to `modrinth.com/login`.
* It utilizes `CookieManager` and `JavascriptInterface` to intercept the valid session token post-login.
* **Trade-off**: High maintenance (brittle if DOM changes), but provides the best user experience (zero manual token copy-pasting).

### 2. Background Sync vs. Battery Efficiency

**Challenge**: Users want "Real-time" alerts, but Android's *Doze Mode* kills aggressive background services to save battery.
**Solution**: Leveraging `WorkManager` with `Constraints`.

* We operate on a **6-hour periodic fetch cycle**.
* Constraints: `NetworkType.CONNECTED` only.
* **Result**: The app appears "always on" but consumes <0.1% battery daily.
* **Future Improvement**: Implement FCM (Firebase Cloud Messaging) for true push notifications from a server, removing the need for client-side polling.

### 3. Data Visualization

**Challenge**: Rendering complex financial data on mobile screens.
**Solution**: Integration of **Vico**, a Compose-native graphing library. We apply Standard Deviation smoothing to the data points to prevent "spiky" graphs caused by irregular payout times.

---

## ✅ Implemented Features

### Core

* **Secure Dashboard**: Local encrypted storage of financial data.
* **Revenue History**: Day-by-day persistence using Room entities (`@Entity data class DailyRevenue`).
* **Smart Notifications**: Logic `if (newBalance > oldBalance)` triggers a high-priority refined notification.

### Monetization

* **AdMob Integration**: Non-intrusive "Support Us" model using Rewarded Video Ads.
* **Verification**: `app-ads.txt` hosted via GitHub Pages for full compliance.

---

## 🔮 Future Roadmap & Limitations

### Current Limitations

1. **Platform Support**: Currently only supports Modrinth. CurseForge support is blocked by their complex API requirements.
2. **Scraping Fragility**: If Modrinth changes their CSS classes or Auth flow, the WebView interceptor will break until patched.

### Planned Improvements

1. **Multi-Platform Aggregation**: Unified dashboard for CurseForge + Modrinth.
2. **Server-Side Auth**: Move authentication to a backend proxy to allow true OAuth2 compliance.
3. **Widget Interactivity**: Add "Refresh" buttons to widgets (requires Android 12+ Broadcast redesign).

---

## 📦 Build Instructions

1. **Clone**: `git clone https://github.com/Sourish25/MineBucks.git`
2. **Secret Management**:
    * No secrets are needed to build `debug`.
    * Create a `secrets.properties` for release signing keys if forking.
3. **AdMob**:
    * This project is configured with specific Ad Unit IDs. If forking, replace `AdManager.kt` constants and `AndroidManifest.xml` metadata with your own.

---

**Contributions**: Open for PRs. Please follow the `ktlint` style guide.
**License**: MIT.
