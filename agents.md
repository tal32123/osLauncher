# AGENTS.MD: A Guide for Developing TALauncher

This document outlines the vision, technical standards, and workflow for the TALauncher project. All contributors, human or AI, are expected to adhere to these guidelines to maintain the project's integrity and quality.

## 1. Project Goal & Vision

**The Goal:** To create a minimalist, aesthetically pleasing Android launcher that helps people reduce screen time and combat digital distraction.

**The Vision:** TALauncher is not just another app; it's a tool for a more intentional digital life. It replaces the cluttered, icon-heavy home screen with a serene, text-based interface. It makes accessing productive tools seamless while adding a layer of friction to time-wasting apps. The user should feel calmer and more in control of their device.

## 2. Design Philosophy & Aesthetics

*   **Minimalism is Key:** Every visual element must have a purpose. If it doesn't serve a function, it doesn't belong.
*   **Text-Centric:** The interface will be primarily text-based. Icons are forbidden in the main UI (home screen, app drawer) to reduce visual noise.
*   **Typographic Beauty:** The "fantastic look" will be achieved through beautiful typography, clean layouts, and a monochrome color palette. Font choice, spacing, and alignment are paramount.
*   **Calm & Focused:** The user experience should be tranquil. No flashy animations, no bright colors, no notification badges.
*   **Excluded Features:** To protect the app's vision, we will deliberately avoid widgets, app icons (the interface is text-only), notification badges/dots, and complex theming options.

## 3. Core Features

1.  **Home Screen:** Displays the device wallpaper (or a solid color), time, date, and a user-curated list of "Essential Apps." Supports a swipe-down gesture to open the notification shade.
2.  **App Drawer:** A simple, alphabetical, and searchable list of all installed apps. Includes an entry for "Device Settings". Long-pressing an app provides "Uninstall" and "App Info" options.
3.  **Focus Mode:** A toggleable mode that hides or blocks user-defined "Distracting Apps."
4.  **Usage Insights:** A non-intrusive screen that shows time spent in distracting apps.
5.  **Settings:** Allows the user to set a custom wallpaper or a solid background color.

Refer to the `spec.md` file for a more detailed breakdown of these features.

## 4. Android Development & Technical Standards

This project follows modern Android development best practices.

*   **Language:** **Kotlin only.** All new code must be written in Kotlin.
*   **Architecture:** **Model-View-ViewModel (MVVM).** This ensures a clean separation of UI, business logic, and data.
    *   **View:** Implemented with Jetpack Compose. Represents the UI.
    *   **ViewModel:** Holds UI state and exposes it via `StateFlow`. Handles user interactions.
    *   **Model:** Represents the data layer (e.g., Room database for app lists).
*   **UI Toolkit:** **Jetpack Compose.** We will build the entire UI declaratively with Compose. No XML layouts.
*   **Asynchronous Operations:** **Kotlin Coroutines and Flows.** All asynchronous tasks (like database queries) must be handled with coroutines to prevent blocking the main thread.
*   **Dependency Management:** Dependencies are defined in the `build.gradle.kts` files. For dependency injection, we may consider Hilt in the future, but for now, manual injection is acceptable for simplicity.
*   **Testing:** Writing tests is crucial.
    *   **Unit Tests:** For ViewModels and business logic.
    *   **Instrumentation Tests:** For UI components and database operations.

## 5. Code Style & Quality

*   **Kotlin Style Guide:** All code must adhere to the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
*   **Linting:** We will use `ktlint` to enforce a consistent code style. Before committing, ensure your code is free of linting errors.
*   **Readability:** Write clear, self-documenting code. Add comments only to explain the *why* behind complex logic, not the *what*.

## 6. Instructions for AI Agents

As an AI agent contributing to this project, you must:

1.  **Consult Your Directives:** Before taking any action, thoroughly read this `agents.md` file and the `spec.md` file to understand the project's goals and constraints.
2.  **Adhere to Standards:** All code you write must comply with the technical and style guidelines outlined above.
3.  **Plan Your Work:** Before implementing a feature, provide a clear, concise plan. Explain what you are going to do and why.
4.  **Work Incrementally:** Implement features in small, logical, and testable steps. Do not attempt to implement large, complex features in a single turn.
5.  **Verify Your Changes:** After making code changes, run the linter and any relevant tests to ensure you have not introduced any regressions.
