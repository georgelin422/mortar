/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.mortar;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.example.flow.appflow.AppFlow;
import com.example.flow.appflow.Screen;
import com.example.flow.screenswitcher.CanShowScreen;
import com.example.flow.screenswitcher.HandlesBack;
import com.example.flow.screenswitcher.HandlesUp;
import com.example.mortar.android.ActionBarOwner;
import com.example.mortar.core.MortarDemoActivityBlueprint;
import com.example.mortar.mortarflow.AppFlowPresenter;
import flow.Flow;
import javax.inject.Inject;
import mortar.Mortar;
import mortar.MortarActivityScope;
import mortar.MortarScope;
import mortar.MortarScopeDevHelper;

import static android.content.Intent.ACTION_MAIN;
import static android.content.Intent.CATEGORY_LAUNCHER;
import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

public class MortarDemoActivity extends android.app.Activity
    implements ActionBarOwner.Activity, AppFlowPresenter.Activity {
  private MortarActivityScope activityScope;
  private ActionBarOwner.MenuAction actionBarMenuAction;

  @Inject ActionBarOwner actionBarOwner;
  @Inject AppFlowPresenter<MortarDemoActivity> appFlowPresenter;

  private CanShowScreen container;
  private HandlesBack containerAsHandlesBack;
  private HandlesUp containerAsHandlesUp;

  @Override public MortarScope getScope() {
    return activityScope;
  }

  @Override public void showScreen(Screen screen, Flow.Direction direction,
      Flow.Callback callback) {
    container.showScreen(screen, direction, callback);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (isWrongInstance()) {
      finish();
      return;
    }

    MortarScope parentScope = Mortar.getScope(getApplication());
    activityScope = Mortar.requireActivityScope(parentScope, new MortarDemoActivityBlueprint(this));
    Mortar.inject(this, this);

    activityScope.onCreate(savedInstanceState);

    actionBarOwner.takeView(this);
    appFlowPresenter.takeView(this);

    setContentView(R.layout.root_layout);
    container = (CanShowScreen) findViewById(R.id.container);
    containerAsHandlesBack = (HandlesBack) container;
    containerAsHandlesUp = (HandlesUp) container;

    AppFlow.loadInitialScreen(this);
  }

  @Override public Object getSystemService(String name) {
    if (Mortar.isScopeSystemService(name)) return activityScope;
    if (AppFlow.isAppFlowSystemService(name)) return appFlowPresenter.getAppFlow();

    return super.getSystemService(name);
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    activityScope.onSaveInstanceState(outState);
  }

  /** Inform the view about back events. */
  @Override public void onBackPressed() {
    if (!containerAsHandlesBack.onBackPressed()) super.onBackPressed();
  }

  /** Inform the view about up events. */
  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) return containerAsHandlesUp.onUpPressed();
    return super.onOptionsItemSelected(item);
  }

  /** Configure the action bar menu as required by {@link ActionBarOwner.Activity}. */
  @Override public boolean onCreateOptionsMenu(Menu menu) {
    if (actionBarMenuAction != null) {
      menu.add(actionBarMenuAction.title)
          .setShowAsActionFlags(SHOW_AS_ACTION_ALWAYS)
          .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem menuItem) {
              actionBarMenuAction.action.call();
              return true;
            }
          });
    }
    menu.add("Log Scope Hierarchy")
        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
          @Override public boolean onMenuItemClick(MenuItem item) {
            Log.d("DemoActivity", MortarScopeDevHelper.scopeHierarchyToString(activityScope));
            return true;
          }
        });
    return true;
  }

  @Override protected void onDestroy() {
    actionBarOwner.dropView(this);
    appFlowPresenter.dropView(this);

    // activityScope may be null in case isWrongInstance() returned true in onCreate()
    if (isFinishing() && activityScope != null) {
      MortarScope parentScope = Mortar.getScope(getApplication());
      parentScope.destroyChild(activityScope);
      activityScope = null;
    }

    super.onDestroy();
  }

  @Override public void setShowHomeEnabled(boolean enabled) {
    ActionBar actionBar = getActionBar();
    actionBar.setDisplayShowHomeEnabled(false);
  }

  @Override public void setUpButtonEnabled(boolean enabled) {
    ActionBar actionBar = getActionBar();
    actionBar.setDisplayHomeAsUpEnabled(enabled);
    actionBar.setHomeButtonEnabled(enabled);
  }

  @Override public void setTitle(CharSequence title) {
    getActionBar().setTitle(title);
  }

  @Override public void setMenu(ActionBarOwner.MenuAction action) {
    if (action != actionBarMenuAction) {
      actionBarMenuAction = action;
      invalidateOptionsMenu();
    }
  }

  /**
   * Dev tools and the play store (and others?) launch with a different intent, and so
   * lead to a redundant instance of this activity being spawned. <a
   * href="http://stackoverflow.com/questions/17702202/find-out-whether-the-current-activity-will-be-task-root-eventually-after-pendin"
   * >Details</a>.
   */
  private boolean isWrongInstance() {
    if (!isTaskRoot()) {
      Intent intent = getIntent();
      boolean isMainAction = intent.getAction() != null && intent.getAction().equals(ACTION_MAIN);
      return intent.hasCategory(CATEGORY_LAUNCHER) && isMainAction;
    }
    return false;
  }
}
