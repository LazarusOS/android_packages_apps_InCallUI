/*
 * Copyright (C) 2014 The MoKee OpenSource Project
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

package com.android.incallui;

import android.app.Activity;
import android.content.ContentUris;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.incallui.ContactInfoCache.ContactCacheEntry;
import com.android.incallui.ContactInfoCache.ContactInfoCacheCallback;
import com.android.services.telephony.common.CallIdentification;
import com.android.services.telephony.common.Call;

import org.mokee.location.PhoneLocation;
import org.mokee.util.MoKeeUtils;

/**
 * Handles the call card activity that pops up when a call
 * arrives
 */
public class InCallCardActivity extends Activity {
    private static final int SLIDE_IN_DURATION_MS = 500;

    private TextView mNameTextView;
    private TextView mLocationTextView;
    private ImageView mContactImage;

    private Call mCall;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

        setContentView(R.layout.card_call_incoming);

        InCallPresenter.getInstance().setCardActivity(this);

        final CallList calls = CallList.getInstance();
        mCall = calls.getIncomingCall();

        CallIdentification identification = mCall.getIdentification();

        // Setup the fields to show the information of the call
        mNameTextView = (TextView) findViewById(R.id.txt_contact_name);
        mLocationTextView = (TextView) findViewById(R.id.txt_location);
        mContactImage = (ImageView) findViewById(R.id.img_contact);

        // Setup the call button
        Button answer = (Button) findViewById(R.id.btn_answer);
        answer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InCallPresenter.getInstance().startIncomingCallUi(InCallPresenter.InCallState.INCALL);
                CallCommandClient.getInstance().answerCall(mCall.getCallId());
                finish();
            }
        });

        Button reject = (Button) findViewById(R.id.btn_reject);
        reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallCommandClient.getInstance().rejectCall(mCall, false, null);
                finish();
            }
        });

        // Slide in the dialog
        final LinearLayout vg = (LinearLayout) findViewById(R.id.root);

        vg.setTranslationY(getResources().getDimensionPixelSize(R.dimen.incoming_call_card_height));
        vg.animate().translationY(0.0f).setDuration(SLIDE_IN_DURATION_MS)
            .setInterpolator(new DecelerateInterpolator()).start();

        // Lookup contact info
        startContactInfoSearch(identification);
    }

    /**
     * Starts a query for more contact data for the save primary and secondary calls.
     */
    private void startContactInfoSearch(final CallIdentification identification) {
        final ContactInfoCache cache = ContactInfoCache.getInstance(InCallCardActivity.this);

        cache.findInfo(identification, true, new ContactInfoCacheCallback() {
                @Override
                public void onContactInfoComplete(int callId, ContactCacheEntry entry) {
                    mNameTextView.setText(entry.name == null ? entry.number : entry.name);
                    String tmp;
                    if (MoKeeUtils.isChineseLanguage()) {
                        tmp = PhoneLocation.getCityFromPhone(entry.number);
                    } else {
                        tmp = TextUtils.isEmpty(entry.location) ? CallerInfo.getGeoDescription(InCallCardActivity.this, entry.number) : entry.location;
                    }
                    String location = TextUtils.isEmpty(tmp) ? getString(R.string.unknown) : tmp;
                    mLocationTextView.setText(TextUtils.isEmpty(entry.label) ? location : entry.label + " " + location);
                    if (entry.personUri != null) {
                        CallerInfoUtils.sendViewNotification(InCallCardActivity.this, entry.personUri);
                    }
                }

                @Override
                public void onImageLoadComplete(int callId, ContactCacheEntry entry) {
                    if (entry.photo != null) {
                        Drawable current = mContactImage.getDrawable();
                        AnimationUtils.startCrossFade(mContactImage, current, entry.photo);
                    }
                }
            });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                return false;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

}
