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
package com.example.mortar.screen;

import android.os.Bundle;
import com.example.flow.appflow.Screen;
import com.example.mortar.R;
import com.example.mortar.core.MortarDemoActivityBlueprint;
import com.example.mortar.model.Chat;
import com.example.mortar.model.Chats;
import com.example.mortar.mortarscreen.WithModule;
import com.example.mortar.view.ChatListView;
import dagger.Provides;
import flow.Flow;
import flow.Layout;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import mortar.ViewPresenter;

@Layout(R.layout.chat_list_view) @WithModule(ChatListScreen.Module.class)
public class ChatListScreen extends Screen {

  @dagger.Module(injects = ChatListView.class, addsTo = MortarDemoActivityBlueprint.Module.class)
  public static class Module {
    @Provides List<Chat> provideConversations(Chats chats) {
      return chats.getAll();
    }
  }

  @Singleton
  public static class Presenter extends ViewPresenter<ChatListView> {
    private final Flow flow;
    private final List<Chat> chats;

    @Inject Presenter(Flow flow, List<Chat> chats) {
      this.flow = flow;
      this.chats = chats;
    }

    @Override public void onLoad(Bundle savedInstanceState) {
      super.onLoad(savedInstanceState);
      ChatListView view = getView();
      if (view == null) return;

      view.showConversations(chats);
    }

    public void onConversationSelected(int position) {
      flow.goTo(new ChatScreen(position));
    }
  }
}
