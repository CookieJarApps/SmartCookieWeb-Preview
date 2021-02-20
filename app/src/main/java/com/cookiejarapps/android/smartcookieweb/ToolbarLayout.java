package com.cookiejarapps.android.smartcookieweb;

import android.content.Context;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import com.cookiejarapps.android.smartcookieweb.icon.TabCountView;
import com.cookiejarapps.android.smartcookieweb.tabs.TabSessionManager;

public class ToolbarLayout extends LinearLayout {

    public interface TabListener {
        void switchToTab(int tabId);
        void onBrowserActionClick();
    }

    private AutoCompleteTextView mLocationView;
    private TabCountView mTabsCountView;
    private View mBrowserAction;
    private TabListener mTabListener;
    private TabSessionManager mSessionManager;

    public ToolbarLayout(Context context, TabSessionManager sessionManager) {
        super(context);
        mSessionManager = sessionManager;
        initView();
    }

    private void initView() {
        float dp = getContext().getResources().getDisplayMetrics().density;
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f));
        setOrientation(LinearLayout.HORIZONTAL);
        mLocationView = findViewById(R.id.location_view);

        mTabsCountView = getTabsCountButton();
        addView(mTabsCountView);
    }

    private TabCountView getTabsCountButton() {
        float dp = getContext().getResources().getDisplayMetrics().density;
        LayoutParams layoutParams = new LayoutParams((int) (24 * dp), LayoutParams.MATCH_PARENT);

        TabCountView tabCountView = new TabCountView(getContext());
        layoutParams.setMargins(0, 48, 16, 48);

        tabCountView.setLayoutParams(layoutParams);
        tabCountView.setId(R.id.tabs_button);
        tabCountView.setOnClickListener(this::onTabButtonClicked);
        return tabCountView;
    }

    public void onTabButtonClicked(View view) {
        PopupMenu tabButtonMenu = new PopupMenu(getContext(), mTabsCountView);
        for(int idx = 0; idx < mSessionManager.sessionCount(); ++idx) {
            tabButtonMenu.getMenu().add(0, idx, idx,
                    mSessionManager.getSession(idx).getTitle());
        }
        tabButtonMenu.setOnMenuItemClickListener(item -> {
            mTabListener.switchToTab(item.getItemId());
            return true;
        });
        tabButtonMenu.show();
    }

}
