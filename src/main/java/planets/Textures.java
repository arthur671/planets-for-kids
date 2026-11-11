package planets;

import java.util.Random;
import java.util.stream.IntStream;

/** Paints every planet, the Sun and the starry sky from noise, so no image files are needed. */
final class Textures {
    private Textures() {
    }

    private static final float PI = (float) Math.PI;

    /** Colour of one spot on a ball. (x,y,z) is the direction, z points to the north pole. */
    interface Shade {
        int at(float x, float y, float z, float lon, float lat);
    }

    static Tex sphere(int w, int h, Shade s) {
        Tex t = new Tex(w, h);
        IntStream.range(0, h).parallel().forEach(j -> {
            float lat = PI * ((j + 0.5f) / h - 0.5f);
            float cl = (float) Math.cos(lat), sl = (float) Math.sin(lat);
            for (int i = 0; i < w; i++) {
                float lon = 2 * PI * (i + 0.5f) / w;
                t.px[j * w + i] = s.at(cl * (float) Math.cos(lon), cl * (float) Math.sin(lon), sl, lon, lat);
            }
        });
        return t;
    }

    // ---------------------------------------------------------------- noise

    private static float hash(int x, int y, int z, int s) {
        int n = x * 0x27d4eb2d + y * 0x165667b1 + z * 0x9e3779b1 + s * 0x85ebca6b;
        n ^= n >>> 15;
        n *= 0x2c1b3c6d;
        n ^= n >>> 12;
        n *= 0x297a2d39;
        n ^= n >>> 15;
        return (n >>> 8) * (1f / 16777216f);
    }

