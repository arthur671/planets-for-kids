package planets;

import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/** A picture held as ARGB pixels (row 0 is the top) that can become a jME texture. */
final class Tex {
    final int w, h;
    final int[] px;

    Tex(int w, int h) {
        this.w = w;
        this.h = h;
        this.px = new int[w * h];
    }

    static Tex of(BufferedImage img) {
        Tex t = new Tex(img.getWidth(), img.getHeight());
        img.getRGB(0, 0, t.w, t.h, t.px, 0, t.w);
        return t;
    }

    /** Nearest pixel at u (wraps around) and v (0 = top row). */
    int sample(float u, float v) {
        int x = Math.floorMod((int) (u * w), w);
        int y = Math.max(0, Math.min(h - 1, (int) (v * h)));
        return px[y * w + x];
    }

    /** @param smooth true for 3D surfaces (mipmaps, wraps sideways), false for crisp 1:1 screen pictures */
    Texture2D toTexture(boolean smooth) {
        ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
        byte[] row = new byte[w * 4];
        for (int y = h - 1; y >= 0; y--) { // jME wants the bottom row first
            int o = y * w;
            for (int x = 0; x < w; x++) {
                int c = px[o + x];
                row[x * 4] = (byte) (c >> 16);
                row[x * 4 + 1] = (byte) (c >> 8);
                row[x * 4 + 2] = (byte) c;
                row[x * 4 + 3] = (byte) (c >>> 24);
            }
            buf.put(row);
        }
        buf.flip();
        Texture2D t = new Texture2D(new Image(Image.Format.RGBA8, w, h, buf, null, ColorSpace.sRGB));
        t.setMagFilter(Texture.MagFilter.Bilinear);
        if (smooth) {
            t.setMinFilter(Texture.MinFilter.Trilinear);
            t.setAnisotropicFilter(8);
            t.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
        } else {
            t.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        }
        return t;
    }
}
