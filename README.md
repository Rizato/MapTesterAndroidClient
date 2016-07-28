# Game View

This is a library for a tiled game view. I did this as a way to learn custom
components in Android. 

At the most basic level, this just takes in a sparse array of integer to
bitmaps, a list of those integers for terrain, and a list of items to go on the
terrain. Then, it puts them all together on the screen (in the appropriate
places of course)


*This project includes a sample application (the app module) for using this
gameview to use the map tester server I have in another project.*

## Using the Game View

#### Add it to XML

```xml
<com.rizato.gameview.GameView
        android:id="@+id/game"
        android:layout_width="260dp"
        android:layout_height="150dp"
        android:background="#000"
        app:verticalTiles="13"
        app:horizontalTiles="13"
        app:zoomEnabled="true"
        app:imageTileSize="32"
        />

```

There are four styleable attributes defined for the GameView.


* **verticalTiles:** number of tiles on the y axis
* **horizontalTiles:** number of tiles on the x axis
* **imageTileSize:** the size of the images you are using for tiles in pixels
* **zoomEnabled:** enable to disable pinch to zoom

#### Interact with the GameView object

You need to have a reference to the object to add tiles to the map. 

```java
    GameView view = (GameView) findViewById(R.id.game); 
```

At the minimum, users need to call three methods.


* **setMapping(SparseArray\<Bitmap\> mapping)** This expects int to bitmap mappings. These are used when reading the terrain and items lists.
* **setTerrain(List\<TerrainTile\> terrain)** This sets the terrain mappings as a list. The list must hold the entire grid.
* **setItems(List\<ItemTile\> items)** This sets the items mappings. Same as terrain mappings, but this can be a sparse list (ItemTiles specify their location)

The rest are setters and getters. Any setter will cause the view to redraw. The tile count setters will reset the user scale.

#### Listening for user input

The gameview defines a GameViewCallbacks interface you can implement to listed for three actions.


* **onTileClicked(x, y)** Gives the x,y in terms of tiles when one is clicked
* **onTileCountChanged(horizontal, vertical)** Gives the new dimensions of the grid. This is called as a result of user pinch to zoom. Using setters will not result in a callback.
* **onSwipe(@Direction int direction)** Gives the direction of a user swipe/fling.

@Direction is defined as follows

0. **NORTH**
1. **NORTHEAST**
2. **NORTHWEST**
3. **SOUTH**
4. **SOUTHEAST**
5. **SOUTHWEST**
6. **WEST**
7. **EAST**

## TerrainTile

The TerrainTile is a POJO with five properties. At the moment bordering is not implemented.


* **tile** The int that maps to the bitmap to be drawn
* **priority** An int priority for drawing borders. Higher priority borders are drawn if two tiles are competeting.
* **hasBorders** Boolean that determines whether this object accepts/gives borders.
* **bordersIn** Boolean that determines whether an object can draw its border on top of this tile.
* **bordersOut** Boolean that determines whether this object can draw its border on other tiles.

## ItemTile

The ItemTile is a POJO with three properties.


* **tile** The in that maps to the bitmap to be drawn.
* **x** An int for the x coordinate.
* **y** An int for the y coordinate.

## License

```
Copyright (c) 2016, Robert Lathrop
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```