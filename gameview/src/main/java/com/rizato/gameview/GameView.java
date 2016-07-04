package com.rizato.gameview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * This class displays a grid of bitmaps. It takes in a list of integers which are mapped
 * to paths. At those paths are bitmaps. The view then loads all of the bitmaps where they are
 * supposed to be on the grid.
 *
 * The gameview expects that there will be an extra making a ring around the viewable area.
 * This is for bordering, if I ever get that far.
 *
 * Future ideas
 *  System announcements get drawn over the center of the screen
 *
 *  TODO Borders
 */
@SuppressWarnings("unused")
public class GameView extends View {
    private static final String TAG = GameView.class.getSimpleName();
    //Attributes
    private int mVerticalTileCount;
    private int mHorizontalTileCount;
    private int mImageTileSize;
    private boolean mIsZoomEnabled;

    //Drawn content
    private List<TerrainTile> mTerrain;
    private List<ItemTile> mObjects;
    private SparseArray<Bitmap> mImageMap;

    //Draw help (less allocations
    private Integer mPaddingStart = null;
    private Integer mPaddingEnd = null;
    private Integer mPaddingTop = null;
    private Integer mPaddingBottom = null;
    private float mScale = 1f;
    private Paint mBitmapPaint;
    private Rect mDest;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleDetector;

    //Listeners
    private List<GameViewCallbacks> mCallbacks;


    public GameView(Context context) {
        super(context);
        prep(context);
        init(null, 0);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        prep(context);
        init(attrs, 0);
    }

