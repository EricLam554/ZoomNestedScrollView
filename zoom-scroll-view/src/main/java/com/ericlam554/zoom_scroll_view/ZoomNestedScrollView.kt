package com.ericlam554.zoom_scroll_view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import androidx.core.widget.NestedScrollView
import kotlin.math.sign
import kotlin.math.sqrt

@SuppressLint("ClickableViewAccessibility")
class ZoomNestedScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr), ScaleGestureDetector.OnScaleGestureListener {

    private enum class Mode { NONE, DRAG, ZOOM }

    private companion object {
        private const val MIN_ZOOM = 1.0f
        private const val MAX_ZOOM = 4.0f
    }

    private var mode = Mode.NONE
    private var scale = 1.0f
    private var lastScaleFactor = 0f

    private var startX = 0f
    private var startY = 0f

    private var dx = 0f
    private var dy = 0f
    private var prevDx = 0f
    private var prevDy = 0f

    private val scaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, this)

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var interceptDownX = 0f
    private var interceptDownY = 0f

    /**
     * Override to ignore requestDisallowInterceptTouchEvent from children.
     * This ensures the child views cannot prevent us from intercepting their events
     * (e.g., stopping a WebView from consuming scroll events).
     */
    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // Do nothing. We want to be able to intercept events whenever we decide they are gestures.
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        handleTouch(event)
        val handling = (mode != Mode.NONE) || scale > MIN_ZOOM || scaleDetector.isInProgress
        return if (handling) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    private fun handleTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (scale > MIN_ZOOM) {
                    mode = Mode.DRAG
                    startX = event.x - prevDx
                    startY = event.y - prevDy
                } else {
                    mode = Mode.NONE
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == Mode.DRAG) {
                    dx = event.x - startX
                    dy = event.y - startY
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                mode = Mode.ZOOM
            }

            MotionEvent.ACTION_POINTER_UP -> {
                mode = Mode.DRAG
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mode = Mode.NONE
                prevDx = dx
                prevDy = dy
                lastScaleFactor = 0f
            }
        }

        scaleDetector.onTouchEvent(event)

        if ((mode == Mode.DRAG && scale >= MIN_ZOOM) || mode == Mode.ZOOM) {
            parent?.requestDisallowInterceptTouchEvent(true)
            val child = childView()
            if (child != null) {
                clampTranslationAndApply(child)
            }
        } else {
            parent?.requestDisallowInterceptTouchEvent(false)
        }
    }

    private fun childView(): View? = if (childCount > 0) getChildAt(0) else null

    private fun clampTranslationAndApply(child: View) {
        val childWidth = child.width.toFloat()
        val childHeight = child.height.toFloat()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        dx = clamp(dx, childWidth, viewWidth, scale)
        dy = clamp(dy, childHeight, viewHeight, scale)

        child.scaleX = scale
        child.scaleY = scale
        child.translationX = dx
        child.translationY = dy
    }

    /**
     * Clamps translation value so the scaled content stays within viewport boundaries.
     * If content is smaller than viewport, it is centered (returns 0).
     */
    private fun clamp(
        translation: Float, childSize: Float, viewportSize: Float, scale: Float
    ): Float {
        if (scale == MIN_ZOOM) {
            return 0f
        }
        val scaledSize = childSize * scale
        if (scaledSize <= viewportSize) {
            return 0f
        }
        // Max translation (positive) aligns the start/left/top edge of content to start of viewport
        val maxTrans = (scaledSize - childSize) / 2f
        // Min translation (negative) aligns the end/right/bottom edge of content to end of viewport
        val minTrans = viewportSize - (childSize + scaledSize) / 2f

        return translation.coerceIn(minTrans, maxTrans)
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = true

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        val scaleFactor = detector.scaleFactor
        if (lastScaleFactor == 0f || (sign(scaleFactor) == sign(lastScaleFactor))) {
            scale *= scaleFactor
            scale = scale.coerceIn(MIN_ZOOM, MAX_ZOOM)
            lastScaleFactor = scaleFactor
        } else {
            lastScaleFactor = 0f
        }

        val child = childView()
        if (child != null) {
            clampTranslationAndApply(child)
        }
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        lastScaleFactor = 0f
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                interceptDownX = ev.x
                interceptDownY = ev.y

                if (scale > MIN_ZOOM) {
                    startX = ev.x - prevDx
                    startY = ev.y - prevDy
                }

                super.onInterceptTouchEvent(ev)
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                val dxMove = ev.x - interceptDownX
                val dyMove = ev.y - interceptDownY
                val distance = sqrt(dxMove * dxMove + dyMove * dyMove)

                if (distance > touchSlop) {
                    if (scale > MIN_ZOOM) {
                        mode = Mode.DRAG
                    }
                    return true
                }
                return false
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mode = Mode.NONE
                return false
            }

            else -> return true
        }
    }
}