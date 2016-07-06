package com.rizato.gameclient.networking;

import com.rizato.gameview.ItemTile;
import com.rizato.gameview.TerrainTile;

import java.util.List;

public class Protocol {
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
            return terrain.hashCode() * items.hashCode();
        }
    }
}