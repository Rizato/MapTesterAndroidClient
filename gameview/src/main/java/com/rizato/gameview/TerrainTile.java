package com.rizato.gameview;

/**
 * This is a POJO for holding the tile information.
 * It holds the int for the mapping, as well as all of the bordering information.
 * We don't do any of the bordering yet though. That would be a pain.
 */
@SuppressWarnings("unused")
public class TerrainTile {
    private final boolean mIsInBorderEnabled;
    private final boolean mIsOutBorderEnabled;
    private final boolean mHasBorders;
    private final int mBorderPriority;
    private final int mTile;

    public TerrainTile(int tile, int priority, boolean hasBorders, boolean bordersOut, boolean bordersIn) {
        mTile = tile;
        mBorderPriority = priority;
        mHasBorders = hasBorders;
        mIsOutBorderEnabled = bordersOut;
        mIsInBorderEnabled = bordersIn;
    }

    public TerrainTile(int terrain) {
        //Parses the border & tile info from the integer
        mHasBorders = (terrain >> 29 & 0x1) == 1;
        mIsInBorderEnabled = (terrain >> 30 & 0x1) == 1;
        mIsOutBorderEnabled = (terrain >> 31 & 0x1) == 1;
        mBorderPriority = terrain >> 16 & 0x1FFF;
        mTile = terrain & 0xFFFF;
    }

    public boolean hasBorders() {
        return mHasBorders;
    }

    public boolean isInBorderEnabled() {
        return mIsInBorderEnabled;
    }

    public boolean isOutBorderEnabled() {
        return mIsOutBorderEnabled;
    }

    public int getTile() {
        return mTile;
    }

    public int getBorderPriority() {
        return mBorderPriority;
    }
}
