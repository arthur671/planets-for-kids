package planets;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.ui.Picture;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The big buttons and words on top of the picture. Everything is painted with Java2D at screen size. */
final class Hud {

    private record Button(float x, float y, float w, float h, Runnable action) {
        boolean contains(float px, float py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }

    private static final float[] ICON_SIZE = { 1f, 0.55f, 0.7f, 0.74f, 0.62f, 1f, 0.72f, 0.84f, 0.82f };
    private static final Color PANEL = new Color(10, 18, 48, 150);
    private static final Color EDGE = new Color(255, 255, 255, 70);

    private final Main app;
    private final Node layer = new Node("hud");
    private final List<Button> buttons = new ArrayList<>();
    private final Picture[] labels = new Picture[9];
    private final String family;
    private Picture toast;
    private float toastTime;
    private int w, h;
    private float ui;

    Hud(Main app, Node guiNode) {
        this.app = app;
        guiNode.attachChild(layer);
        List<String> have = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames());
        String pick = Font.SANS_SERIF;
        for (String f : new String[] { "Fredoka", "Baloo 2", "Nunito", "Comfortaa", "Ubuntu", "DejaVu Sans" }) {
            if (have.contains(f)) {
                pick = f;
                break;
            }
        }
        family = pick;
    }

    // ------------------------------------------------------------------ layout

    void rebuild() {
        layer.detachAllChildren();
        buttons.clear();
        toast = null;
        Camera cam = app.getCamera();
        w = cam.getWidth();
        h = cam.getHeight();
        ui = Math.max(0.55f, Math.min(2.4f, Math.min(h / 900f, w / 1500f)));
        planetBar();
        sideButtons();
        infoCard();
        nameTags();
    }

    private void planetBar() {
        int cell = Math.round(100 * ui), cellH = Math.round(122 * ui), pad = Math.round(12 * ui);
        int barW = cell * 9 + pad * 2, barH = cellH + pad;
        float x0 = (w - barW) / 2f, y0 = 10 * ui;

        BufferedImage bg = image(barW, barH);
        Graphics2D g = pen(bg);
        panel(g, barW, barH, 28 * ui);
        g.dispose();
        put(bg, x0, y0, 1);

        for (int i = 0; i < 9; i++) {
            BufferedImage img = image(cell, cellH);
            g = pen(img);
            float cx = cell / 2f, cy = 50 * ui, r = 40 * ui * ICON_SIZE[i];
            if (i == app.selected) {
                g.setColor(new Color(255, 255, 255, 40));
                g.fill(new RoundRectangle2D.Float(2, 2, cell - 4, cellH - 4, 22 * ui, 22 * ui));
                g.setColor(new Color(255, 220, 90));
                g.setStroke(new BasicStroke(3.5f * ui));
                g.draw(new RoundRectangle2D.Float(3, 3, cell - 6, cellH - 6, 22 * ui, 22 * ui));
            }
            planetIcon(g, i, cx, cy, r);
            g.setFont(new Font(family, Font.BOLD, Math.round(17 * ui)));
            text(g, Main.NAMES[i][app.lang], cx, cellH - 11 * ui, i == app.selected ? new Color(255, 230, 130)
                    : Color.WHITE);
            g.dispose();
            float bx = x0 + pad + i * cell, by = y0 + pad / 2f;
            put(img, bx, by, 2);
            int index = i;
            buttons.add(new Button(bx, by, cell, cellH, () -> app.pick(index)));
        }
    }

    private void sideButtons() {
        int b = Math.round(78 * ui);
        float gap = 12 * ui, x = w - b - 16 * ui, y = h - 16 * ui - b;
        String[] kinds = { "turtle", app.paused ? "play" : "pause", "rabbit", "rocket", "comet", "home", "lang" };
        Runnable[] actions = { () -> app.changeSpeed(-1), app::togglePause, () -> app.changeSpeed(1),
                app::toggleTour, app::spawnComet, app::home, app::toggleLang };
        for (int i = 0; i < kinds.length; i++) {
            boolean on = (kinds[i].equals("rocket") && app.tour) || kinds[i].equals("play");
            BufferedImage img = image(b, b);
            Graphics2D g = pen(img);
            float s = 3 * ui;
            g.setColor(on ? new Color(40, 70, 150, 215) : new Color(14, 24, 62, 190));
            g.fill(new Ellipse2D.Float(s, s, b - 2 * s, b - 2 * s));
            g.setColor(on ? new Color(255, 220, 90) : new Color(255, 255, 255, 130));
            g.setStroke(new BasicStroke(s));
            g.draw(new Ellipse2D.Float(s, s, b - 2 * s, b - 2 * s));
            g.translate(b / 2.0, b / 2.0);
            g.scale(b / 100.0, b / 100.0);
            icon(g, kinds[i]);
            g.dispose();
            put(img, x, y, 2);
            buttons.add(new Button(x, y, b, b, actions[i]));
            y -= b + gap;
        }
    }

