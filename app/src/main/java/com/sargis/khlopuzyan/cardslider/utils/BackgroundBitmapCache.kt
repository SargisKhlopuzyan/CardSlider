package com.sargis.khlopuzyan.cardslider.utils

import android.graphics.Bitmap
import android.util.LruCache

/**
 * LruCache for caching background bitmaps for {@link DecodeBitmapTask}.
 */
class BackgroundBitmapCache {

    private lateinit var mBackgroundsCache: LruCache<Int, Bitmap>

    companion object {
        private var instance: BackgroundBitmapCache? = null

        fun getInstance(): BackgroundBitmapCache {
            if (instance == null) {
                instance = BackgroundBitmapCache()
                instance!!.init()
            }
            return instance!!
        }

    }

    private fun init() {
        val maxMemory: Int = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize: Int = maxMemory / 5

        mBackgroundsCache = object : LruCache<Int, Bitmap>(cacheSize) {
            override fun sizeOf(key: Int, bitmap: Bitmap): Int {
                // The cache size will be measured in kilobytes rather than number of items.
                return bitmap.byteCount / 1024
            }
        }
    }

    fun addBitmapToBgMemoryCache(key: Int, bitmap: Bitmap) {
        if (getBitmapFromBgMemCache(key) == null) {
            mBackgroundsCache.put(key, bitmap)
        }
    }

    fun getBitmapFromBgMemCache(key: Int): Bitmap? {
        return mBackgroundsCache.get(key)
    }

}