ZoomNestedScrollView adds pinch‑to‑zoom 🔎 and panning 👆 to standard scrolling, delivering gallery 🖼️ and map‑like 🗺️ interactions for nested content.

Example:

1. Add JitPack to settings.gradle: <br />
        dependencyResolutionManagement {
           repositories { 
                google() 
                mavenCentral()
                maven { url 'https://jitpack.io' }
            } 
        }

2. Add the dependency in app/build.gradle: <br />
       implementation 'com.github.EricLam554:ZoomNestedScrollView:1.0.0'

3. Use:

        <!-- The custom library view -->
        <com.ericlam554.zoom_scroll_view.ZoomNestedScrollView
            android:id="@+id/zoomScrollView"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:fillViewport="true"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintTop_toTopOf="parent">
    
            <!-- IMPORTANT: Must have exactly ONE direct child -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">
            </LinearLayout>
    
        </com.github.EricLam554.zoom_scroll_view.ZoomNestedScrollView>
