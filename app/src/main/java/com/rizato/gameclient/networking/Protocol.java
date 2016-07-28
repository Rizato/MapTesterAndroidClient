package com.rizato.gameclient.networking;

import android.support.annotation.IntDef;

import com.rizato.gameview.ItemTile;
import com.rizato.gameview.TerrainTile;

import java.util.List;

/**
 * This class defines the commands the client can interperet.
 * It also has static classes for holding all the values from some
 * server message. (Right now we have Text Response & Screens defined)
 */
public class Protocol {
    @IntDef({LOGIN_RESPONSE, ZIPPED_SCREEN, TEXT_OUT, TILE_MAPPINGS, QUIT})
    public @interface Commands {}
    public final static byte LOGIN_RESPONSE = 2;
    public final static byte ZIPPED_SCREEN = 24;
    public final static byte TEXT_OUT = 11;
    public final static byte TILE_MAPPINGS = 8;
    public final static byte QUIT = 13;

    public static class TextResponse {
        public int style;
        public String message;
    }

    public static class Screen {
        public int x;
        public int y;
        public List<TerrainTile> terrain;
        public List<ItemTile> items;

        @Override
        public boolean equals(Object o) {
            return o != null
                    && o instanceof Screen
                    && ((Screen) o).terrain.equals(terrain)
                    && ((Screen) o).items.equals(items);
        }

        @Override
        public int hashCode() {
            return x * y * terrain.hashCode() * items.hashCode();
        }
    }
}