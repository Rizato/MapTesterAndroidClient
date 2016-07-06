package com.rizato.gameclient;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;

import com.rizato.gameclient.networking.NetworkHandlerThread;
import com.rizato.gameclient.networking.Protocol;
import com.rizato.gameview.GameView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GameView.GameViewCallbacks{

    private static final String TAG = MainActivity.class.getSimpleName();
    private NetworkHandlerThread networkThread;
    private GameView game;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        game = (GameView) findViewById(R.id.game);
        game.addGameViewCallbacks(this);
        ClientApplication app = (ClientApplication) getApplication();
        Handler handler = new Handler(new DisplayCallbacks());
        if (app.getNetworkThread() == null) {
            app.setNetworkThread(new NetworkHandlerThread(this,
                    "Network",
                    handler,
                    "map.rizato.com",
                    2222));
            app.getNetworkThread().start();
            app.getNetworkThread().prepare();
            app.getNetworkThread().login("paladin", " ");
        } else {
            app.getNetworkThread().setUiHandler(handler);
        }
        networkThread = app.getNetworkThread();
    }

    @Override
    public void onTileCountChanged(int horizontal, int vertical) {
        networkThread.sendCommand(String.format(Locale.getDefault(),
                "#view %d %d",
                horizontal,
                vertical));
    }

    @Override
    public void onTileClicked(int x, int y) {
        networkThread.sendCommand(String.format(Locale.getDefault(),
                "mouse %d %d",
                x,
                y));
        Log.d(TAG, "onTileClicked: "+ x + " " + y);
    }

    public class DisplayCallbacks implements Handler.Callback {
        public static final int LOG_RESPONSE = 0;
        public static final int SCREEN_RESPONSE = 1;
        public static final int MAP_RESPONSE = 2;
        public static final int QUIT_RESPONSE = 3;
        public static final int TEXT_RESPONSE = 4;

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case LOG_RESPONSE:
                    logResponse(msg);
                    return true;
                case SCREEN_RESPONSE:
                    screeResponse(msg);
                    return true;
                case MAP_RESPONSE:
                    mapResponse(msg);
                    return true;
                case QUIT_RESPONSE:
                    quitResponse(msg);
                    return true;
                case TEXT_RESPONSE:
                    textResponse(msg);
                    return true;
                default:
                    return false;
            }
        }

        public void logResponse(Message msg) {

        }

        public void screeResponse(Message msg) {
            Protocol.Screen screen = (Protocol.Screen) msg.obj;
            game.setTerrain(screen.terrain);
            game.setItems(screen.items);
        }

        public void quitResponse(Message msg) {

        }

        public void textResponse(Message msg) {
            Protocol.TextResponse text = (Protocol.TextResponse) msg.obj;
            Log.d(TAG, "textResponse: "+text.message);

        }

        public void mapResponse(Message msg) {
            SparseArray<Bitmap> map = (SparseArray<Bitmap>) msg.obj;
            game.setMapping(map);
        }
    }
}
