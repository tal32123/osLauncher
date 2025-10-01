# Junior Engineer Assignment: Monochrome Icon Implementation

## Overview
Implement a comprehensive monochrome icon feature that dynamically applies theme colors to app icons across all Android API levels and icon types. The implementation must follow Android best practices, SOLID design principles, and ensure a visually stunning user experience.

## Project Context
- **App**: osLauncher - A minimalist Android launcher
- **Current Theme System**: Material 3 with custom color palettes (see `Theme.kt`)
- **Current Icon Style**: `AppIconStyleOption` enum with THEMED, ORIGINAL, and HIDDEN options
- **Icon Display Location**: Home screen app list and search results

## Learning Objectives
1. Master Android icon type system (Adaptive Icons, legacy icons, different API levels)
2. Understand Canvas drawing, Color filters, and bitmap manipulation
3. Apply SOLID principles to icon rendering architecture
4. Implement Strategy pattern for different icon type handlers
5. Handle edge cases and API compatibility

## Requirements

### Functional Requirements

#### 1. Icon Type Support
Implement monochrome rendering for ALL icon types:
- **Adaptive Icons** (API 26+)
  - Foreground layer monochrome
  - Background layer monochrome
  - Preserve layer separation
  - Handle vector drawables in adaptive icons
- **Legacy Icons** (API < 26)
  - Bitmap icons
  - Vector drawables
  - XML drawables
- **Monochrome Icons** (API 33+)
  - Native monochrome layer support
  - Fallback when not available

#### 2. Color Application Strategy
When `AppIconStyleOption.THEMED` is selected:
- Extract the theme's primary color from `MaterialTheme.colorScheme.primary`
- Apply color uniformly across all icon pixels while preserving alpha channel
- Ensure icons remain visually distinct and recognizable
- Handle both light and dark theme modes

#### 3. Visual Quality Standards
Icons must be:
- **Sharp**: No pixelation or blurring
- **Consistent**: Same style across all icon types
- **Accessible**: Maintain sufficient contrast with background
- **Performant**: No lag when scrolling through app lists

#### 4. API Compatibility
Support Android API levels:
- API 21-25: Legacy icon rendering
- API 26-32: Adaptive icon support
- API 33+: Native monochrome icon layer

### Technical Requirements

#### Architecture & Design Patterns

##### 1. Strategy Pattern for Icon Rendering
Create separate strategies for different icon types:
```
IconRenderingStrategy (interface)
├── AdaptiveIconStrategy (API 26+)
├── LegacyIconStrategy (API < 26)
└── MonochromeIconStrategy (API 33+)
```

##### 2. Single Responsibility Principle
Each class should have ONE responsibility:
- `IconColorApplier`: Applies color to drawables/bitmaps
- `IconExtractor`: Extracts icons from PackageManager
- `IconCacheManager`: Caches rendered icons
- `IconRenderingStrategyFactory`: Creates appropriate strategy based on API level

##### 3. Dependency Injection
All dependencies should be injectable for testability:
- PackageManager wrapper
- Color provider (from theme)
- Icon cache
- Bitmap pool/recycler

