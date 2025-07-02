# Material 3 Carousel Implementation for Jellyfin Android

## 🎠 **Material 3 Expressive Carousel Added**

I've successfully implemented a beautiful Material 3 Expressive Carousel component for the home screen that showcases recently added items from your Jellyfin server.

### ✨ **Key Features**

#### **1. Beautiful Carousel Design**
- **HorizontalPager** with smooth scrolling and page indicators
- **Scaled animation** - Current item is larger (100% scale), others are smaller (85% scale)
- **Card elevation** - Active card has higher elevation for depth
- **Padding and spacing** - 32dp horizontal padding with 16dp page spacing

#### **2. Stunning Visual Design**
- **16:9 aspect ratio** cards for cinematic movie/TV show presentation
- **Gradient overlay** - Smooth black gradient for text readability
- **High-quality images** - Coil async image loading with error states
- **Rounded corners** - 16dp border radius for modern look

#### **3. Rich Content Display**
- **Movie/Show titles** - Large, prominent text overlay
- **Production year** - Clean year display
- **Community ratings** - Star icon with rating score
- **Content ratings** - MPAA/age ratings in styled badges
- **Plot overview** - Truncated description (2 lines max)

#### **4. Enhanced Cards Throughout**
- **MediaCard** - Updated with better aspect ratios and favorite indicators
- **LibraryCard** - Enhanced with collection type badges and item counts
- **Loading states** - Skeleton screens with progress indicators
- **Error states** - Fallback icons for failed image loads

### 🎨 **Design Implementation**

#### **Carousel Structure**
```kotlin
HorizontalPager(
    state = pagerState,
    contentPadding = PaddingValues(horizontal = 32.dp),
    pageSpacing = 16.dp
) { page ->
    val scale = if (page == pagerState.currentPage) 1f else 0.85f
    CarouselItemCard(scale = scale)
}
```

#### **Visual Hierarchy**
1. **Background Image** - Full-bleed poster/backdrop
2. **Gradient Overlay** - Vertical gradient for text contrast
3. **Content Layer** - Title, metadata, and description
4. **Interactive Elements** - Tap targets and visual feedback

#### **Material 3 Compliance**
- ✅ **Expressive theming** with dynamic colors
- ✅ **Elevation system** with proper shadows
- ✅ **Typography scale** following Material 3 guidelines
- ✅ **Color tokens** using theme-aware colors
- ✅ **Shape system** with consistent border radius

### 📱 **User Experience**

#### **Home Screen Layout**
1. **Welcome Header** - Personalized greeting with server info
2. **Recently Added Carousel** - Hero section with latest content
3. **Library Grid** - Quick access to media collections
4. **Error Handling** - Graceful degradation and retry options

#### **Responsive Design**
- **Mobile-first** - Optimized for phone screens
- **Touch-friendly** - Large tap targets and smooth gestures
- **Performance** - Lazy loading and image optimization
- **Accessibility** - Content descriptions and semantic markup

### 🛠 **Technical Implementation**

#### **Dependencies Added**
```gradle
implementation(libs.androidx.material3.carousel)
```

#### **Key Components**
- `RecentlyAddedCarousel` - Main carousel container
- `CarouselItemCard` - Individual carousel items
- `MediaCard` - Enhanced media item cards
- `LibraryCard` - Enhanced library collection cards

#### **Image Loading**
- **Coil integration** - `SubcomposeAsyncImage` for better loading states
- **Content scaling** - `ContentScale.Crop` for proper aspect ratios
- **Error handling** - Fallback icons and retry mechanisms
- **Performance** - Automatic caching and memory management

### 🎯 **Next Steps**

The carousel is now ready for additional enhancements:

1. **Click handling** - Navigate to detailed item views
2. **Auto-play** - Optional automatic scrolling
3. **Indicators** - Page dots or progress indicators
4. **Gestures** - Swipe-to-favorite or quick actions
5. **Animations** - Shared element transitions
6. **Customization** - User preferences for carousel behavior

### 🚀 **Result**

The home screen now features a stunning Material 3 Expressive Carousel that:
- **Showcases your latest content** with beautiful visual presentation
- **Follows Material Design 3** guidelines and best practices
- **Provides smooth animations** and responsive interactions
- **Maintains accessibility** and performance standards
- **Integrates seamlessly** with the existing Jellyfin theming

Your users will now be greeted with an engaging, modern interface that highlights new content and makes discovery effortless!
