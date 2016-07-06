package com.rizato.gameclient.networking;

import android.content.Context;
import android.content.MutableContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import com.rizato.gameclient.MainActivity;
import com.rizato.gameview.ItemTile;
import com.rizato.gameview.TerrainTile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.zip.InflaterInputStream;

/**
 * This class handles all of the networking.
 *
 * It exposes a few methods for writing commands up to the server and also
 * interprets everything coming down from the server.
 */
public class NetworkHandlerThread extends HandlerThread {
    private static final int LOGIN = 0;
    private static final int COMMAND = 1;
    private static final int START = 2;

    private NetworkHandlerThreadCallbacks mCallbacks;

    Handler mHandler;

    public NetworkHandlerThread(Context context, String name, Handler uiHandler, String url, int port) {
        super(name);
        mCallbacks = openConnection(context, uiHandler, url, port);
    }

    public NetworkHandlerThread(Context context, String name, int priority, Handler uiHandler, String url, int port) {
        super(name, priority);
        mCallbacks = openConnection(context, uiHandler, url, port);
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();

    }

    public void prepare() {
        mHandler = new Handler(getLooper(), mCallbacks);
        mHandler.obtainMessage(START).sendToTarget();
    }

    private NetworkHandlerThreadCallbacks openConnection(Context context, Handler uiHandler, String url, int port) {

        return new NetworkHandlerThreadCallbacks(url, port, context, uiHandler);
    }

    public void shutdown() throws IOException {
        mCallbacks.shutdown();
    }

    public void setUiHandler(Handler handler) {
        mCallbacks.changeHandler(handler);
    }

    public void login(String username, String password) {
        mHandler.obtainMessage(LOGIN, String.format("%s\n%s", username, password)).sendToTarget();
    }

    public void sendCommand(String command) {
        mHandler.obtainMessage(COMMAND, command).sendToTarget();
    }

    private static class NetworkHandlerThreadCallbacks implements Handler.Callback {
        private ReadProtocol reader;
        private OutputStream mOutputStream;
        Socket mSocket;
        String url;
        int port;
        Context mContext;
        Handler mUiHandler;