    private void infoCard() {
        int sel = app.selected;
        String title = sel < 0 ? Main.HOME_TITLE[app.lang] : Main.NAMES[sel][app.lang];
        String[] facts = sel < 0 ? Main.HOME_FACTS[app.lang] : Main.FACTS[sel][app.lang];
        Color accent = new Color(sel < 0 ? 0xffffff : Main.ACCENT[sel]);

        int pad = Math.round(20 * ui);
        int cw = Math.round(Math.min(470 * ui, w - 140 * ui));
        Font titleFont = new Font(family, Font.BOLD, Math.round((sel < 0 ? 36 : 48) * ui));
        Font factFont = new Font(family, Font.PLAIN, Math.round(22 * ui));
        Graphics2D probe = pen(image(4, 4));
        FontMetrics tm = probe.getFontMetrics(titleFont), fm = probe.getFontMetrics(factFont);
        probe.dispose();

        int bullet = Math.round(26 * ui);
        List<List<String>> wrapped = new ArrayList<>();
        int lines = 0;
        for (String f : facts) {
            List<String> ls = wrap(f, fm, cw - 2 * pad - bullet);
            wrapped.add(ls);
            lines += ls.size();
        }
        int lineH = Math.round(29 * ui), factGap = Math.round(9 * ui);
        int ch = pad + tm.getHeight() + Math.round(6 * ui) + lines * lineH + facts.length * factGap + pad / 2;

        BufferedImage img = image(cw, ch);
        Graphics2D g = pen(img);
        panel(g, cw, ch, 24 * ui);
        g.setFont(titleFont);
        float y = pad + tm.getAscent();
        g.setColor(new Color(0, 0, 0, 170));
        g.drawString(title, pad + 2 * ui, y + 2 * ui);
        g.setColor(accent);
        g.drawString(title, pad, y);
        y += tm.getDescent() + Math.round(6 * ui);
        g.setFont(factFont);
        for (List<String> ls : wrapped) {
            y += factGap;
            g.setColor(accent);
            star(g, pad + 9 * ui, y + lineH * 0.5f, 8 * ui);
            g.setColor(Color.WHITE);
            for (String line : ls) {
                y += lineH;
                g.drawString(line, pad + bullet, y - lineH * 0.27f);
            }
        }
        g.dispose();
        put(img, 16 * ui, h - 16 * ui - ch, 2);
    }

    /** Small name signs that float above each planet. */
    private void nameTags() {
        Font font = new Font(family, Font.BOLD, Math.round(16 * ui));
        Graphics2D probe = pen(image(4, 4));
        FontMetrics fm = probe.getFontMetrics(font);
        probe.dispose();
        for (int i = 0; i < 9; i++) {
            String name = Main.NAMES[i][app.lang];
            int tw = fm.stringWidth(name) + 10, th = fm.getHeight() + 6;
            BufferedImage img = image(tw, th);
            Graphics2D g = pen(img);
            g.setFont(font);
            text(g, name, tw / 2f, fm.getAscent() + 2, new Color(Main.ACCENT[i]));
            g.dispose();
            labels[i] = put(img, 0, 0, 0);
        }
    }

