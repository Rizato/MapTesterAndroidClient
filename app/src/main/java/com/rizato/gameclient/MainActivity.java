package com.rizato.gameclient;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.rizato.gameclient.networking.NetworkHandlerThread;
import com.rizato.gameclient.networking.Protocol;
import com.rizato.gameview.GameView;

import java.util.Locale;

/**
 * Main Activity for the demo. Handles the callbacks from the game view, manages the recyclerview with
 * chat & commands. Also,
 */
public class MainActivity extends AppCompatActivity implements GameView.GameViewCallbacks{

    private static final String TAG = MainActivity.class.getSimpleName();
    private NetworkHandlerThread networkThread;
    private GameView game;
    private ChatViewAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        prepareGameView();
        prepareChat();
        prepareCommands();
        prepareGameConnection();
    }

    //Grabs the game view
    private void prepareGameView() {
        game = (GameView) findViewById(R.id.game);
        game.addGameViewCallbacks(this);
    }

    /**
     * Sets up the adapter for the recyclerview.
     */
    private void prepareChat() {
        mAdapter = new ChatViewAdapter(this);
        RecyclerView chat = (RecyclerView) findViewById(R.id.chat);
        chat.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        chat.setAdapter(mAdapter);
    }

    /**
     * Sets up the on click listener so the button sends messages
     */
    private void prepareCommands(){
        Button send = (Button) findViewById(R.id.send);
        final TextView input = (TextView) findViewById(R.id.command);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String command = input.getText().toString();
                input.setText("");
                if (networkThread != null) {
                    networkThread.sendCommand(command);
                }
            }
        });
    }

    /**
     * Creates a game connection, or connects and existing one to this activity.
     * Logs in with the username paladin.
     */
    private void prepareGameConnection(){
        ClientApplication app = (ClientApplication) getApplication();
        Handler handler = new Handler(new DisplayCallbacks());
        if (app.getNetworkThread() == null) {
            app.setNetworkThread(new NetworkHandlerThread(this,
                    "Network",
                    handler,
//                    "192.168.1.157",
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
    protected void onStop() {
        super.onStop();
        game.removeGameViewCallbacks(this);
    }

    @Override
    public void onTileCountChanged(int horizontal, int vertical) {
        networkThread.sendCommand(String.format(Locale.getDefault(),
                "#view %d %d",
                horizontal,
                vertical));
        Log.d(TAG, "onTileCountChanged: "+ horizontal + " "+ vertical);
    }

    @Override
    public void onTileClicked(int x, int y) {
        networkThread.sendCommand(String.format(Locale.getDefault(),
                "mouse %d %d",
                x,
                y));
        Log.d(TAG, "onTileClicked: "+ x + " " + y);
    }

    @Override
    public void onSwipe(@GameView.GameViewCallbacks.Direction int direction) {
        switch (direction) {

            case GameView.GameViewCallbacks.EAST:
                networkThread.sendCommand("numpad-6");
                Log.d(TAG, "onSwipe: EAST");
                break;
            case GameView.GameViewCallbacks.NORTHEAST:
                networkThread.sendCommand("numpad-9");
                Log.d(TAG, "onSwipe: NORTHEAST");
                break;
            case GameView.GameViewCallbacks.NORTHWEST:
                networkThread.sendCommand("numpad-7");
                Log.d(TAG, "onSwipe: NW");
                break;
            case GameView.GameViewCallbacks.NORTH:
                networkThread.sendCommand("numpad-8");
                Log.d(TAG, "onSwipe: n");
                break;
            case GameView.GameViewCallbacks.SOUTHEAST:
                networkThread.sendCommand("numpad-3");
                Log.d(TAG, "onSwipe: SE");
                break;
            case GameView.GameViewCallbacks.SOUTHWEST:
                networkThread.sendCommand("numpad-1");
                Log.d(TAG, "onSwipe: SW");
                break;
            case GameView.GameViewCallbacks.SOUTH:
                networkThread.sendCommand("numpad-2");
                Log.d(TAG, "onSwipe: S");
                break;
            case GameView.GameViewCallbacks.WEST:
                networkThread.sendCommand("numpad-4");
                Log.d(TAG, "onSwipe: W");
                break;
        }
    }

    /**
     * Handles the callbacks from the network thread into a handler running on the main
     * thread. Can update the UI directly.
     */
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
                    loginResponse(msg);
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

        @SuppressWarnings({"UnusedParameters", "EmptyMethod"})
        public void loginResponse(Message msg) {
            //not implemented
        }

        public void screeResponse(Message msg) {
            Protocol.Screen screen = (Protocol.Screen) msg.obj;
            if (screen.x >=0 && screen.y >=0 ) {
                game.setAll(screen.terrain, screen.items, screen.x, screen.y);
            } else {
                game.setTerrain(screen.terrain);
                game.setItems(screen.items);
            }
        }

        @SuppressWarnings({"UnusedParameters", "EmptyMethod"})
        public void quitResponse(Message msg) {
            //Not implemented
        }

        public void textResponse(Message msg) {
            Protocol.TextResponse text = (Protocol.TextResponse) msg.obj;
            Log.d(TAG, "textResponse: "+text.message);
            mAdapter.addResponse(text);
        }

        public void mapResponse(Message msg) {
            @SuppressWarnings("unchecked") SparseArray<Bitmap> map = (SparseArray<Bitmap>) msg.obj;
            game.setMapping(map);
        }
    }
}
