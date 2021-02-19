package com.cookiejarapps.android.smartcookieweb.tabs

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtension.ActionDelegate
import org.mozilla.geckoview.WebExtension.SessionTabDelegate

class TabSessionManager {
    private val mTabSessions: ArrayList<TabSession> = ArrayList()
    private var mCurrentSessionIndex = 0
    private var mTabObserver: TabObserver? = null
    private var mTrackingProtection = false

    interface TabObserver {
        fun onCurrentSession(session: TabSession?)
    }

    fun unregisterWebExtension() {
        for (session in sessions) {
            session.action = null
        }
    }

    fun setWebExtensionDelegates(
        extension: WebExtension?,
        actionDelegate: ActionDelegate?,
        tabDelegate: SessionTabDelegate?
    ) {
        for (session in sessions) {
            val sessionController = session.webExtensionController
            sessionController.setActionDelegate(extension!!, actionDelegate)
            sessionController.setTabDelegate(extension, tabDelegate)
        }
    }

    fun setUseTrackingProtection(trackingProtection: Boolean) {
        if (trackingProtection == mTrackingProtection) {
            return
        }
        mTrackingProtection = trackingProtection
        for (session in sessions) {
            session.settings.useTrackingProtection = trackingProtection
        }
    }

    fun setTabObserver(observer: TabObserver?) {
        mTabObserver = observer
    }

    fun addSession(session: TabSession) {
        sessions.add(session)
    }

    fun getSession(index: Int): TabSession? {
        return if (index >= sessions.size) {
            null
        } else sessions[index]
    }

    val currentSession: TabSession?
        get() = getSession(mCurrentSessionIndex)

    fun getSession(session: GeckoSession?): TabSession? {
        val index = sessions.indexOf(session)
        return if (index == -1) {
            null
        } else getSession(index)
    }

    fun setCurrentSession(session: TabSession) {
        var index = sessions.indexOf(session)
        if (index == -1) {
            sessions.add(session)
            index = sessions.size - 1
        }
        mCurrentSessionIndex = index
        if (mTabObserver != null) {
            mTabObserver!!.onCurrentSession(session)
        }
    }

    private fun isCurrentSession(session: TabSession): Boolean {
        return session == currentSession
    }

    fun closeSession(session: TabSession?) {
        if (session == null) {
            return
        }
        if (isCurrentSession(session)
            && mCurrentSessionIndex == sessions.size - 1
        ) {
            --mCurrentSessionIndex
        }
        session.close()
        sessions.remove(session)
    }

    fun newSession(settings: GeckoSessionSettings?): TabSession {
        val tabSession = TabSession(settings)
        sessions.add(tabSession)
        return tabSession
    }

    fun sessionCount(): Int {
        return sessions.size
    }

    fun getSessions(): ArrayList<TabSession> {
        return mTabSessions
    }

    companion object {
        val sessions = ArrayList<TabSession>()
    }
}