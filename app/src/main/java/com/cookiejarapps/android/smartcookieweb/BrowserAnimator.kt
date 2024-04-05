package com.cookiejarapps.android.smartcookieweb

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.NavOptions
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import kotlinx.coroutines.launch
import mozilla.components.ui.widgets.behavior.ToolbarPosition as OldToolbarPosition
import mozilla.components.concept.engine.EngineView
import java.lang.ref.WeakReference

/**
 * Handles properly animating the browser engine based on `SHOULD_ANIMATE_FLAG` passed in through
 * nav arguments.
 */
class BrowserAnimator(
    private val fragment: WeakReference<Fragment>,
    private val engineView: WeakReference<EngineView>,
    private val swipeRefresh: WeakReference<View>,
    private val viewLifecycleScope: WeakReference<LifecycleCoroutineScope>
) {

    private val unwrappedEngineView: EngineView?
        get() = engineView.get()

    private val unwrappedSwipeRefresh: View?
        get() = swipeRefresh.get()

    fun beginAnimateInIfNecessary() {
        unwrappedEngineView?.asView()?.visibility = View.VISIBLE
        unwrappedSwipeRefresh?.background = null
    }

    /**
     * Makes the swipeRefresh background a screenshot of the engineView in its current state.
     * This allows us to "animate" the engineView.
     */
    fun captureEngineViewAndDrawStatically(onComplete: () -> Unit) {
        unwrappedEngineView?.asView()?.context.let {
            viewLifecycleScope.get()?.launch {
                // isAdded check is necessary because of a bug in viewLifecycleOwner. See AC#3828
                if (!fragment.isAdded()) {
                    return@launch
                }
                unwrappedEngineView?.captureThumbnail { bitmap ->
                    if (!fragment.isAdded()) {
                        return@captureThumbnail
                    }

                    unwrappedSwipeRefresh?.apply {
                        // If the bitmap is null, the best we can do to reduce the flash is set transparent
                        background = bitmap?.toDrawable(context.resources)
                            ?: ColorDrawable(Color.TRANSPARENT)
                    }

                    unwrappedEngineView?.asView()?.visibility = View.GONE

                    onComplete()
                }
            }
        }
    }

    private fun WeakReference<Fragment>.isAdded(): Boolean {
        val unwrapped = get()
        return unwrapped != null && unwrapped.isAdded
    }

    companion object {
        fun getToolbarNavOptions(context: Context): NavOptions {
            val navOptions = NavOptions.Builder()

            when (UserPreferences(context).toolbarPosition) {
                OldToolbarPosition.TOP.ordinal -> {
                    navOptions.setEnterAnim(R.anim.fade_in)
                    navOptions.setExitAnim(R.anim.fade_out)
                }
                OldToolbarPosition.BOTTOM.ordinal -> {
                    navOptions.setEnterAnim(R.anim.fade_in_up)
                    navOptions.setExitAnim(R.anim.fade_out_down)
                }
            }

            return navOptions.build()
        }
    }
}