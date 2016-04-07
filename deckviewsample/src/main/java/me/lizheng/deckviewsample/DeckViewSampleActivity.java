/*
 * Copyright (C) 2016 Zheng Li <https://lizheng.me>
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.lizheng.deckviewsample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Random;

import me.lizheng.deckview.views.DeckChildView;
import me.lizheng.deckview.views.DeckView;

/**
 * Basic sample for DeckView.
 * Images are downloaded and cached using
 * Picasso "http://square.github.io/picasso/".
 * DeckView is *very* young and can only
 * afford basic functionality.
 */

public class DeckViewSampleActivity extends Activity {

    // View that stacks its children like a deck of cards
    DeckView<CardDataModel> mDeckView;

    Drawable mDefaultHeaderIcon;
    ArrayList<CardDataModel> mEntries;

    // Placeholder for when the image is being downloaded
    Bitmap mDefaultThumbnail;

    // Retain position on configuration change
    int scrollToChildIndex = -1;

    // SavedInstance bundle keys
    final String CURRENT_SCROLL = "current.scroll", CURRENT_LIST = "current.list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deck_view_sample);

        mDeckView = (DeckView) findViewById(R.id.deckview);
        mDefaultThumbnail = BitmapFactory.decodeResource(getResources(),
                R.drawable.default_thumbnail);
        mDefaultHeaderIcon = getResources().getDrawable(R.drawable.default_header_icon);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(CURRENT_LIST)) {
                mEntries = savedInstanceState.getParcelableArrayList(CURRENT_LIST);
            }

            if (savedInstanceState.containsKey(CURRENT_SCROLL)) {
                scrollToChildIndex = savedInstanceState.getInt(CURRENT_SCROLL);
            }
        }

        if (mEntries == null) {
            mEntries = new ArrayList<>();

            for (int i = 1; i < 100; i++) {
                CardDataModel cardDataModel = new CardDataModel();
                cardDataModel.id = generateUniqueKey();
                cardDataModel.title = "Image ID " + cardDataModel.id;
                mEntries.add(cardDataModel);
            }
        }

        // Callback implementation
        DeckView.Callback<CardDataModel> deckViewCallback = new DeckView.Callback<CardDataModel>() {
            @Override
            public ArrayList<CardDataModel> getData() {
                return mEntries;
            }

            @Override
            public void loadViewData(WeakReference<DeckChildView<CardDataModel>> dcv, CardDataModel item) {
                if (dcv.get() != null) {
                    dcv.get().onDataLoaded(item, mDefaultThumbnail, mDefaultHeaderIcon, item.title, Color.DKGRAY);
                }
            }

            @Override
            public void unloadViewData(CardDataModel item) {
            }

            @Override
            public void onViewDismissed(CardDataModel item) {
                mEntries.remove(item);
                mDeckView.notifyDataSetChanged();
            }

            @Override
            public void onItemClick(CardDataModel item) {
                Toast.makeText(DeckViewSampleActivity.this,
                        "Item with title: '" + item.title + "' clicked",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNoViewsToDeck() {
                Toast.makeText(DeckViewSampleActivity.this,
                        "No views to show",
                        Toast.LENGTH_SHORT).show();
            }
        };

        mDeckView.initialize(deckViewCallback);

        if (scrollToChildIndex != -1) {
            mDeckView.post(new Runnable() {
                @Override
                public void run() {
                    // Restore scroll position
                    mDeckView.scrollToChild(scrollToChildIndex);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_deck_view_sample, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Add a new item to the end of the list
        if (id == R.id.action_add) {
            CardDataModel cardDataModel = new CardDataModel();
            cardDataModel.id = generateUniqueKey();
            cardDataModel.title = "(New) Image ID " + cardDataModel.id;

            mEntries.add(cardDataModel);
            mDeckView.notifyDataSetChanged();
            return true;
        } else if (id == R.id.action_add_multiple) {
            // Add multiple items (between 5 & 10 items)
            // at random indices
            Random rand = new Random();

            // adding between 5 and 10 items
            int numberOfItemsToAdd = rand.nextInt(6) + 5;

            for (int i = 0; i < numberOfItemsToAdd; i++) {
                int atIndex = mEntries.size() > 0 ?
                        rand.nextInt(mEntries.size()) : 0;

                CardDataModel cardDataModel = new CardDataModel();
                cardDataModel.id = generateUniqueKey();
                cardDataModel.title = "(New) Image ID " + cardDataModel.id;

                mEntries.add(atIndex, cardDataModel);
            }

            mDeckView.notifyDataSetChanged();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save current scroll and the list
        int currentChildIndex = mDeckView.getCurrentChildIndex();
        outState.putInt(CURRENT_SCROLL, currentChildIndex);
        outState.putParcelableArrayList(CURRENT_LIST, mEntries);

        super.onSaveInstanceState(outState);
    }

    // Generates a key that will remain unique
    // during the application's lifecycle
    private static int generateUniqueKey() {
        return ++KEY;
    }

    private static int KEY = 0;
}
