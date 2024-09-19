package com.kroy.webrtc.utils

import android.graphics.Bitmap
import android.util.Log
import org.webrtc.VideoFrame
import org.webrtc.VideoSink

object ImageConverter  {


     fun convertFrameToBitmap(frame: VideoFrame): Bitmap {
        // You can use the VideoFrame's buffer to create a Bitmap
        val buffer = frame.buffer.toI420()
        val width = buffer.width
        val height = buffer.height

        // Convert the buffer to a byte array and then to a Bitmap
        // This part requires more detailed handling depending on the format
        // You might need YUV to RGB conversion, depending on the format.
        // For demonstration purposes, assume you have a conversion method.
        return createBitmapFromBuffer(buffer, width, height)
    }
    private fun createBitmapFromBuffer(buffer: VideoFrame.I420Buffer, width: Int, height: Int): Bitmap {
        // Create an empty bitmap with the same size as the video frame
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Allocate a buffer to hold the ARGB pixel data
        val argbBuffer = IntArray(width * height)

        // Convert YUV to ARGB
        yuvToRgb(buffer, argbBuffer, width, height)

        // Set the pixel data to the bitmap
        bitmap.setPixels(argbBuffer, 0, width, 0, 0, width, height)

        // Release the buffer after use
        buffer.release()

        return bitmap
    }

    private fun yuvToRgb(buffer: VideoFrame.I420Buffer, argbBuffer: IntArray, width: Int, height: Int) {
        val yPlane = buffer.dataY
        val uPlane = buffer.dataU
        val vPlane = buffer.dataV

        val strideY = buffer.strideY
        val strideU = buffer.strideU
        val strideV = buffer.strideV

        for (y in 0 until height) {
            for (x in 0 until width) {
                val yIndex = y * strideY + x
                val uIndex = (y / 2) * strideU + (x / 2)
                val vIndex = (y / 2) * strideV + (x / 2)

                val yValue = yPlane.get(yIndex).toInt() and 0xFF
                val uValue = uPlane.get(uIndex).toInt() and 0xFF
                val vValue = vPlane.get(vIndex).toInt() and 0xFF

                // YUV to RGB conversion formula
                val r = (yValue + 1.370705 * (vValue - 128)).toInt().coerceIn(0, 255)
                val g = (yValue - 0.337633 * (uValue - 128) - 0.698001 * (vValue - 128)).toInt().coerceIn(0, 255)
                val b = (yValue + 1.732446 * (uValue - 128)).toInt().coerceIn(0, 255)

                // Combine the RGB values into ARGB format
                argbBuffer[y * width + x] = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            }
        }
    }


}
