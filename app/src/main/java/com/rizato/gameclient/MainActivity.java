package com.rizato.gameclient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;

import com.rizato.gameview.GameView;
import com.rizato.gameview.ItemTile;
import com.rizato.gameview.TerrainTile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GameView.GameViewCallbacks{

    private static final String TAG = MainActivity.class.getSimpleName();
    private List<ItemTile> items;
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
        //generate list of assets & load values
        SparseArray<Bitmap> map = new SparseArray<>();
        try {
            InputStream grassStream = getAssets().open("art/game/terrain/parquet.gif");
            Bitmap grass = BitmapFactory.decodeStream(grassStream);
            grassStream.close();
            map.put(0, grass);
            InputStream playerStream = getAssets().open("art/game/players/dwarf.S.gif");
            Bitmap player = BitmapFactory.decodeStream(playerStream);
            grassStream.close();
            map.put(1, player);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<TerrainTile> terrain = new ArrayList<>();
        for (int i = 0; i < 13*30; i++) {
            //Adding 500 tiles. No borders, no priority
            terrain.add(new TerrainTile(0));
        }
        items = new ArrayList<>();
        //Adding one to 7, 7
        items.add(new ItemTile(7 << 4 | 7, 1));

        game.setMapping(map);
        game.setItems(items);
        game.setTerrain(terrain);
    }

    @Override
    public void onTileCountChanged(int horizontal, int vertical) {
        Log.d(TAG, "onTileCountChanged: "+ horizontal + " "+vertical);
        List<TerrainTile> terrain = new ArrayList<>();
        for (int i = 0; i < (horizontal+2)*(vertical+2); i++) {
            //Adding 500 tiles. No borders, no priority
            terrain.add(new TerrainTile(0));
        }
        items = new ArrayList<>();
        //Adding one to 7, 7
        items.add(new ItemTile((vertical/2+1) << 4 | (horizontal/2+1), 1));
        game.setAll(terrain, items, horizontal, vertical);
    }

    @Override
    public void onTileClicked(int x, int y) {
        items = new ArrayList<>();
        //Adding one to 7, 7
        items.add(new ItemTile(y << 4 | x, 1));
        if (game != null) {
            game.setItems(items);
        }
    }
}
