package com.auvious.auviousproject.cobrowse

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.auvious.auviousproject.MainActivity
import com.auvious.auvioussdk.cobrowse.AuviousCobrowse
import com.auvious.auvioussdk.cobrowse.AuviousCobrowseEndReason
import com.auvious.auvioussdk.cobrowse.AuviousCobrowseListener
import com.auvious.auvioussdk.cobrowse.AuviousCobrowseState
import java.lang.ref.WeakReference

/**
 * Co-browse the agent starts during a video call. When sharing starts the call shrinks to picture-in-picture, as it
 * does for a screen share, and once the app's own screen is back this opens the send-money page over it, so there is
 * something to share; when co-browse ends, the pages opened since close and the main page is back.
 *
 * The page is opened from the app's screen, not from the call: opened while the call is still full screen it would
 * stack on the call and go into the PiP window with it.
 */
object InCallCobrowseDemo : AuviousCobrowseListener, Application.ActivityLifecycleCallbacks {

    private var application: Application? = null
    private val pages = mutableListOf<WeakReference<Activity>>()
    private var showingPages = false
    /** Sharing started: the page opens as soon as one of the app's screens is on top */
    private var pagesPending = false
    /** The app's own screen on top (never the call or the SDK's screens) */
    private var resumedAppActivity: WeakReference<Activity>? = null

    fun install(application: Application) {
        if (this.application == null) application.registerActivityLifecycleCallbacks(this)
        this.application = application
        AuviousCobrowse.inCallListener = this
    }

    fun uninstall() {
        if (AuviousCobrowse.inCallListener === this) AuviousCobrowse.inCallListener = null
        application?.unregisterActivityLifecycleCallbacks(this)
        application = null
        resumedAppActivity = null
        pagesPending = false
    }

    override fun onCobrowseStateChanged(state: AuviousCobrowseState) {
        // Sharing again (e.g. after returning to the app) keeps the pages that are open
        if (state != AuviousCobrowseState.SHARING || showingPages || pagesPending) return
        pagesPending = true
        // The call is already in PiP: the app's screen is on top now
        resumedAppActivity?.get()?.let(::openPages)
    }

    override fun onCobrowseEnded(reason: AuviousCobrowseEndReason) {
        val application = application ?: return
        Toast.makeText(application, "Co-browse ended: $reason", Toast.LENGTH_SHORT).show()
        pagesPending = false
        if (!showingPages) return
        showingPages = false
        // Back to the main page, which stays under them
        pages.forEach { it.get()?.finish() }
        pages.clear()
    }

    private fun openPages(from: Activity) {
        pagesPending = false
        showingPages = true
        from.startActivity(Intent(from, SendMoneyActivity::class.java))
    }

    private fun isAppScreen(activity: Activity) = activity.javaClass.name.startsWith(appPackage)

    private val appPackage = MainActivity::class.java.`package`!!.name + "."

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (showingPages && activity.javaClass.name.startsWith(SendMoneyActivity::class.java.`package`!!.name)) {
            pages.add(WeakReference(activity))
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (!isAppScreen(activity)) return
        resumedAppActivity = WeakReference(activity)
        if (pagesPending) openPages(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumedAppActivity?.get() === activity) resumedAppActivity = null
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        pages.removeAll { it.get() == null || it.get() === activity }
    }
}