    static float n3(float x, float y, float z, int seed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y), zi = (int) Math.floor(z);
        float fx = x - xi, fy = y - yi, fz = z - zi;
        float u = fx * fx * (3 - 2 * fx), v = fy * fy * (3 - 2 * fy), w = fz * fz * (3 - 2 * fz);
        float a = lerp(hash(xi, yi, zi, seed), hash(xi + 1, yi, zi, seed), u);
        float b = lerp(hash(xi, yi + 1, zi, seed), hash(xi + 1, yi + 1, zi, seed), u);
        float c = lerp(hash(xi, yi, zi + 1, seed), hash(xi + 1, yi, zi + 1, seed), u);
        float d = lerp(hash(xi, yi + 1, zi + 1, seed), hash(xi + 1, yi + 1, zi + 1, seed), u);
        return lerp(lerp(a, b, v), lerp(c, d, v), w);
    }

    static float fbm(float x, float y, float z, int octaves, int seed) {
        float amp = 0.5f, sum = 0, norm = 0;
        for (int o = 0; o < octaves; o++) {
            sum += amp * n3(x, y, z, seed + o * 131);
            norm += amp;
            x *= 2.03f;
            y *= 2.03f;
            z *= 2.03f;
            amp *= 0.5f;
        }
        return sum / norm;
    }

    // --------------------------------------------------------------- colour

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    static float clamp01(float v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    static float smooth(float a, float b, float x) {
        float t = clamp01((x - a) / (b - a));
        return t * t * (3 - 2 * t);
    }

    static int mix(int a, int b, float t) {
        t = clamp01(t);
        int r = (int) lerp((a >> 16) & 255, (b >> 16) & 255, t);
        int g = (int) lerp((a >> 8) & 255, (b >> 8) & 255, t);
        int bl = (int) lerp(a & 255, b & 255, t);
        return 0xff000000 | r << 16 | g << 8 | bl;
    }

    static int scale(int c, float k) {
        int r = Math.min(255, (int) (((c >> 16) & 255) * k));
        int g = Math.min(255, (int) (((c >> 8) & 255) * k));
        int b = Math.min(255, (int) ((c & 255) * k));
        return (c & 0xff000000) | r << 16 | g << 8 | b;
    }

    /** Picks a colour from a row of evenly spaced colours. */
    static int ramp(float t, int... colors) {
        float f = clamp01(t) * (colors.length - 1);
        int i = Math.min((int) f, colors.length - 2);
        return mix(colors[i], colors[i + 1], f - i);
    }

    // -------------------------------------------------------------- planets

    private static int rocky(float x, float y, float z, int seed, int dark, int mid, int light) {
        float a = fbm(x * 3, y * 3, z * 3, 6, seed);
        float c = n3(x * 13, y * 13, z * 13, seed + 7);
        float c2 = n3(x * 29, y * 29, z * 29, seed + 9);
        int col = ramp((a - 0.25f) * 2f, dark, mid, light);
        float crater = Math.max(smooth(0.68f, 0.78f, c), smooth(0.74f, 0.82f, c2));
        float rim = Math.max(smooth(0.60f, 0.68f, c), smooth(0.68f, 0.74f, c2)) * (1 - crater);
        return scale(col, 1 - 0.33f * crater + 0.14f * rim);
    }

    private static int banded(float x, float y, float z, int seed, float freq, float turbAmt, float streakAmt,
            int... colors) {
        float turb = fbm(x * 3, y * 3, z * 3, 4, seed) - 0.5f;
        float zz = z + turb * turbAmt;
        float band = 0.5f + 0.5f * (float) Math.sin(zz * freq + 3 * n3(0.5f, 0.5f, zz * 6, seed + 1));
        float streak = fbm(x * 4, y * 4, zz * 45, 4, seed + 2) - 0.5f;
        return ramp(band + streak * streakAmt, colors);
    }

    static Tex sun() {
        return sphere(1024, 512, (x, y, z, lon, lat) -> {
            float a = fbm(x * 5, y * 5, z * 5, 5, 31);
            float b = fbm(x * 14, y * 14, z * 14, 4, 37);
            return ramp((a * 0.6f + b * 0.4f - 0.3f) * 2.4f, 0xe85d00, 0xff9a00, 0xffc933, 0xfff0a0);
        });
    }

    static Tex mercury() {
        return sphere(1024, 512, (x, y, z, lon, lat) -> rocky(x, y, z, 101, 0x57504a, 0x8d857c, 0xbdb5aa));
    }

    static Tex moon() {
        return sphere(512, 256, (x, y, z, lon, lat) -> rocky(x, y, z, 202, 0x77777a, 0xa9a9ac, 0xdcdcdc));
    }

    static Tex venus() {
        return sphere(1024, 512, (x, y, z, lon, lat) -> {
            float w1 = fbm(x * 2, y * 2, z * 2, 4, 1), w2 = fbm(x * 2 + 5, y * 2 + 5, z * 2 + 5, 4, 2);
            float t = fbm(x * 2.5f + w1 * 1.5f, y * 2.5f + w2 * 1.5f, z * 7f + w1, 5, 3);
            return ramp((t - 0.5f) * 2.4f + 0.5f, 0xb9853f, 0xdcae62, 0xf2d999, 0xfbeecb);
        });
    }

    static Tex earth() {
        return sphere(2048, 1024, (x, y, z, lon, lat) -> {
            float h = fbm(x * 1.8f + 3, y * 1.8f, z * 1.8f, 7, 11);
            float alat = Math.abs(lat) / (PI / 2);
            float ice = alat + (fbm(x * 6, y * 6, z * 6, 3, 5) - 0.5f) * 0.25f;
            if (ice > 0.87f) {
                return 0xfff4f8ff;
            }
            int col;
            if (h < 0.53f) {
                col = ramp((0.53f - h) / 0.2f, 0x3a9be0, 0x1a5fb4, 0x0c2f78);
            } else {
                float e = (h - 0.53f) / 0.2f;
                col = ramp(e, 0x4fa64a, 0x2e7d32, 0x6b8f4a, 0x8c7b62, 0xb5ab9c);
                float dry = fbm(x * 3 + 9, y * 3, z * 3, 4, 23);
                col = mix(col, 0xd8bd75, smooth(0.5f, 0.62f, dry) * (1 - smooth(0.25f, 0.5f, alat)));
            }
            return mix(col, 0xe6eef2, smooth(0.76f, 0.87f, ice));
        });
    }

    /** White clouds with see-through gaps. */
    static Tex clouds() {
        return sphere(1024, 512, (x, y, z, lon, lat) -> {
            float w1 = fbm(x * 2, y * 2, z * 2, 3, 61) - 0.5f;
            float c = fbm(x * 3.5f + w1 * 2, y * 3.5f + w1 * 2, z * 5f, 6, 67);
            int a = (int) (235 * smooth(0.5f, 0.72f, c));
            return a << 24 | 0xffffff;
        });
    }

    static Tex mars() {
        return sphere(1024, 512, (x, y, z, lon, lat) -> {
            float a = fbm(x * 2.5f, y * 2.5f, z * 2.5f, 6, 41);
            int col = ramp((a - 0.25f) * 2f, 0x6e2c14, 0xb5522a, 0xdb8b55, 0xe9a873);
            float dark = fbm(x * 1.5f + 4, y * 1.5f, z * 1.5f, 4, 43);
            col = mix(col, 0x4f2415, 0.7f * smooth(0.55f, 0.68f, dark));
            float ice = Math.abs(lat) / (PI / 2) + (fbm(x * 7, y * 7, z * 7, 3, 47) - 0.5f) * 0.12f;
            return mix(col, 0xfaf3ee, smooth(0.86f, 0.9f, ice));
        });
    }

    static Tex jupiter() {
        return sphere(2048, 1024, (x, y, z, lon, lat) -> {
            int col = banded(x, y, z, 7, 24, 0.06f, 0.5f, 0x8f5b3a, 0xc48e63, 0xe9d3b0, 0xf6ecd9, 0xd3a273);
            // the Great Red Spot
            float dl = lon - 4.0f;
            float d = (dl / 0.30f) * (dl / 0.30f) + ((z + 0.38f) / 0.10f) * ((z + 0.38f) / 0.10f);
            col = mix(col, 0xf3dcc0, smooth(1.9f, 1.2f, d) * 0.6f);
            return mix(col, 0xb8432b, smooth(1.2f, 0.6f, d));
        });
    }

    static Tex saturn() {
        return sphere(1024, 512, (x, y, z, lon, lat) -> banded(x, y, z, 71, 18, 0.03f, 0.25f, 0xb89b63, 0xe3cf9a,
                0xf1e2b8, 0xd6bb7f));
    }

    static Tex uranus() {
        return sphere(512, 256, (x, y, z, lon, lat) -> banded(x, y, z, 81, 10, 0.02f, 0.08f, 0x8fd8dc, 0xa9e8ea,
                0xc4f2f2));
    }

    static Tex neptune() {
        return sphere(1024, 512, (x, y, z, lon, lat) -> {
            int col = banded(x, y, z, 91, 14, 0.05f, 0.3f, 0x1c36a0, 0x2b55cf, 0x4a7ff0, 0x2a4cc0);
            float wisp = fbm(x * 3, y * 3, z * 22, 5, 93);
            return mix(col, 0xdfeaff, 0.75f * smooth(0.66f, 0.76f, wisp));
        });
    }

    /** A strip across Saturn's rings, left = inner edge, right = outer edge. */
    static Tex ring() {
        Tex t = new Tex(512, 4);
        for (int i = 0; i < t.w; i++) {
            float r = i / (float) (t.w - 1);
            float dens = 0.45f + 0.55f * n3(r * 38, 0.5f, 0.5f, 5);
            dens *= smooth(0f, 0.04f, r) * smooth(1f, 0.96f, r);
            dens *= lerp(0.4f, 1f, smooth(0.12f, 0.2f, r));
            dens *= 1 - 0.92f * smooth(0.035f, 0.01f, Math.abs(r - 0.66f)); // the big gap
            int col = ramp(n3(r * 11, 2.5f, 0.5f, 6), 0xa8946c, 0xd9c69a, 0xefe2bf);
            int argb = (int) (235 * clamp01(dens)) << 24 | (col & 0xffffff);
            for (int j = 0; j < t.h; j++) {
                t.px[j * t.w + i] = argb;
            }
        }
        return t;
    }

    /** Soft white blob, bright in the middle. */
    static Tex glow(float power) {
        int n = 128;
        Tex t = new Tex(n, n);
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                float dx = (i + 0.5f) / n * 2 - 1, dy = (j + 0.5f) / n * 2 - 1;
                float d = clamp01((float) Math.sqrt(dx * dx + dy * dy));
                t.px[j * n + i] = (int) (255 * Math.pow(1 - d, power)) << 24 | 0xffffff;
            }
        }
        return t;
    }

    /** Glowing rim that peeks out behind a planet; the planet edge sits at 0.6 of the picture. */
    static Tex halo() {
        int n = 128;
        Tex t = new Tex(n, n);
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                float dx = (i + 0.5f) / n * 2 - 1, dy = (j + 0.5f) / n * 2 - 1;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = d < 0.6f ? 1 : (float) Math.pow(clamp01(1 - (d - 0.6f) / 0.4f), 2.6f);
                t.px[j * n + i] = (int) (255 * a) << 24 | 0xffffff;
            }
        }
        return t;
    }

    /** Deep space: faint coloured clouds and thousands of stars. */
    static Tex sky() {
        int w = 4096, h = 2048;
        Tex t = sphere(w, h, (x, y, z, lon, lat) -> {
            float n = fbm(x * 1.5f, y * 1.5f, z * 1.5f, 5, 51);
            float m = fbm(x * 2.5f + 7, y * 2.5f, z * 2.5f, 4, 57);
            float neb = smooth(0.5f, 0.8f, n);
            float milky = (float) Math.exp(-(y * y) / 0.05f) * smooth(0.35f, 0.7f, fbm(x * 4, y * 4, z * 4, 5, 59));
            float r = neb * (0.05f + 0.22f * m) + milky * 0.10f;
            float g = neb * 0.05f + milky * 0.09f;
            float b = neb * (0.22f - 0.12f * m) + milky * 0.13f + 0.012f;
            return 0xff000000 | (int) (255 * clamp01(r)) << 16 | (int) (255 * clamp01(g)) << 8
                    | (int) (255 * clamp01(b));
        });
        Random rnd = new Random(42);
        int[] tints = { 0xffffff, 0xffffff, 0xcfe0ff, 0xfff0c8, 0xffd6b0, 0xbcd4ff };
        for (int s = 0; s < 11000; s++) {
            double x = rnd.nextGaussian(), y = rnd.nextGaussian(), z = rnd.nextGaussian();
            double len = Math.sqrt(x * x + y * y + z * z);
            int i = (int) ((Math.atan2(y, x) / (2 * Math.PI) + 0.5) * w) % w;
            int j = Math.min(h - 1, (int) ((Math.asin(z / len) / Math.PI + 0.5) * h));
            float f = rnd.nextFloat();
            float bright = 0.3f + 0.7f * f * f * f;
            int tint = tints[rnd.nextInt(tints.length)];
            add(t, i, j, tint, bright);
            if (f > 0.8f) { // a big star
                float k = (f - 0.8f) * 3.5f;
                add(t, i + 1, j, tint, k);
                add(t, i - 1, j, tint, k);
                add(t, i, j + 1, tint, k);
                add(t, i, j - 1, tint, k);
            }
        }
        return t;
    }

    private static void add(Tex t, int i, int j, int tint, float k) {
        if (j < 0 || j >= t.h) {
            return;
        }
        int idx = j * t.w + Math.floorMod(i, t.w);
        int c = t.px[idx];
        int r = Math.min(255, ((c >> 16) & 255) + (int) (((tint >> 16) & 255) * k));
        int g = Math.min(255, ((c >> 8) & 255) + (int) (((tint >> 8) & 255) * k));
        int b = Math.min(255, (c & 255) + (int) ((tint & 255) * k));
        t.px[idx] = 0xff000000 | r << 16 | g << 8 | b;
    }
}
