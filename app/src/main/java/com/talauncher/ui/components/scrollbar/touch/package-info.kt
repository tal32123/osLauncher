/**
 * Touch handling layer for the scrollbar component.
 *
 * This package provides a robust, production-ready touch handling system for scrollbar interactions,
 * following SOLID principles and Android best practices.
 *
 * ## Architecture Overview
 *
 * The touch handling layer is organized into several components with clear separation of concerns:
 *
 * ### Core Interfaces
 * - [IScrollbarTouchHandler]: Main interface for touch event processing
 * - [IScrollPositionCalculator]: Interface for position calculations (implemented in calculation layer)
 *
 * ### Implementations
 * - [ScrollbarTouchHandler]: Concrete touch handler that orchestrates gesture detection and position calculation
 * - [ScrollbarGestureDetector]: Specialized gesture detector for scrollbar interactions
 *
 * ### Data Models
 * - [TouchState]: Sealed class hierarchy representing touch states
 * - [TouchResult]: Result of touch event processing with scroll targets and state transitions
 * - [StateTransition]: Sealed class for state machine transitions
 * - [GestureType]: Enum of supported gestures (TAP, DRAG, FLING, LONG_PRESS)
 *
 * ### Configuration
 * - [GestureConfig]: Configuration for gesture detection thresholds
 * - [TouchHandlerConfig]: Configuration for touch handler behavior
 *
 * ## SOLID Principles Applied
 *
 * ### Single Responsibility Principle
 * Each class has a single, well-defined responsibility:
 * - ScrollbarGestureDetector: Gesture recognition only
 * - ScrollbarTouchHandler: Coordinate touch event processing
 * - IScrollPositionCalculator: Position calculations only
 * - TouchState: Represent touch states
 *
 * ### Open/Closed Principle
 * - Classes are open for extension through configuration and dependency injection
 * - Sealed classes allow adding new variants without modifying existing code
 * - Interfaces allow different implementations to be plugged in
 *
 * ### Liskov Substitution Principle
 * - All implementations fully implement their interface contracts
 * - ScrollbarTouchHandler can be substituted with any IScrollbarTouchHandler implementation
 *
 * ### Interface Segregation Principle
 * - Interfaces are focused and contain only relevant methods
 * - Clients depend only on the interfaces they need
 *
 * ### Dependency Inversion Principle
 * - High-level components depend on abstractions (interfaces)
 * - ScrollbarTouchHandler depends on IScrollPositionCalculator, not concrete implementations
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Create dependencies
 * val positionCalculator: IScrollPositionCalculator = MyPositionCalculator()
 * val density = LocalDensity.current
 * val scrollbarBounds = Rect(0f, 0f, 48f, 1000f)
 *
 * // Create touch handler with custom configuration
 * val touchHandler = ScrollbarTouchHandler(
 *     positionCalculator = positionCalculator,
 *     scrollbarBounds = scrollbarBounds,
 *     totalItems = 100,
 *     density = density,
 *     config = TouchHandlerConfig.Accessible
 * )
 *
 * // Handle touch events
 * val downResult = touchHandler.onTouchDown(Offset(24f, 500f), System.currentTimeMillis())
 * if (downResult.shouldConsumeEvent) {
 *     // Consume the event
 *     if (downResult.shouldTriggerHaptic) {
 *         // Trigger haptic feedback
 *     }
 * }
 *
 * // Handle move
 * val moveResult = touchHandler.onTouchMove(Offset(24f, 550f), System.currentTimeMillis())
 * if (moveResult.targetItemIndex != null) {
 *     // Scroll to the target item
 *     listState.scrollToItem(moveResult.targetItemIndex)
 * }
 *
 * // Handle up
 * val upResult = touchHandler.onTouchUp(Offset(24f, 550f), System.currentTimeMillis())
 * when (upResult.gestureType) {
 *     GestureType.TAP -> { /* Handle tap */ }
 *     GestureType.DRAG -> { /* Handle drag completion */ }
 *     GestureType.FLING -> { /* Handle fling with animation */ }
 *     else -> { /* Handle other gestures */ }
 * }
 * ```
 *
 * ## Gesture Recognition
 *
 * The layer supports four main gesture types:
 *
 * 1. **TAP**: Quick touch and release without significant movement
 *    - Used for jumping to a specific scroll position
 *    - Maximum duration: 300ms (configurable)
 *    - Maximum movement: 10dp (configurable)
 *
 * 2. **DRAG**: Continuous movement while touching
 *    - Used for smooth, controlled scrolling
 *    - Starts after exceeding drag threshold: 8dp (configurable)
 *    - Provides real-time scroll position updates
 *
 * 3. **FLING**: Fast drag with high velocity
 *    - Used for quick scrolling with momentum
 *    - Minimum velocity: 2000px/s (configurable)
 *    - Can be used to trigger animated scrolling
 *
 * 4. **LONG_PRESS**: Touch and hold without movement
 *    - Used for showing additional UI or entering special modes
 *    - Minimum duration: 500ms (configurable)
 *    - Maximum movement: 20px (configurable)
 *
 * ## State Machine
 *
 * The touch handler follows a well-defined state machine:
 *
 * ```
 * Idle -> Down -> Moving/Pressing -> Released -> Idle
 *        |                            |
 *        +----> Cancelled <-----------+
 * ```
 *
 * State transitions are represented by the [StateTransition] sealed class and are included
 * in [TouchResult] to allow the UI to respond to state changes.
 *
 * ## Threading and Performance
 *
 * - All touch processing is synchronous and happens on the UI thread
 * - Gesture detection is optimized for minimal allocations
 * - Velocity tracking uses Android's VelocityTracker for efficiency
 * - No blocking operations or long-running computations
 *
 * ## Error Handling
 *
 * The layer includes comprehensive error handling:
 * - Bounds validation for all touch events
 * - Null-safety through Kotlin's type system
 * - Input validation in configuration classes
 * - Graceful degradation when dependencies return null
 *
 * ## Accessibility
 *
 * The layer supports accessibility through:
 * - Configurable gesture thresholds
 * - Haptic feedback hooks
 * - TouchHandlerConfig.Accessible preset for accessibility-optimized behavior
 * - Forgiving touch bounds with allowDragOutsideBounds option
 *
 * ## Testing
 *
 * The architecture is designed for testability:
 * - All dependencies are injected through interfaces
 * - Pure functions for calculations
 * - Deterministic state machine behavior
 * - No hidden state or global variables
 *
 * @since 1.0.0
 */
package com.talauncher.ui.components.scrollbar.touch
