package com.rizato.gameview;

/**
 * POJO that holds the information for drawing an object. Holds the X, Y, and int for mapping
 * to a bitmap
 */
@SuppressWarnings("unused")
public class ItemTile {
    protected  final int mTile;
    protected final int mX;
    protected final int mY;

    public ItemTile(int x, int y, int tile) {
        mTile = tile;
        mX = x;
        mY = y;
    }

    public ItemTile(int coordinates, int tile) {
        mTile = tile;
        mX = coordinates & 0xFF;
        mY = coordinates >> 8 & 0xFF;
    }

    public int getTile(){
        return mTile;
    }

    public int getX() {
        return mX;
    }

    public int getY() {
        return mY;
    }

    @Override
    public int hashCode() {
        return mX * mY * mTile;
    }

    @Override
    public boolean equals(Object o) {
        return o != null
                && o instanceof ItemTile
                && ((ItemTile) o).mTile == mTile
                && ((ItemTile) o).mX == mX
                && ((ItemTile) o).mY == mY;
    }
}