    public GameView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        prep(context);
        init(attrs, defStyle);
    }

    /**
     * Creating callback list.
     * Setting up gesture detectors.
     * Loading default settings
     * @param context
     */
    private void prep(Context context) {
        mCallbacks = new ArrayList<>();
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context,new TapListener() );
        mVerticalTileCount = context.getResources()
                .getInteger(R.integer.game_view_default_tile_count);
        mHorizontalTileCount = context.getResources()
                .getInteger(R.integer.game_view_default_tile_count);
        mImageTileSize = context.getResources().getInteger(R.integer.gave_view_default_tile_size);
        mIsZoomEnabled = context.getResources()
                .getBoolean(R.bool.default_zoom);
    }

    /**
     * Loads data from the TypedArray (All the custom styles)
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.GameView, defStyle, 0);

        mVerticalTileCount = a.getInt(R.styleable.GameView_verticalTiles,
                mVerticalTileCount);
        mHorizontalTileCount = a.getInt(R.styleable.GameView_horizontalTiles,
                mHorizontalTileCount);
        mImageTileSize = a.getInt(R.styleable.GameView_imageTileSize,
                mImageTileSize);
        mIsZoomEnabled = a.getBoolean(R.styleable.GameView_zoomEnabled,
                mIsZoomEnabled);

        //release the typed array back to the system
        a.recycle();

        mBitmapPaint = new Paint();
        mDest = new Rect();
    }

    /**
     * Grabs the padding and stores it in a member variable
     */
    private void loadPadding() {
        if (mPaddingStart == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                mPaddingStart = getPaddingStart();
            } else {
                mPaddingStart = getPaddingLeft();
            }
        }
        if (mPaddingEnd == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                mPaddingEnd = getPaddingEnd();
            } else {
                mPaddingEnd = getPaddingRight();
            }
        }
        if (mPaddingBottom == null) {
            mPaddingBottom = getPaddingBottom();
        }
        if (mPaddingTop == null) {
            mPaddingTop = getPaddingTop();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Passing event to the scale detector first
        boolean flag = mScaleDetector.onTouchEvent(event);
        if (!mScaleDetector.isInProgress()) {
            //If we aren't currently scaling, pass it to the normal gesture detector
            flag = mGestureDetector.onTouchEvent(event);
        }
        return flag;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //Making sure we have padding
        loadPadding();
        //Getting the allowable height and width
        int contentWidth = getWidth() - mPaddingStart - mPaddingEnd;
        int contentHeight = getHeight() - mPaddingTop - mPaddingBottom;
        canvas.save();
        //Setting the scale
        if (mIsZoomEnabled) {
            canvas.scale(mScale, mScale, (float) contentWidth / 2, (float) contentHeight / 2);
        }
        //Doing nothing if we don't have the image map, or any terrain to load
        if (mImageMap == null || mTerrain == null) {
            //Draw placeholder images
            Log.d(TAG, "onDraw: No map or terrain");
            return;
        }
        //Computing the size of tiles
        int tileWidth = contentWidth / mHorizontalTileCount;
        int tileHeight = contentHeight / mVerticalTileCount;
        int tileSize = tileWidth < tileHeight
                ? tileWidth
                : tileHeight;

        int maxItemLength = (mHorizontalTileCount + 2) * (mVerticalTileCount + 2);
        int centerXOffset =  Math.max((contentWidth - (tileSize * mHorizontalTileCount)) / 2, 0);
        int centerYOffset = Math.max((contentHeight - (tileSize * mVerticalTileCount)) / 2, 0);
        //Drawing all terrain
        for (int i = 0; i < mTerrain.size() && i < maxItemLength; i++) {
            int x = i % (mHorizontalTileCount +2);
            int y = i / (mHorizontalTileCount +2);
            if (x < 1 || x > mHorizontalTileCount || y < 1 || y > mVerticalTileCount) {
                //These are out border tiles. Don't draw them.
                continue;
            }
            //Grab terrain
            Bitmap bmp = mImageMap.get(mTerrain.get(i).getTile());
            if (bmp != null) {
                //Draw terrain at x,y
                int imageWidthInTiles = bmp.getWidth() / mImageTileSize;
                int imageHeightInTiles = bmp.getHeight() / mImageTileSize;
                int start = (x-1) * tileSize + centerXOffset;
                int top = (y-1) * tileSize + centerYOffset;
                mDest.set(start,
                        top,
                        (start + imageHeightInTiles * tileSize),
                        (top + imageWidthInTiles * tileSize));
                canvas.drawBitmap(bmp,
                        null,
                        mDest,
                        mBitmapPaint);
            } else {
                Log.d(TAG, "onDraw: Missing Tile" + mTerrain.get(i).getTile());
            }
            //TODO Borders
        }
        if (mObjects != null) {
            for (ItemTile item : mObjects) {
                Bitmap bmp = mImageMap.get(item.getTile());
                if (bmp != null) {
                    int tileWidthInPixels  = bmp.getWidth() / mImageTileSize;
                    int tileHeightInPixels = bmp.getHeight() / mImageTileSize;
                    int start = (item.getX()-1) * tileSize + centerXOffset;
                    int top = (item.getY()-1) * tileSize + centerYOffset;
                    mDest.set(start,
                            top,
                            (start + tileWidthInPixels * tileSize),
                            (top + tileHeightInPixels * tileSize));
                    canvas.drawBitmap(bmp,
                            null,
                            mDest,
                            mBitmapPaint);
                } else {
                    Log.d(TAG, "onDraw: Missing item" + item.getTile());
                }
            }
        }
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = mImageTileSize * mHorizontalTileCount;
        int height = mImageTileSize * mVerticalTileCount;

        //Getting details
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthConstraint = MeasureSpec.getSize(widthMeasureSpec);
        switch (widthMode) {
            case MeasureSpec.AT_MOST:
                //match parent?
                width = widthConstraint;
                break;
            case MeasureSpec.EXACTLY:
                //given size
                width = widthConstraint;
                break;
            case MeasureSpec.UNSPECIFIED:
                //wrap_content
                break;
        }
        loadPadding();
        int tileSize = (width - mPaddingStart - mPaddingEnd)/ mHorizontalTileCount;
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightConstraint = MeasureSpec.getSize(heightMeasureSpec);
        switch (heightMode) {
            case MeasureSpec.AT_MOST:
                //match parent?
                height = tileSize* mVerticalTileCount + mPaddingTop + mPaddingBottom;
                height = Math.min(height, heightConstraint);
                break;
            case MeasureSpec.EXACTLY:
                //given size
                height = heightConstraint;
                break;
            case MeasureSpec.UNSPECIFIED:
                //wrap_content
                height = tileSize* mVerticalTileCount + mPaddingTop + mPaddingBottom;
                break;
        }
        int tileSizeByH = (height - mPaddingTop - mPaddingBottom)/ mVerticalTileCount;
        //Experiment
        int realSize = tileSize < tileSizeByH ? tileSize : tileSizeByH;
        if (widthMode != MeasureSpec.EXACTLY) {
            width = realSize * mHorizontalTileCount + mPaddingStart + mPaddingEnd;
            width = width >= getSuggestedMinimumWidth() ? width : getSuggestedMinimumWidth();
        }
        if (heightMode != MeasureSpec.EXACTLY) {
            height = realSize * mVerticalTileCount + mPaddingTop + mPaddingBottom;
            height = height >= getSuggestedMinimumHeight() ? height: getSuggestedMinimumHeight();
        }
        setMeasuredDimension(width, height);
    }

    public void setVerticalTileCount(int count) {
        mVerticalTileCount = count;
        mScale = 1;
        invalidate();
        requestLayout();
    }

    public void setHorizontalTileCount(int count) {
        mHorizontalTileCount = count;
        mScale = 1;
        invalidate();
        requestLayout();
    }

    public void setTileCount(int horizontal, int vertical) {
        mHorizontalTileCount = horizontal;
        mVerticalTileCount = vertical;
        mScale = 1;
        invalidate();
        requestLayout();
    }

    public void setIsZoomEnabled(boolean zoom) {
        mIsZoomEnabled = zoom;
        invalidate();
        requestLayout();
    }

    public void setTerrain(List<TerrainTile> tiles) {
        mTerrain = tiles;
        invalidate();
        requestLayout();
    }

    public void setItems(List<ItemTile> objects) {
        mObjects = objects;
        invalidate();
        requestLayout();
    }


    public void setMapping(SparseArray<Bitmap> map) {
        //Force reload of all bitmaps.
        mImageMap = map;
        invalidate();
        requestLayout();
    }

    public void setAll(List<TerrainTile> terrain, List<ItemTile> items, int horizontal, int vertical) {
        mTerrain = terrain;
        mObjects = items;
        mHorizontalTileCount = horizontal;
        mVerticalTileCount = vertical;
        //TODO animate scale to 1 (This could end up looking terrible)
        mScale = 1;
        invalidate();
        requestLayout();
    }

    public int getVerticalTileCount() {
        return mVerticalTileCount;
    }

    public int getHorizontalTileCount() {
        return mHorizontalTileCount;
    }

    public boolean isZoomEnabled() {
        return mIsZoomEnabled;
    }

    public List<ItemTile> getObjects() {
        return mObjects;
    }

    public List<TerrainTile> getTerrain() {
        return mTerrain;
    }

    public SparseArray<Bitmap> getMapping() {
        return mImageMap;
    }

    public void addGameViewCallbacks(GameViewCallbacks listener) {
        mCallbacks.add(listener);
    }

    public void removeGameViewCallbacks(GameViewCallbacks listener){
        mCallbacks.remove(listener);
    }

    public interface GameViewCallbacks {
        void onTileCountChanged(int horizontal, int vertical);
        void onTileClicked(int x, int y);
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScale *= detector.getScaleFactor();
            invalidate();
            requestLayout();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            //Convert scale to new dimensions.
            //callbacks
            int adjustedHorizontalTileCount = (int) Math.max((mHorizontalTileCount / mScale), 1);
            int adjustVerticalTileCount = (int) Math.max((mVerticalTileCount / mScale),1);
            if (isZoomEnabled() && mCallbacks != null) {
                for (GameViewCallbacks callbacks: mCallbacks) {
                    callbacks.onTileCountChanged(adjustedHorizontalTileCount,
                            adjustVerticalTileCount);
                }
            }
        }
    }

    private class TapListener implements GestureDetector.OnGestureListener{

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();
            //No need to add one, because we want clicks to be in the scale of the visual tiles
            int windowWidth = getWidth() - mPaddingEnd - mPaddingStart;
            int windowHeight = getHeight() - mPaddingStart - mPaddingBottom;
            int clickedX = (int)(x /  (windowWidth) * mHorizontalTileCount+ .5);
            int clickedY = (int)(y /  (windowHeight) * mVerticalTileCount + .5);
            if (clickedX > 0
                    && clickedX <= mHorizontalTileCount
                    && clickedY > 0
                    && clickedY <= mVerticalTileCount) {
                //Hit callbacks so they can interpret
                if (mCallbacks != null) {
                    for (GameViewCallbacks callbacks : mCallbacks) {
                        callbacks.onTileClicked(clickedX, clickedY);
                    }
                }
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }
    }
}