        public NetworkHandlerThreadCallbacks(String url, int port, Context context, Handler uiHandler) {
            mContext = context;
            this.url = url;
            this.port = port;
            mUiHandler = uiHandler;
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case START:
                    try {
                        start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                case LOGIN:
                    try {
                        login(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                case COMMAND:
                    try {
                        textToServer(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                default:
                    return false;
            }
        }

        private void textToServer(Message msg) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(baos);
            stream.writeUTF((String)msg.obj);
            mOutputStream.write(baos.toByteArray());
        }

        private void start() throws IOException {
            mSocket = new Socket(url, port);
            final DataInputStream socketInputStream
                    = new DataInputStream(mSocket.getInputStream());
            //Starting an always alive reader to read the responses from the server
            reader = new ReadProtocol(mContext, mUiHandler, socketInputStream);
            mOutputStream = mSocket.getOutputStream();
            Thread thread = new Thread(reader);
            thread.start();

        }

        private void login(Message msg) throws IOException {
            String[] values = ((String)msg.obj).split("\n");
            String username = values[0];
            String password;
            if (values.length == 2) {
                password = values[1];
            } else {
                password = "";
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(baos);
            stream.writeInt(1);
            stream.writeShort(13);
            stream.writeShort(13);
            stream.writeUTF(username);
            stream.writeUTF(password);
            stream.writeUTF("Android Client");
            stream.flush();
            mOutputStream.write(baos.toByteArray());
        }

        private void shutdown() {
            reader.cancel();
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void changeHandler(Handler uiHandler) {
            reader.setUiHandler(uiHandler);
        }
    }

    /**
     * Probably should have just done a handler thread
     */
    private static class ReadProtocol implements Runnable {
        private static final String TAG = ReadProtocol.class.getSimpleName();
        DataInputStream mInputStream;
        private volatile boolean cancelled;
        private volatile Handler mUiHandler;
        private Context mContext;
        SparseArray<Bitmap> tiles;
        private Protocol.Screen mLastScreen;

        public ReadProtocol(Context context, Handler uiHandler, DataInputStream inputStream) {
            mInputStream = inputStream;
            cancelled = false;
            tiles = new SparseArray<>();
            mUiHandler = uiHandler;
            mContext = context;
        }

        public void cancel() {
            cancelled = true;
        }

        private boolean isCancelled() {
            return cancelled;
        }


        @Override
        public void run() {
            //Yay infinite loops!
            //Max 8k
            while (!isCancelled()) {
                try {
                    if (mInputStream.available() > 0) {
//                        Log.d(TAG, "available");
                        byte command = mInputStream.readByte();
//                        Log.d(TAG, String.format("command %d", command));
                        byte temp = mInputStream.readByte();
//                        Log.d(TAG, String.format("temp %d", temp));
                        int low = ((int) temp) < 0 ? temp + 256 :temp;
                        temp = mInputStream.readByte();
//                        Log.d(TAG, String.format("temp %d", temp));
                        int mid = ((int) temp)< 0 ? temp + 256 : temp;
                        temp = mInputStream.readByte();
//                        Log.d(TAG, String.format("temp %d", temp));
                        int high = ((int) temp) < 0 ? temp + 256 : temp;
                        int length = low << 16 | mid << 8 | high;
//                        Log.d(TAG, String.format("Expected Length %d", length));
                        Integer actual = 0;
                        while (mInputStream.available() < length && !isCancelled()) {
                            //Sleeping if not enough data
                            Thread.sleep(10);
                        }
                        switch (command) {
                            case Protocol.LOGIN_RESPONSE:
                                try {
                                    actual += readLogin(mInputStream);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case Protocol.TEXT_OUT:
                                try {
                                    actual += readText(mInputStream);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case Protocol.TILE_MAPPINGS:
                                try {
                                    actual += readTiles(mInputStream);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case Protocol.ZIPPED_SCREEN:
                                try {
                                    actual += readZippedScreen(mInputStream);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case Protocol.QUIT:
                                readQuit();
                                break;
                            default:
                                actual += skip(mInputStream, length);
                                break;
                        }
//                        Log.d(TAG, String.format("expected: %d actual %d", length, actual));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private int skip(DataInputStream mInputStream, int length) throws IOException {
            for (int i = 0; i < length;i++) {
                mInputStream.readByte();
            }
            return length;
        }

        private void readQuit() {
            mUiHandler.obtainMessage(MainActivity.DisplayCallbacks.QUIT_RESPONSE).sendToTarget();
            cancel();
        }

        private int readZippedScreen(DataInputStream zipped) throws IOException {
            int size = zipped.readInt();
            int width = (size & 0xffff0000) >> 16;
            int height = size & 0xffff;
//            Log.d(TAG, String.format("width %d height %d", width, height));
            int zlength = zipped.readInt();
            int ulength = zipped.readInt();
            byte[] buf = new byte[zlength];
            zipped.read(buf, 0, zlength);
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            DataInputStream stream = new DataInputStream(new InflaterInputStream(bais));
//            Log.d(TAG, "readZippedScreen Tiles");
            List<TerrainTile> terrain = new ArrayList<>();
            for (int i = 0; i < height; i++){
                for (int j = 0; j < width; j++){
                    //Have to reverse ints here
                    int tile = stream.readUnsignedByte()
                            | stream.readUnsignedByte() << 8
                            | stream.readUnsignedByte()<<16
                            | stream.readUnsignedByte() <<24;
                    ulength -=4;
                    terrain.add(new TerrainTile(tile));
                }
            }
            Log.d(TAG, "readZippedScreen: ulen remainder " + (ulength%4));
            List<ItemTile> items = new ArrayList<>();
            for (int i = 0; i < ulength /4; i++) {
                int y = stream.readUnsignedByte();
                int x = stream.readUnsignedByte();

                int tile = stream.readUnsignedByte() | stream.readUnsignedByte() <<8;

                items.add(new ItemTile(x, y, tile));
            }
            Protocol.Screen screen = new Protocol.Screen();
            screen.items = items;
            screen.terrain = terrain;
            if (screen != mLastScreen) {
                mUiHandler.obtainMessage(MainActivity.DisplayCallbacks.SCREEN_RESPONSE, screen).sendToTarget();
                mLastScreen = screen;
            } else {
                Log.d(TAG, "readZippedScreen: Same. Skipping");
            }
            return zlength + 12;
        }

        private int readTiles(DataInputStream zipped) throws IOException {
            int zlength = zipped.readInt();
            int ulength = zipped.readInt();
            byte[] buf = new byte[zlength];
            zipped.read(buf, 0, zlength);
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            DataInputStream stream = new DataInputStream(new InflaterInputStream(bais));
            while (bais.available() > 0){
                //Read stream
                Integer tile = (int) stream.readShort();
                String path = stream.readUTF();
                //Need to verify that the length is big enough for this
                InputStream tileStream;
                try {
                    tileStream = mContext.getAssets().open(String.format(Locale.getDefault(),
                            "art/game/%s.gif", path));
                } catch (IOException e) {
//                    Log.d(TAG, "readTiles: Couldn't find art/game/"+path+".gif");
                    continue;
                }
                Bitmap bitmap = BitmapFactory.decodeStream(tileStream);
                tiles.put(tile, bitmap);
                Log.d(TAG, String.format("Tile: %d, Path: %s", tile, path));

            }
            stream.close();
            bais.close();
            mUiHandler.obtainMessage(MainActivity.DisplayCallbacks.MAP_RESPONSE, tiles).sendToTarget();
            //This literally means nothing.
            return zlength + 8;
        }

        private int readText(DataInputStream stream) throws IOException {
            byte style = stream.readByte();
            String message = stream.readUTF();
            //Log.d(TAG, String.format("Style: %d. Message: %s", style, message));
            Protocol.TextResponse text = new Protocol.TextResponse();
            text.style = style;
            text.message = message;
            mUiHandler.obtainMessage(MainActivity.DisplayCallbacks.TEXT_RESPONSE, text).sendToTarget();
            return 3 + message.getBytes("UTF-8").length;
        }

        private int readLogin(DataInputStream stream) throws IOException {
            int version = stream.readInt();
            //Log.d(TAG, String.format("Version: %d", version));
            //Log.d(TAG, String.format("VERSION: %d.%d", (version & 0xffff0000) >> 16, (version & 0xffff)));
            byte success = stream.readByte();
            mUiHandler.obtainMessage(MainActivity.DisplayCallbacks.LOG_RESPONSE, success).sendToTarget();
            if (success == 3){
                //Log.d(TAG, "LOGGED IN");
                return 5;
            } else {
                return 5 + readText(stream);
            }
        }

        public void setUiHandler(Handler uiHandler) {
            this.mUiHandler = uiHandler;
        }
    }
}