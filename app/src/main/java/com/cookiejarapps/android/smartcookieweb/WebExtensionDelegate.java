package com.cookiejarapps.android.smartcookieweb;

import com.cookiejarapps.android.smartcookieweb.tabs.TabSession;

import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.WebExtension;

interface WebExtensionDelegate {
    default TabSession getSession(GeckoSession session) {
        return null;
    }
    default TabSession getCurrentSession() {
        return null;
    }
    default void closeTab(TabSession session) {}
    default void updateTab(TabSession session, WebExtension.UpdateTabDetails details) {}
    default TabSession openNewTab(WebExtension.CreateTabDetails details) { return null; }
}
