
# Spec for "ZenLauncher": A Minimalist Android Launcher

## 1. High-Level Description

### 1.1. What & Why

ZenLauncher is a minimalist Android launcher designed to combat digital distraction and reduce screen time. It replaces the standard home screen with a clean, text-based interface that prioritizes focus and intention. The core philosophy is to make it easy to access productive tools while adding friction to opening time-wasting applications.

### 1.2. The Problem

Modern smartphone interfaces are designed to maximize engagement, often leading to mindless scrolling, constant notifications, and digital addiction. Users find it difficult to disconnect from distracting apps like social media, games, and news aggregators.

### 1.3. User Experience & Journeys

The user experience will be calm, focused, and intentional.

*   **The Home Screen:**
    *   **Appearance:** A clean, monochrome background. Displays only the time, date, and a user-curated list of "Essential Apps."
    *   **Interaction:** Tapping an app name opens it directly. There are no icons, widgets, or wallpapers to create visual clutter.

*   **The App Drawer:**
    *   **Appearance:** A simple, searchable, alphabetical list of all installed applications, presented as plain text.
    *   **Interaction:** Users can scroll through the list or use a search bar at the top to quickly find and launch any app.

*   **"Focus Mode" - The Core Feature:**
    *   **Setup:** The user designates specific applications as "Distracting Apps" (e.g., Instagram, TikTok, games).
    *   **Activation:** The user can toggle "Focus Mode" from the home screen.
    *   **Effect:** When active, "Distracting Apps" are hidden from the App Drawer. Attempting to launch them through other means (e.g., notifications) will be blocked or will require breaking through an intentional friction barrier (e.g., typing a reason for opening the app).

*   **Usage Insights:**
    *   **Appearance:** A simple, non-gamified screen showing the amount of time spent in "Distracting Apps" today.
    *   **Goal:** To provide gentle awareness of digital habits without creating a new source of obsessive stat-checking.

### 1.4. Desired Outcomes

*   Users feel more in control of their smartphone usage.
*   A measurable decrease in time spent on user-defined "Distracting Apps."
*   A more peaceful and intentional relationship with their digital devices.
*   The launcher itself is beautiful in its simplicity and feels fast and responsive.

## 2. Technical Details

### 2.1. Technology Stack

*   **Language:** Kotlin (Primary)
*   **UI Toolkit:** Jetpack Compose for a modern, declarative, and minimalist UI.
*   **Architecture:** MVVM (Model-View-ViewModel) to ensure a clean separation of concerns and testability.
*   **Android Architecture Components:**
    *   **ViewModel:** To manage UI-related data.
    *   **Room:** To persist user settings (list of essential apps, distracting apps, etc.).
    *   **Kotlin Flows:** For reactive data streams between the UI and data layers.
*   **Core Android APIs:**
    *   **UsageStatsManager:** To gather app usage data for the Insights feature.
    *   **PackageManager:** To get the list of all installed applications.

### 2.2. Architecture & Constraints

*   **Performance:** The launcher must be extremely lightweight and fast. It should have a minimal memory footprint and near-instantaneous response times.
*   **Battery Life:** No background services that could drain the battery. Usage stats will be queried on-demand when the user visits the Insights screen.
*   **Compatibility:** Target API Level 21 (Android 5.0 Lollipop) as the minimum to ensure broad device support, while compiling against the latest stable Android SDK.
*   **Modularity:** Code should be organized by feature (e.g., `home`, `app_drawer`, `focus_mode`, `settings`) to maintain clarity and scalability.
*   **No Network Calls:** The app will be entirely offline. No data is to be collected or sent to any server.

