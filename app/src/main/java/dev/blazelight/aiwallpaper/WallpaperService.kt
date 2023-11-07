import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import dev.blazelight.aiwallpaper.WallpaperImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val ACTION_UPDATE_WALLPAPER = "dev.blazelight.aiwallpaper.ACTION_UPDATE_WALLPAPER"

class WallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        Log.i("wallpaperservice", "creating wallpaper engine")
        return WallpaperEngine()
    }

    private inner class WallpaperEngine : Engine() {
        private val paint = Paint()
        private val imageLoader = WallpaperImageLoader(applicationContext)

        private val wallpaperReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_UPDATE_WALLPAPER) {
                    Log.i("wallpaper", "Received update wallpaper intent")
                    // Update the wallpaper when the broadcast is received
                    drawBackground()
                }
            }
        }



        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            Log.i("wallpaper", "surface created")
            drawBackground()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawBackground()
        }

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            Log.i("wallpaperservice", "touch event")
            drawBackground()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Log.i("wallpaperservice", "visibility changed")
            if(visible) {
                drawBackground()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)

            // Register the broadcast receiver
            val filter = IntentFilter()
            filter.addAction(ACTION_UPDATE_WALLPAPER)
            applicationContext.registerReceiver(wallpaperReceiver, filter)
            Log.i("wallpaperservice", "registered intent receiver")
        }

        override fun onDestroy() {
            super.onDestroy()

            // Unregister the broadcast receiver
            Log.i("wallpaperservice", "unregister receiver")
            applicationContext.unregisterReceiver(wallpaperReceiver)
        }

        private fun drawBackground() {
            Log.i("wallpaperservice", "drawBackground called")
            while (true) {
                Log.i("wallpaperservice", "in drawBackground loop")
                try {
                    val canvas: Canvas? = surfaceHolder.lockCanvas()
                    //surfaceHolder.lockCanvas()
                    canvas?.let { drawingCanvas ->
                        // Clear the canvas
                        drawingCanvas.drawColor(Color.BLACK)

                        // Use a coroutine to load and draw the latest wallpaper image asynchronously
                        GlobalScope.launch(Dispatchers.IO) {
                            val wallpaper = imageLoader.getLatestWallpaper()

                            // Switch back to the main thread to draw the bitmap on the canvas
                            withContext(Dispatchers.Main) {
                                wallpaper?.let { bitmap ->
                                    val dstRect = calculateDestinationRect(
                                        bitmap,
                                        drawingCanvas.width,
                                        drawingCanvas.height
                                    )
                                    drawingCanvas.drawBitmap(bitmap, null, dstRect, paint)
                                }

                                // Unlock the canvas
                                surfaceHolder.unlockCanvasAndPost(drawingCanvas)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("wallpaperservice", e.toString())
                }
                Thread.sleep(1000)
            }


        }

        private fun calculateDestinationRect(bitmap: Bitmap, canvasWidth: Int, canvasHeight: Int): Rect {
            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height

            val scaleFactorX = canvasWidth.toFloat() / bitmapWidth.toFloat()
            val scaleFactorY = canvasHeight.toFloat() / bitmapHeight.toFloat()

            val scaleFactor = maxOf(scaleFactorX, scaleFactorY)

            val newWidth = (bitmapWidth * scaleFactor).toInt()
            val newHeight = (bitmapHeight * scaleFactor).toInt()

            val dstRect = Rect(0, 0, newWidth, newHeight)
            dstRect.offset(
                (canvasWidth - newWidth) / 2,
                (canvasHeight - newHeight) / 2
            )

            return dstRect
        }
    }
}