    void toast(String message) {
        if (toast != null) {
            toast.removeFromParent();
        }
        Font font = new Font(family, Font.BOLD, Math.round(32 * ui));
        Graphics2D probe = pen(image(4, 4));
        FontMetrics fm = probe.getFontMetrics(font);
        probe.dispose();
        int tw = fm.stringWidth(message) + Math.round(56 * ui), th = fm.getHeight() + Math.round(22 * ui);
        BufferedImage img = image(tw, th);
        Graphics2D g = pen(img);
        g.setColor(new Color(14, 24, 62, 215));
        g.fill(new RoundRectangle2D.Float(1, 1, tw - 2, th - 2, th, th));
        g.setColor(new Color(255, 220, 90));
        g.setStroke(new BasicStroke(2.5f * ui));
        g.draw(new RoundRectangle2D.Float(2, 2, tw - 4, th - 4, th, th));
        g.setFont(font);
        text(g, message, tw / 2f, Math.round(11 * ui) + fm.getAscent(), Color.WHITE);
        g.dispose();
        toast = put(img, (w - tw) / 2f, h - th - 24 * ui, 4);
        toastTime = 1.8f;
    }

    // ------------------------------------------------------------------ every frame, clicks

    void update(float tpf) {
        Camera cam = app.getCamera();
        for (int i = 0; i < 9; i++) {
            Picture tag = labels[i];
            Main.Body b = app.bodies.get(i);
            Vector3f top = cam.getUp().mult(b.radius * 1.12f).addLocal(b.pos);
            Vector3f s = cam.getScreenCoordinates(top);
            // not on top of the buttons, the planet bar or the info card
            boolean free = s.x < w - 110 * ui && s.y > 150 * ui && !(s.x < 510 * ui && s.y > h - 280 * ui);
            boolean show = free && i != app.selected && s.z > 0 && s.z < 1
                    && cam.getDirection().dot(top.subtract(cam.getLocation())) > 0;
            tag.setCullHint(show ? Spatial.CullHint.Never : Spatial.CullHint.Always);
            if (show) {
                float tw = tag.getLocalScale().x;
                tag.setPosition(Math.round(s.x - tw / 2), Math.round(s.y + 4 * ui));
            }
        }
        if (toast != null) {
            toastTime -= tpf;
            if (toastTime <= 0) {
                toast.removeFromParent();
                toast = null;
            } else {
                toast.getMaterial().setColor("Color", new ColorRGBA(1, 1, 1, Math.min(1, toastTime * 2.5f)));
            }
        }
    }

