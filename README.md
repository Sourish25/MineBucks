# MineBucks

**Automated Revenue & Analytics Tracker for Modrinth Creators**

MineBucks is an open-source Android application designed to provide comprehensive financial tracking and statistical analysis for mod developers on the Modrinth platform. It automates the retrieval of revenue data, bridging the gap between platform availability and user accessibility.

---

## 🏗️ Technical Architecture

The application is engineered using modern Android development standards, ensuring scalability, maintainability, and battery efficiency.

* **Language**: Kotlin
* **Architecture Pattern**: MVVM (Model-View-ViewModel) with Clean Architecture principles.
* **UI Framework**: Jetpack Compose (Material Design 3).
* **Dependency Injection**: Hilt (Dagger).
* **Asynchronous Processing**: Kotlin Coroutines & Flow.
* **Local Persistence**: Room Database (SQLite).
* **Network Layer**: Retrofit (API) & Jsoup (DOM Parsing).

---

## ⚙️ Engineering Challenges & Implementations

### 1. Retrieval of Reward Points (Scraping Implementation)

**Problem**: The official Modrinth API (`v2`) currently exposes public statistics (downloads, followers) and transaction history (`/payouts`), but it **does not** provide an endpoint for the real-time "Current Reward Points" or "Wallet Balance".

**Solution**:
To bypass this limitation, we implemented a secure client-side scraper.

1. **Authenticated Request**: The application utilizes the user's secure session token to make a request to the Modrinth dashboard (`modrinth.com/dashboard/revenue`).
2. **DOM Parsing**: Using **Jsoup**, the application creates a semantic model of the dashboard HTML and extracts the specific balance integer located within the revenue container class.
3. **Resilience**: The parsing logic is encapsulated in a dedicated repository interactors, allowing for rapid updates if the platform's DOM structure changes.

### 2. Authentication Mechanism (WebView Interception)

**Problem**: Implementing a full OAuth2 flow requires a callback server, and manual Personal Access Token (PAT) generation is a significant friction point for end-users.

**Implementation**:

* The app instances a sandboxed `WebView` targeting the official login portal.
* It utilizes a `JavascriptInterface` bridge to monitor the authentication state.
* Upon successful login, the application securely intercepts the `auth_token` from the browser's local storage or cookies.
* **Security**: The token is immediately encrypted using Android's `EncryptedSharedPreferences` and is never transmitted to any third-party server.

### 3. Battery-Optimized Background Synchronization

**Requirement**: Users require timely updates on revenue changes without significant battery impact.

**Implementation**:

* **WorkManager**: We utilize `PeriodicWorkRequest` configured for a 6-hour interval.
* **Constraints**: Jobs are strictly constrained to `NetworkType.CONNECTED`. The application adheres to Android's "Doze" mode guidelines, ensuring zero wake-locks are held when the device is idle.
* **Delta Logic**: Notifications are only triggered if the `current_balance` > `last_known_balance`.

---

## 📊 Feature Overview

### Revenue Visualization

* **Vico Graphing Engine**: The app renders historical revenue data using a custom implementation of the Vico library.
* **Data Normalization**: Raw data points are smoothed to visualize trends effectively, filtering out daily fluctuations.
* **Local Caching**: All historical data is persisted in a relational Room database, allowing for offline analysis.

### Passive Widget System

* The architecture includes a Home Screen Widget that observes the database source of truth.
* It does **not** run its own background service. Instead, it relies on `BroadcastReceiver` updates from the main sync worker, ensuring 0% additional battery consumption.

---

## 📖 User Guide: Authentication

MineBucks supports two methods of authentication:

### Method 1: Web Login (Standard)

1. Select "Login with Modrinth".
2. Authenticate via the secure browser window.
3. The application will automatically detect functionality and redirect to the dashboard.

### Method 2: Manual API Token (Advanced)

For users preferring granular control, a Personal Access Token (PAT) can be manually provided.

**Required API Scopes:**
To function correctly, the generated token must include the following permissions:

| Scope | Purpose |
| :--- | :--- |
| `USER_READ` | Identification of the account profile. |
| `PROJECTS_READ` | Retrieval of mod lists and statistics. |
| `PAYOUTS_READ` | Access to transaction history. |
| `ANALYTICS_READ` | (Optional) Access to detailed graph data points. |

---

## 🤝 Contributing

We welcome contributions from the community. Please adhere to the following guidelines:

### Critical Areas for Contribution

1. **CurseForge Integration**: We are seeking contributors with experience in the CurseForge/Overwolf API to implement a parallel revenue tracking module.
2. **Localization**: Translations for `values-xx/strings.xml` are needed for broader accessibility.
3. **Scraper Robustness**: Improvements to the Jsoup parsing logic to handle edge cases or UI changes on the host platform.

### Development Setup

1. Clone the repository:

    ```bash
    git clone https://github.com/Sourish25/MineBucks.git
    ```

2. Open in Android Studio (Hedgehog or later recommended).
3. Sync Gradle dependencies.

---

## 📄 License

This project is licensed under the **MIT License**.
*Disclaimer: This application is a third-party tool and is not affiliated with, endorsed by, or sponsored by Modrinth.*
