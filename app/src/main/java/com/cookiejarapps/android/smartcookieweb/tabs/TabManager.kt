package com.cookiejarapps.android.smartcookieweb.tabs

import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import java.util.*

class TabManager {
    private var mCurrentSessionIndex = 0
    fun addSession(session: TabInstance) {
        sessions.add(session)
    }

    fun getSession(index: Int): TabInstance {
        return sessions[index]
    }

    var currentSession: TabInstance
        get() = getSession(mCurrentSessionIndex)
        set(session) {
            var index = sessions.indexOf(session)
            if (index == -1) {
                sessions.add(session)
                index = sessions.size - 1
            }
            mCurrentSessionIndex = index
        }

    fun getSession(session: GeckoSession?): TabInstance? {
        val index = sessions.indexOf(session)
        return if (index == -1) {
            null
        } else getSession(index)
    }

    private fun isCurrentSession(session: TabInstance): Boolean {
        return session == currentSession
    }

    fun closeSession(session: TabInstance?) {
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

    fun newSession(settings: GeckoSessionSettings?): TabInstance {
        val tabSession = TabInstance(settings)
        sessions.add(tabSession)
        return tabSession
    }

    fun sessionCount(): Int {
        return sessions.size
    }

    companion object {
        val sessions =
            ArrayList<TabInstance>()
    }
}