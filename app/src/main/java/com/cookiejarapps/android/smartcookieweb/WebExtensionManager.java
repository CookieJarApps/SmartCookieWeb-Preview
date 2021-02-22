package com.cookiejarapps.android.smartcookieweb;

import android.graphics.Bitmap;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cookiejarapps.android.smartcookieweb.tabs.TabSession;
import com.cookiejarapps.android.smartcookieweb.tabs.TabSessionManager;

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.Image;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebExtensionController;

import java.lang.ref.WeakReference;

class WebExtensionManager implements WebExtension.ActionDelegate,
        WebExtension.SessionTabDelegate,
        WebExtension.TabDelegate,
        WebExtensionController.PromptDelegate,
        WebExtensionController.DebuggerDelegate,
        TabSessionManager.TabObserver {
    public WebExtension extension;

    private LruCache<Image, Bitmap> mBitmapCache = new LruCache<>(5);
    private GeckoRuntime mRuntime;
    private WebExtension.Action mDefaultAction;
    private TabSessionManager mTabManager;

    private WeakReference<WebExtensionDelegate> mExtensionDelegate;

    @Nullable
    @Override
    public GeckoResult<AllowOrDeny> onInstallPrompt(final @NonNull WebExtension extension) {
        return GeckoResult.fromValue(AllowOrDeny.ALLOW);
    }

    @Nullable
    @Override
    public GeckoResult<AllowOrDeny> onUpdatePrompt(@NonNull WebExtension currentlyInstalled,
                                                   @NonNull WebExtension updatedExtension,
                                                   @NonNull String[] newPermissions,
                                                   @NonNull String[] newOrigins) {
        return GeckoResult.fromValue(AllowOrDeny.ALLOW);
    }

    @Override
    public void onExtensionListUpdated() {
        refreshExtensionList();
    }

    // We only support either one browserAction or one pageAction
    private void onAction(final WebExtension extension, final GeckoSession session,
                          final WebExtension.Action action) {
        WebExtensionDelegate delegate = mExtensionDelegate.get();
        if (delegate == null) {
            return;
        }

        WebExtension.Action resolved;

        if (session == null) {
            // This is the default action
            mDefaultAction = action;
            resolved = actionFor(delegate.getCurrentSession());
        } else {
            if (delegate.getSession(session) == null) {
                return;
            }
            delegate.getSession(session).action = action;
            if (delegate.getCurrentSession() != session) {
                // This update is not for the session that we are currently displaying,
                // no need to update the UI
                return;
            }
            resolved = action.withDefault(mDefaultAction);
        }

        updateAction(resolved);
    }

    @Override
    public GeckoResult<GeckoSession> onNewTab(WebExtension source,
                                              WebExtension.CreateTabDetails details) {
        WebExtensionDelegate delegate = mExtensionDelegate.get();
        if (delegate == null) {
            return GeckoResult.fromValue(null);
        }
        return GeckoResult.fromValue(delegate.openNewTab(details));
    }

    @Override
    public GeckoResult<AllowOrDeny> onCloseTab(WebExtension extension, GeckoSession session) {
        final WebExtensionDelegate delegate = mExtensionDelegate.get();
        if (delegate == null) {
            return GeckoResult.fromValue(AllowOrDeny.DENY);
        }

        final TabSession tabSession = mTabManager.getSession(session);
        if (tabSession != null) {
            delegate.closeTab(tabSession);
        }

        return GeckoResult.fromValue(AllowOrDeny.ALLOW);
    }

    @Override
    public GeckoResult<AllowOrDeny> onUpdateTab(WebExtension extension,
                                                GeckoSession session,
                                                WebExtension.UpdateTabDetails updateDetails) {
        final WebExtensionDelegate delegate = mExtensionDelegate.get();
        if (delegate == null) {
            return GeckoResult.fromValue(AllowOrDeny.DENY);
        }

        final TabSession tabSession = mTabManager.getSession(session);
        if (tabSession != null) {
            delegate.updateTab(tabSession, updateDetails);
        }

        return GeckoResult.fromValue(AllowOrDeny.ALLOW);
    }

    @Override
    public void onPageAction(final WebExtension extension,
                             final GeckoSession session,
                             final WebExtension.Action action) {
        onAction(extension, session, action);
    }

    @Override
    public void onBrowserAction(final WebExtension extension,
                                final GeckoSession session,
                                final WebExtension.Action action) {
        onAction(extension, session, action);
    }

    private WebExtension.Action actionFor(TabSession session) {
        if (session.action == null) {
            return mDefaultAction;
        } else {
            return session.action.withDefault(mDefaultAction);
        }
    }

    private void updateAction(WebExtension.Action resolved) {
        WebExtensionDelegate extensionDelegate = mExtensionDelegate.get();
        if (extensionDelegate == null) {
            return;
        }
    }

    public void onClicked(TabSession session) {
        WebExtension.Action action = actionFor(session);
        if (action != null) {
            action.click();
        }
    }

    public void setExtensionDelegate(WebExtensionDelegate delegate) {
        mExtensionDelegate = new WeakReference<>(delegate);
    }

    @Override
    public void onCurrentSession(TabSession session) {
        if (mDefaultAction == null) {
            // No action was ever defined, so nothing to do
            return;
        }

        if (session.action != null) {
            updateAction(session.action.withDefault(mDefaultAction));
        } else {
            updateAction(mDefaultAction);
        }
    }

    public GeckoResult<Void> unregisterExtension() {
        if (extension == null) {
            return GeckoResult.fromValue(null);
        }

        mTabManager.unregisterWebExtension();

        return mRuntime.getWebExtensionController().uninstall(extension).accept((unused) -> {
            extension = null;
            mDefaultAction = null;
            updateAction(null);
        });
    }

    public GeckoResult<WebExtension> updateExtension() {
        if (extension == null) {
            return GeckoResult.fromValue(null);
        }

        return mRuntime.getWebExtensionController().update(extension).map(newExtension -> {
            registerExtension(newExtension);
            return newExtension;
        });
    }

    public void registerExtension(WebExtension extension) {
        extension.setActionDelegate(this);
        extension.setTabDelegate(this);
        mTabManager.setWebExtensionDelegates(extension, this, this);
        this.extension = extension;
    }

    private void refreshExtensionList() {
        mRuntime.getWebExtensionController()
                .list().accept(extensions -> {
            for (final WebExtension extension : extensions) {
                registerExtension(extension);
            }
        });
    }

    public WebExtensionManager(GeckoRuntime runtime,
                               TabSessionManager tabManager) {
        mTabManager = tabManager;
        mRuntime = runtime;
        refreshExtensionList();
    }
}
