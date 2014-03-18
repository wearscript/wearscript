package com.dappervision.wearscript.managers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.dappervision.wearscript.BackgroundService;
import com.dappervision.wearscript.dataproviders.PebbleEventReceiver;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PebbleManager extends Manager{
    private static final String TAG = "PebbleManager";
    private static final String CLICK = "onPebbleClick";

    private final static UUID PEBBLE_APP_UUID = UUID.fromString("88c99af8-9512-4e23-b79e-ba437c788446");
    private PebbleEventReceiver dataReceiver;
    private final PebbleMessageManager pebbleMessageManager;

    public static class Cmd {
        public static final int Cmd_setText = 0;
        public static final int Cmd_singleClick = 1;
        public static final int Cmd_longClick = 2;
        public static final int Cmd_accelTap = 3;
        public static final int Cmd_vibe = 4;
        public static final int Cmd_setScrollable = 5;
        public static final int Cmd_setStyle = 6;
        public static final int Cmd_setFullScreen = 7;
        public static final int Cmd_accelData = 8;
        public static final int Cmd_getAccelData = 9;
        public static final int Cmd_configAcceldata = 10;
        public static final int Cmd_configButtons = 11;
    }


    public PebbleManager(Context context, BackgroundService bs) {
        super(bs);
        dataReceiver = new PebbleEventReceiver(PEBBLE_APP_UUID, this);
        PebbleKit.registerReceivedDataHandler(context, dataReceiver);
        pebbleMessageManager = new PebbleMessageManager(context);
    }

    public void onPebbleClick(String button) {
        registerCallback("SINGLECLICK: " + button, CLICK);
        makeCall("onPebbleClick", String.format("'%s'", button));
    }

    public void onPebbleLongClick(String button) {
        registerCallback("LONGCLICK: " + button, "onPebbleLongClick");
        makeCall("onPebbleLongClick", String.format("'%s'", button));
    }


    public void pebbleSetTitle(String title, boolean clear) {
        PebbleDictionary data = new PebbleDictionary();
        data.addInt8(0, (byte) Cmd.Cmd_setText);
        data.addString(1, title);
        if(clear)
            data.addInt32(4, 1);
        pebbleMessageManager.offer(data);
    }

    public void pebbleSetBody(String body, boolean clear) {
        PebbleDictionary data = new PebbleDictionary();
        data.addInt8(0, (byte) Cmd.Cmd_setText);
        data.addString(3, body);
        if(clear)
            data.addInt32(4, 1);
        pebbleMessageManager.offer(data);
    }

    public void pebbleSetSubTitle(String subTitle, boolean clear) {
        PebbleDictionary data = new PebbleDictionary();
        data.addInt8(0, (byte) Cmd.Cmd_setText);
        data.addString(2, subTitle);
        if(clear)
            data.addInt32(4, 1);
        pebbleMessageManager.offer(data);
    }

    public void pebbleVibe(int type) {
        PebbleDictionary data = new PebbleDictionary();
        data.addInt8(0, (byte) Cmd.Cmd_vibe);
        data.addInt32(1, type);
        pebbleMessageManager.offer(data);
    }

    public void pebbleAccelSensorOn(int rate) {
        PebbleDictionary data = new PebbleDictionary();

        data.addInt8(0, (byte) Cmd.Cmd_configAcceldata);
        data.addInt32(1, rate); // Set rate 10 25 50 100
        data.addInt32(2, 1); // Hard code 1 for data samples
        data.addInt32(3, 1); // subscribe
        pebbleMessageManager.offer(data);
    }

    public void pebbleconfigGesture(boolean up, boolean down, boolean back, boolean select) {
        PebbleDictionary data = new PebbleDictionary();
        // set up all buttons
        // need to figure out this enum
        data.addInt32(1, up ? 1 : 0);
        data.addInt32(2, down ? 1 : 0);
        data.addInt32(3, back ? 1 : 0);
        data.addInt32(4, select ? 1 : 0);
    }

    public class PebbleMessageManager implements Runnable {
        public Handler messageHandler;
        private final BlockingQueue<PebbleDictionary> messageQueue = new LinkedBlockingQueue<PebbleDictionary>();
        private Boolean isMessagePending = Boolean.valueOf(false);
        private Context context;

        public PebbleMessageManager(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            Looper.prepare();
            messageHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Log.w(this.getClass().getSimpleName(), "Please post() your blocking runnables to Mr Manager, " +
                            "don't use sendMessage()");
                }

            };
            Looper.loop();
        }

        private void consumeAsync() {
            messageHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (isMessagePending) {
                        if (isMessagePending.booleanValue()) {
                            return;
                        }

                        synchronized (messageQueue) {
                            if (messageQueue.size() == 0) {
                                return;
                            }
                            PebbleKit.sendDataToPebble(context, PEBBLE_APP_UUID, messageQueue.peek());
                        }

                        isMessagePending = Boolean.valueOf(true);
                    }
                }
            });
        }

        public void notifyAckReceivedAsync() {
            messageHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (isMessagePending) {
                        isMessagePending = Boolean.valueOf(false);
                    }
                    if(!messageQueue.isEmpty())
                        messageQueue.remove();
                }
            });
            consumeAsync();
        }

        public void notifyNackReceivedAsync() {
            messageHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (isMessagePending) {
                        isMessagePending = Boolean.valueOf(false);
                    }
                }
            });
            consumeAsync();
        }

        public boolean offer(final PebbleDictionary data) {
            final boolean success = messageQueue.offer(data);

            if (success) {
                consumeAsync();
            }

            return success;
        }
    }



}