##### 4. Open/Closed Principle
Design should be:
- Open for extension (new icon types, new color modes)
- Closed for modification (existing strategies shouldn't need changes)

#### Implementation Components

##### Component 1: Icon Type Detection
Create a robust system to detect icon type:
- Check API level
- Query adaptive icon availability
- Check for monochrome layer (API 33+)
- Fallback logic for edge cases

##### Component 2: Color Filter Application
Implement multiple color application techniques:
- `PorterDuff` color filter for simple cases
- Canvas drawing for complex cases
- Preserve alpha channel/transparency
- Handle semi-transparent icons

##### Component 3: Bitmap Optimization
- Use `BitmapPool` to avoid allocations
- Recycle bitmaps properly
- Use optimal `Bitmap.Config` (ARGB_8888 vs RGB_565)
- Consider memory constraints

##### Component 4: Caching Strategy
Implement multi-level cache:
- Memory cache (LruCache)
- Disk cache (optional for persistence)
- Cache key: `packageName + colorHex + iconSize`
- Cache invalidation when theme changes

##### Component 5: Edge Case Handling
Handle these scenarios gracefully:
- Icons with no foreground
- Fully transparent icons
- Icons that are already monochrome
- App icon updates
- Theme changes during runtime

### Testing Requirements

#### Unit Tests
Write tests for:
- Color filter creation with different colors
- Icon type detection across API levels
- Cache hit/miss scenarios
- Edge cases (null icons, invalid packages)

#### Integration Tests
Test end-to-end flows:
- Theme change triggers icon re-rendering
- Scrolling app list maintains performance
- Memory usage stays within bounds

#### Visual Tests
Create visual regression tests:
- Screenshot tests for different icon types
- Before/after comparison
- Different theme colors

### Performance Requirements

1. **Initial Load**: First 20 icons render < 100ms
2. **Scroll Performance**: Maintain 60 FPS during scroll
3. **Memory**: Icon cache < 50MB for 100 apps
4. **Cache Hit Rate**: > 90% after initial load

### Deliverables

#### Code Files
1. `IconRenderingStrategy.kt` - Strategy interface
2. `AdaptiveIconRenderingStrategy.kt` - Adaptive icon implementation
3. `LegacyIconRenderingStrategy.kt` - Legacy icon implementation
4. `MonochromeIconRenderingStrategy.kt` - API 33+ monochrome
5. `IconColorApplier.kt` - Color application logic
6. `IconExtractor.kt` - Icon extraction from PackageManager
7. `IconCacheManager.kt` - Caching implementation
8. `IconRenderingStrategyFactory.kt` - Factory for strategy creation
9. `IconRenderer.kt` - Main entry point that uses factory + strategies

#### Test Files
1. `IconColorApplierTest.kt`
2. `IconCacheManagerTest.kt`
3. `IconRenderingStrategyTest.kt`
4. `IconRendererIntegrationTest.kt`

#### Documentation
1. **Architecture Decision Record (ADR)**: Document why you chose specific approaches
2. **API Documentation**: KDoc for all public classes/methods
3. **Performance Report**: Benchmark results with analysis
4. **Edge Case Matrix**: Document how each edge case is handled

### Integration Points

#### Where to Integrate
1. **HomeViewModel.kt** (line 183): Read `appIconStyle` setting
2. **App List Composables**: Use the IconRenderer when displaying app icons
3. **Search Results**: Apply same rendering to search results
4. **Theme Changes**: Subscribe to theme changes and invalidate cache

#### Integration Example Pattern
```kotlin
// In a Composable
val iconRenderer = remember { IconRenderer(context) }
val currentColor = MaterialTheme.colorScheme.primary
val iconStyle = viewModel.uiState.value.appIconStyle

val processedIcon = remember(app.packageName, currentColor, iconStyle) {
    when (iconStyle) {
        AppIconStyleOption.THEMED -> iconRenderer.renderMonochrome(app.packageName, currentColor)
        AppIconStyleOption.ORIGINAL -> iconRenderer.renderOriginal(app.packageName)
        AppIconStyleOption.HIDDEN -> null
    }
}
```

### Best Practices to Follow

#### Android Best Practices
1. Use `@RequiresApi` annotations for API-specific code
2. Use `Build.VERSION.SDK_INT` checks properly
3. Handle PackageManager exceptions gracefully
4. Use `LaunchedEffect` in Compose for side effects
5. Follow Material Design icon guidelines

#### Kotlin Best Practices
1. Use `sealed class` for icon result states
2. Use `data class` for icon metadata
3. Use `inline` functions for performance-critical code
4. Use coroutines for async icon loading
5. Use `Flow` for reactive icon updates

#### Performance Best Practices
1. Lazy load icons off the main thread
2. Use `remember` in Compose to avoid recomputation
3. Implement proper equals/hashCode for cache keys
4. Profile memory usage with Android Profiler
5. Use `StrictMode` during development

### Resources

#### Android Documentation
- [Adaptive Icons](https://developer.android.com/develop/ui/views/launch/icon_design_adaptive)
- [Icon Design Guidelines](https://material.io/design/iconography/product-icons.html)
- [Canvas and Drawables](https://developer.android.com/develop/ui/views/graphics/drawables)
- [Bitmap and Color](https://developer.android.com/reference/android/graphics/Bitmap)

#### Code Examples in Project
- `Theme.kt`: See how theme colors are managed
- `ThemeComponents.kt`: See color application in UI
- `HomeViewModel.kt`: See settings integration pattern
- `SettingsOptions.kt`: See enum pattern for settings

### Evaluation Criteria

#### Code Quality (30%)
- SOLID principles applied correctly
- Clean, readable code with proper naming
- Minimal code duplication (DRY)
- Proper error handling

#### Functionality (30%)
- All icon types render correctly
- Theme colors apply properly
- Performance targets met
- Edge cases handled

#### Testing (20%)
- Unit test coverage > 80%
- Integration tests pass
- Edge cases tested
- Performance tests included

#### Documentation (20%)
- Clear KDoc comments
- Architecture decisions explained
- Edge cases documented
- Integration guide complete

### Hints & Tips

1. **Start Simple**: Get one icon type working perfectly before moving to others
2. **Use Existing Tools**: Android provides `ColorFilter`, `ColorMatrix`, `PorterDuffColorFilter`
3. **Check osLauncher Pattern**: See how the theme system already works in `Theme.kt`
4. **Profile Early**: Use Android Profiler from day 1 to catch performance issues
5. **Ask Questions**: If you're stuck on an Android API, ask for clarification

### Timeline Suggestion

- **Day 1-2**: Research & Design (read docs, design architecture, write ADR)
- **Day 3-4**: Implement core rendering strategy for one icon type
- **Day 5-6**: Extend to all icon types with proper API checks
- **Day 7-8**: Implement caching and optimization
- **Day 9**: Write comprehensive tests
- **Day 10**: Documentation, benchmarking, and polish

### Success Metrics

Your implementation will be considered successful if:
1. ✅ Icons look fantastic and visually consistent
2. ✅ Works perfectly on Android 5.0 through Android 14+
3. ✅ No performance degradation compared to original icons
4. ✅ Theme color changes apply instantly
5. ✅ Code is maintainable and extensible
6. ✅ All tests pass with >80% coverage
7. ✅ Memory usage is optimal

## Questions to Consider

As you design your solution, think about:
1. How will you handle icons that are already the same color as the theme?
2. What happens if an app updates its icon while the launcher is running?
3. How will you ensure icons remain recognizable when monochrome?
4. Should you brighten/darken icons for better visibility?
5. How will you handle icons with complex gradients or multiple colors?
6. What's the tradeoff between memory cache size and performance?

## Additional Challenge (Optional)

If you finish early and want an extra challenge:
1. **Accessibility Enhancement**: Add a contrast checker to ensure icons are visible
2. **Color Modes**: Support "accent color per app" where each app gets a unique color
3. **Animation**: Smooth transition when theme color changes
4. **Advanced Caching**: Implement persistent disk cache with LRU eviction

Good luck! Remember: quality over speed. Take your time to understand the Android icon system thoroughly, and your implementation will be robust and maintainable.
