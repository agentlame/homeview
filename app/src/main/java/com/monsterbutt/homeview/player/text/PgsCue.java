package com.monsterbutt.homeview.player.text;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.exoplayer2.text.Cue;

public class PgsCue extends Cue {

    final private static byte PIXEL_ASHIFT = 24;
    final private static byte PIXEL_RSHIFT = 16;
    final private static byte PIXEL_GSHIFT = 8;
    //final private static byte PIXEL_BSHIFT = 0;

    final private long start_display_time;
    final private int x;
    final private int y;
    final private int height;
    final private int width;
    final private Bitmap bitmap;
    final private boolean isForced;

    public PgsCue(PgsSubtitle.AVSubtitleRect avSubtitleRect) {

        super("");
        this.start_display_time = avSubtitleRect.start_display_time;
        this.x = avSubtitleRect.x;
        this.y = avSubtitleRect.y;
        this.height = avSubtitleRect.h;
        this.width = avSubtitleRect.w;
        this.isForced = 0 < (avSubtitleRect.flags & PgsSubtitle.AVSubtitleRect.FLAG_FORCED);
        bitmap = buildBitmap(avSubtitleRect, false);
    }
    public long getStartDisplayTime() { return start_display_time; }
    public boolean isForcedSubtitle() { return isForced; }

    private Bitmap buildBitmap(PgsSubtitle.AVSubtitleRect  avSubtitleRect, boolean mergeAlpha) {

        final int height = avSubtitleRect.h;
        final int width = avSubtitleRect.w;

        int[] palette = new int[256];
        for(int i = 0; i < avSubtitleRect.nb_colors; ++i) {

            int palettePixel = avSubtitleRect.pict.clut[i];
            palette[i] = build_rgba((palettePixel >> PIXEL_ASHIFT) & 0xff
                    , (palettePixel >> PIXEL_RSHIFT)    & 0xff
                    , (palettePixel >> PIXEL_GSHIFT)    & 0xff
                    , (palettePixel)/*>> PIXEL_BSHIFT)& 0xff */
                    , mergeAlpha);
        }
        final byte[] data = avSubtitleRect.pict.data;
        final int lineSize = avSubtitleRect.pict.linesize;
        int[] argb = new int[height * width];
        for(int row = 0; row < height; ++row) {
            int rowStart = row * width;
            int dataStart = row * lineSize;
            for (int col = 0; col < width; ++col)
                argb[rowStart + col] = palette[(data[dataStart + col] & 0xFF)];
        }

        if (width == 0 || height == 0)
            return null;
        return Bitmap.createBitmap(argb, 0, width, width, height, Bitmap.Config.ARGB_8888);
    }

    static int build_rgba(int a, int r, int g, int b, boolean mergealpha)
    {
        if(mergealpha)
            return     a            << PIXEL_ASHIFT
                    | (r * a / 255) << PIXEL_RSHIFT
                    | (g * a / 255) << PIXEL_GSHIFT
                    | (b * a / 255) ;//<< PIXEL_BSHIFT;
        else
            return a << PIXEL_ASHIFT
                    | r << PIXEL_RSHIFT
                    | g << PIXEL_GSHIFT
                    | b ;//<< PIXEL_BSHIFT;
    }

    public void updateParams(int surfaceAnchorX, int surfaceAnchorY, int surfaceWidth, int surfaceHeight,
                             int sourceWidth, int sourceHeight, ImageView view) {

        if (bitmap != null) {

            int subAnchorX = x;
            int subAnchorY = y;
            int subScaleWidth = width;
            int subScaleHeight = height;

            // they should change together as we keep the aspect ratio
            if (surfaceHeight != sourceHeight || surfaceWidth != sourceWidth) {

                double scale;
                if (surfaceWidth != sourceWidth)
                    scale = (double) surfaceWidth / (double) sourceWidth;
                else
                    scale = (double) surfaceHeight / (double) sourceHeight;
                subScaleHeight = (int) (scale * subScaleHeight);
                subScaleWidth = (int) (scale * subScaleWidth);
            }
            if (surfaceAnchorX != 0)
                subAnchorX += surfaceAnchorX;
            if (subAnchorY != 0)
                subAnchorY += surfaceAnchorY;

            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = subScaleWidth;
            params.height = subScaleHeight;
            view.setY(subAnchorY);
            view.setX(subAnchorX);
            view.setLayoutParams(params);
            view.setImageBitmap(bitmap);
            view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            view.setVisibility(View.VISIBLE);
        }
        else {

            view.setImageBitmap(null);
            view.setVisibility(View.INVISIBLE);
        }
    }
}
