package com.dappervision.wearscript.managers;

import android.content.Context;

import com.dappervision.wearscript.BackgroundService;
import com.dappervision.wearscript.Log;
import com.dappervision.wearscript.dataproviders.PebbleEventReciever;
import com.getpebble.android.kit.PebbleKit;

import java.util.UUID;

public class PebbleManager extends Manager{
    private static final String TAG = "PebbleManager";
    private static final String CLICK = "onPebbleClick";

    private final static UUID PEBBLE_APP_UUID = UUID.fromString("88c99af8-9512-4e23-b79e-ba437c788446");

    private PebbleEventReciever dataReceiver;

    public PebbleManager(Context activity, BackgroundService bs) {
        super(bs);
        dataReceiver = new PebbleEventReciever(PEBBLE_APP_UUID, this);
        PebbleKit.registerReceivedDataHandler(activity, dataReceiver);
    }

    public void onPebbleClick(String button) {
        registerCallback(CLICK, CLICK);
        makeCall("onPebbleClick", String.format("'%s'", button));
    }

}