    boolean hit(float x, float y) {
        for (Button b : buttons) {
            if (b.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    boolean click(float x, float y) {
        for (Button b : buttons) {
            if (b.contains(x, y)) {
                b.action.run();
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ painting helpers

    private static BufferedImage image(int w, int h) {
        return new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
    }

    private static Graphics2D pen(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g;
    }

    private Picture put(BufferedImage img, float x, float y, float z) {
        Picture p = new Picture("hud");
        p.setTexture(app.getAssetManager(), Tex.of(img).toTexture(false), true);
        p.setWidth(img.getWidth());
        p.setHeight(img.getHeight());
        p.setPosition(Math.round(x), Math.round(y));
        p.move(0, 0, z);
        layer.attachChild(p);
        return p;
    }

    private static void panel(Graphics2D g, int w, int h, float round) {
        g.setColor(PANEL);
        g.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, round, round));
        g.setColor(EDGE);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, round, round));
    }

    /** Centred text with a dark shadow so it can be read on any background. */
    private static void text(Graphics2D g, String s, float cx, float baseline, Color color) {
        float x = cx - g.getFontMetrics().stringWidth(s) / 2f;
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString(s, x + 1.5f, baseline + 1.5f);
        g.drawString(s, x - 1f, baseline + 1f);
        g.setColor(color);
        g.drawString(s, x, baseline);
    }

    private static List<String> wrap(String s, FontMetrics fm, int width) {
        List<String> out = new ArrayList<>();
        String line = "";
        for (String word : s.split(" ")) {
            String t = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && fm.stringWidth(t) > width) {
                out.add(line);
                line = word;
            } else {
                line = t;
            }
        }
        out.add(line);
        return out;
    }

    private static Path2D starShape(float cx, float cy, float r) {
        Path2D p = new Path2D.Float();
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5, rr = i % 2 == 0 ? r : r * 0.45;
            double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        p.closePath();
        return p;
    }

    private static void star(Graphics2D g, float cx, float cy, float r) {
        g.fill(starShape(cx, cy, r));
    }

    /** A little shaded ball painted from the same picture the real 3D planet wears. */
    private void planetIcon(Graphics2D g, int index, float cx, float cy, float r) {
        Tex tex = app.bodies.get(index).tex;
        boolean sun = index == 0, saturn = index == 6;
        AffineTransform old = g.getTransform();
        if (sun) {
            for (int k = 5; k >= 1; k--) {
                g.setColor(new Color(255, 170, 40, 18));
                float rr = r * (1 + 0.07f * k);
                g.fill(new Ellipse2D.Float(cx - rr, cy - rr, rr * 2, rr * 2));
            }
            r *= 0.92f;
        }
        Rectangle2D ringBox = new Rectangle2D.Float(-r * 1.3f, -r * 0.36f, r * 2.6f, r * 0.72f);
        if (saturn) {
            r *= 0.62f / 0.72f;
            g.translate(cx, cy);
            g.rotate(-0.35);
            g.setColor(new Color(217, 198, 154, 235));
            g.setStroke(new BasicStroke(r * 0.26f));
            g.draw(new Arc2D.Double(ringBox, 0, 180, Arc2D.OPEN));
            g.setTransform(old);
        }
        int n = (int) Math.ceil(r * 2) + 2;
        BufferedImage ball = image(n, n);
        float c = n / 2f;
        for (int py = 0; py < n; py++) {
            for (int px = 0; px < n; px++) {
                float nx = (px + 0.5f - c) / r, ny = (py + 0.5f - c) / r;
                float d2 = nx * nx + ny * ny;
                if (d2 > 1.05f) {
                    continue;
                }
                float d = (float) Math.sqrt(d2);
                float nz = (float) Math.sqrt(Math.max(0, 1 - d2));
                float u = (float) (Math.atan2(nx, nz) / (2 * Math.PI)) + 0.6f;
                float v = 0.5f + (float) (Math.asin(Math.max(-1, Math.min(1, -ny))) / Math.PI);
                int col = tex.sample(u, v);
                float light = sun ? 1.05f : 0.34f + 0.78f * Math.max(0, -0.5f * nx - 0.45f * ny + 0.74f * nz);
                int a = (int) (255 * Textures.clamp01((1 - d) * r + 0.5f));
                ball.setRGB(px, py, a << 24 | (Textures.scale(col, light) & 0xffffff));
            }
        }
        g.drawImage(ball, Math.round(cx - c), Math.round(cy - c), null);
        if (saturn) {
            g.translate(cx, cy);
            g.rotate(-0.35);
            g.draw(new Arc2D.Double(ringBox, 180, 180, Arc2D.OPEN));
            g.setTransform(old);
        }
    }

    /** Button pictures, drawn in a square from -50 to 50. */
    private void icon(Graphics2D g, String kind) {
        switch (kind) {
            case "pause" -> {
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Float(-19, -22, 14, 44, 6, 6));
                g.fill(new RoundRectangle2D.Float(5, -22, 14, 44, 6, 6));
            }
            case "play" -> {
                Path2D p = new Path2D.Float();
                p.moveTo(-13, -24);
                p.lineTo(25, 0);
                p.lineTo(-13, 24);
                p.closePath();
                g.setColor(new Color(140, 255, 150));
                g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(p);
                g.fill(p);
            }
            case "turtle" -> {
                Color dark = new Color(0x2e8b3d), light = new Color(0x8fd66a);
                g.setColor(light);
                g.fill(new RoundRectangle2D.Float(-23, 6, 11, 17, 8, 8));
                g.fill(new RoundRectangle2D.Float(7, 6, 11, 17, 8, 8));
                Path2D tail = new Path2D.Float();
                tail.moveTo(-26, 4);
                tail.lineTo(-37, 10);
                tail.lineTo(-26, 11);
                tail.closePath();
                g.fill(tail);
                g.fill(new Ellipse2D.Float(15, -11, 21, 19));
                g.setColor(dark);
                g.fill(new Arc2D.Float(-31, -24, 54, 60, 0, 180, Arc2D.CHORD));
                g.setColor(new Color(0x57b85a));
                g.fill(new Ellipse2D.Float(-12, -18, 15, 12));
                g.fill(new Ellipse2D.Float(-24, -8, 11, 9));
                g.fill(new Ellipse2D.Float(6, -9, 11, 9));
                g.setColor(light);
                g.fill(new RoundRectangle2D.Float(-31, 3, 54, 7, 7, 7));
                g.setColor(Color.BLACK);
                g.fill(new Ellipse2D.Float(27, -6, 4.5f, 4.5f));
            }
            case "rabbit" -> {
                Color fur = new Color(0xf7f3ee), pink = new Color(0xffa6c0);
                g.setColor(fur);
                g.fill(new Ellipse2D.Float(-33, 0, 12, 12));
                g.fill(new Ellipse2D.Float(-27, -8, 40, 30));
                g.fill(new Ellipse2D.Float(-20, 15, 20, 9));
                g.fill(new Ellipse2D.Float(4, 15, 15, 8));
                AffineTransform old = g.getTransform();
                g.rotate(-0.25, 12, -14);
                g.fill(new Ellipse2D.Float(4, -42, 9, 30));
                g.setColor(pink);
                g.fill(new Ellipse2D.Float(6.5f, -38, 4, 21));
                g.setTransform(old);
                g.rotate(0.15, 18, -14);
                g.setColor(fur);
                g.fill(new Ellipse2D.Float(14, -43, 9, 30));
                g.setColor(pink);
                g.fill(new Ellipse2D.Float(16.5f, -39, 4, 21));
                g.setTransform(old);
                g.setColor(fur);
                g.fill(new Ellipse2D.Float(4, -20, 26, 23));
                g.setColor(Color.BLACK);
                g.fill(new Ellipse2D.Float(19, -13, 4.5f, 4.5f));
                g.setColor(pink);
                g.fill(new Ellipse2D.Float(27, -8, 4, 4));
            }
            case "rocket" -> {
                g.rotate(Math.PI / 4);
                Path2D flame = new Path2D.Float();
                flame.moveTo(-8, 22);
                flame.lineTo(0, 46);
                flame.lineTo(8, 22);
                flame.closePath();
                g.setColor(new Color(0xff8a1e));
                g.fill(flame);
                Path2D inner = new Path2D.Float();
                inner.moveTo(-4, 22);
                inner.lineTo(0, 35);
                inner.lineTo(4, 22);
                inner.closePath();
                g.setColor(new Color(0xffe066));
                g.fill(inner);
                g.setColor(new Color(0xef3b33));
                for (int side = -1; side <= 1; side += 2) {
                    Path2D fin = new Path2D.Float();
                    fin.moveTo(side * 9, 4);
                    fin.lineTo(side * 25, 27);
                    fin.lineTo(side * 9, 21);
                    fin.closePath();
                    g.fill(fin);
                }
                Ellipse2D hull = new Ellipse2D.Float(-13, -36, 26, 60);
                g.setColor(Color.WHITE);
                g.fill(hull);
                Area nose = new Area(hull);
                nose.intersect(new Area(new Rectangle2D.Float(-20, -40, 40, 21)));
                g.setColor(new Color(0xef3b33));
                g.fill(nose);
                g.setColor(new Color(0x1a5fb4));
                g.fill(new Ellipse2D.Float(-8, -14, 16, 16));
                g.setColor(new Color(0x7fd4ff));
                g.fill(new Ellipse2D.Float(-5.5f, -11.5f, 11, 11));
            }
            case "comet" -> {
                for (int i = 0; i < 3; i++) {
                    float o = (i - 1) * 9;
                    g.setColor(new Color(150, 215, 255, i == 1 ? 230 : 150));
                    g.setStroke(new BasicStroke(i == 1 ? 6 : 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(new Line2D.Float(6 + o * 0.7f, -4 + o * 0.7f, -30 + o, 30 + o * 0.4f));
                }
                g.setColor(new Color(0xffe14d));
                star(g, 12, -12, 21);
            }
            case "home" -> {
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Float(-20, -6, 40, 32, 5, 5));
                Path2D roof = new Path2D.Float();
                roof.moveTo(-30, -3);
                roof.lineTo(0, -30);
                roof.lineTo(30, -3);
                roof.closePath();
                g.setColor(new Color(0xef3b33));
                g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(roof);
                g.fill(roof);
                g.setColor(new Color(0x1a5fb4));
                g.fill(new RoundRectangle2D.Float(-7, 7, 14, 19, 4, 4));
            }
            default -> {
                g.setFont(new Font(family, Font.BOLD, 34));
                text(g, app.lang == 1 ? "DE" : "EN", 0, 12, Color.WHITE);
            }
        }
    }
}
