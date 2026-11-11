package planets;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Dome;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import jme3tools.optimize.GeometryBatchFactory;

/**
 * A solar system for small astronauts: click a planet and a little rocket flies you there.
 */
public class Main extends SimpleApplication implements RawInputListener {

    // ------------------------------------------------------------------ words (English, German)

    static final String[][] NAMES = {
            { "Sun", "Sonne" }, { "Mercury", "Merkur" }, { "Venus", "Venus" }, { "Earth", "Erde" },
            { "Mars", "Mars" }, { "Jupiter", "Jupiter" }, { "Saturn", "Saturn" }, { "Uranus", "Uranus" },
            { "Neptune", "Neptun" } };

    static final String[][][] FACTS = {
            { { "The Sun is a star!", "It is super hot and gives us light.", "A million Earths would fit inside." },
                    { "Die Sonne ist ein Stern!", "Sie ist superheiß und gibt uns Licht.",
                            "Eine Million Erden würden hineinpassen." } },
            { { "The smallest planet.", "It zooms around the Sun the fastest.", "Hot in the day, freezing at night." },
                    { "Der kleinste Planet.", "Er flitzt am schnellsten um die Sonne.",
                            "Am Tag heiß, in der Nacht eiskalt." } },
            { { "The hottest planet of all.", "It is wrapped in thick yellow clouds.", "It spins backwards!" },
                    { "Der heißeste Planet von allen.", "Sie ist in dicke gelbe Wolken gehüllt.",
                            "Sie dreht sich rückwärts!" } },
            { { "Our home! We live here.", "It is blue because of all the water.", "It has one moon." },
                    { "Unser Zuhause! Hier wohnen wir.", "Sie ist blau wegen dem vielen Wasser.",
                            "Sie hat einen Mond." } },
            { { "The red planet.", "It has the tallest volcano of all.", "Robots drive around on it!" },
                    { "Der rote Planet.", "Er hat den höchsten Vulkan von allen.", "Dort fahren Roboter herum!" } },
            { { "The biggest planet.", "It is made of gas. You can't stand on it!",
                    "The red spot is a giant storm." },
                    { "Der größte Planet.", "Er besteht aus Gas. Man kann nicht darauf stehen!",
                            "Der rote Fleck ist ein riesiger Sturm." } },
            { { "The planet with the beautiful rings.", "The rings are made of ice and rocks.",
                    "It is so light it could float on water!" },
                    { "Der Planet mit den schönen Ringen.", "Die Ringe sind aus Eis und Steinen.",
                            "Er ist so leicht, er könnte auf Wasser schwimmen!" } },
            { { "An icy cold planet.", "It rolls on its side like a ball.", "It looks light blue." },
                    { "Ein eiskalter Planet.", "Er rollt auf der Seite wie ein Ball.", "Er sieht hellblau aus." } },
            { { "The planet farthest from the Sun.", "It is dark, cold and very windy.",
                    "One trip around the Sun takes 165 years!" },
                    { "Der Planet, der am weitesten weg ist.", "Dunkel, kalt und sehr windig.",
                            "Eine Runde um die Sonne dauert 165 Jahre!" } } };

    static final String[] HOME_TITLE = { "Our Solar System", "Unser Sonnensystem" };
    static final String[][] HOME_FACTS = {
            { "Click a planet to fly there with the rocket!", "Drag to look around. Scroll to zoom." },
            { "Klick auf einen Planeten und flieg mit der Rakete hin!", "Ziehen zum Umschauen. Mausrad zum Zoomen." } };

    static final int[] ACCENT = { 0xffd54a, 0xd6cfc5, 0xf2d28a, 0x6cb8ff, 0xff8a5c, 0xf0c9a0, 0xf1dfa8, 0xa8f0f0,
            0x8fb0ff };

    static final float[] SPEEDS = { 0.25f, 0.5f, 1, 2, 4, 8, 16 };
    static final String[] SPEED_NAMES = { "¼", "½", "1", "2", "4", "8", "16" };

    // ------------------------------------------------------------------ the things in space

    static final class Body {
        int index;
        Tex tex;
        float radius, dist, period, spinPeriod;
        float angle;
        /** How far the rocket circles from the middle, and how far the camera sits. */
        float clear, viewDist;
        Node node, tilt, spin;
        final Vector3f pos = new Vector3f();
        final List<Moon> moons = new ArrayList<>();
    }

    static final class Moon {
        Node pivot;
        float period;
    }

    static final class Comet {
        Node node;
        ParticleEmitter tail;
        final Vector3f pos = new Vector3f(), vel = new Vector3f();
    }

    final List<Body> bodies = new ArrayList<>();
    private final List<Comet> comets = new ArrayList<>();
    private final Random rnd = new Random();
    private Node belt;
    private Geometry sky;
    private Texture glowTex, haloTex, moonTex;

    // state the buttons change
    int selected = -1; // -1 = looking at everything
    int lang;
    boolean paused, tour;
    int speedIdx = 2;
    private float tourTimer, cometTimer = 12;

    // camera
    private float yaw = 0.6f, pitch = 0.5f, pitchGoal = Float.NaN;
    private float dist = 230, distGoal = 230;
    private final Vector3f focus = new Vector3f(), offset = new Vector3f();
    private int focusKey = -1;

    // rocket
    private Node rocket;
    private ParticleEmitter flame;
    private int rocketBody = 3;
    private boolean traveling;
    private float travelT, travelDur, travelLift, orbitAngle;
    private final Vector3f rocketPos = new Vector3f(), travelStart = new Vector3f();
    private final Quaternion rocketRot = new Quaternion();
    private float rocketScale = 0.6f;

    // mouse
    private boolean dragging, pressOnHud;
    private float dragMoved;

    private Hud hud;
    boolean hudDirty;

    // only for taking a test picture
    private static String shotDir;
    private static int startSelected = -1, startLang = -1;
    private float shotTime;
    private int shotFrames = -1;

    public static void main(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--shot" -> shotDir = args[i + 1];
                case "--select" -> startSelected = Integer.parseInt(args[i + 1]);
                case "--lang" -> startLang = args[i + 1].startsWith("de") ? 1 : 0;
                default -> {
                }
            }
        }
        System.setProperty("java.awt.headless", "true"); // Java2D is only used to paint pictures
        Main app = new Main();
        AppSettings s = new AppSettings(true);
        s.setTitle("Planets");
        s.setResolution(1600, 900);
        s.setResizable(true);
        // Under WSL the Wayland window has no frame; the X11 one gets a normal title bar.
        s.setX11PlatformPreferred(true);
        s.setVSync(true);
        s.setSamples(4);
        s.setGammaCorrection(false);
        s.setAudioRenderer(null);
        app.setSettings(s);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.start();
    }

    public Main() {
        super(new AppState[0]);
    }

    // ------------------------------------------------------------------ building the scene

    @Override
    public void simpleInitApp() {
        lang = startLang >= 0 ? startLang : ("de".equals(System.getProperty("user.language")) ? 1 : 0);
        cam.setFrustumPerspective(45f, (float) cam.getWidth() / cam.getHeight(), 0.5f, 6000f);
        inputManager.setCursorVisible(true);
        inputManager.addRawInputListener(this);

        glowTex = Textures.glow(2.2f).toTexture(false);
        haloTex = Textures.halo().toTexture(false);
        moonTex = Textures.moon().toTexture(true);

        buildSky();
        buildLights();
        buildBodies();
        buildBelt();
        buildRocket();

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        int samples = settings.getSamples();
        if (samples > 1) {
            fpp.setNumSamples(samples);
        }
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloom.setBloomIntensity(2.2f);
        bloom.setBlurScale(2.5f);
        bloom.setDownSamplingFactor(2f);
        fpp.addFilter(bloom);
        viewPort.addProcessor(fpp);

        hud = new Hud(this, guiNode);
        hud.rebuild();

        if (startSelected >= 0) {
            select(startSelected);
        }
        if (shotDir != null) {
            ScreenshotAppState shot = new ScreenshotAppState(shotDir + "/", "shot");
            shot.setIsNumbered(false);
            stateManager.attach(shot);
        }
    }

    private Material lit(Texture tex, ColorRGBA color) {
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        if (tex != null) {
            m.setTexture("DiffuseMap", tex);
        }
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", color);
        m.setColor("Ambient", color);
        m.setColor("Specular", ColorRGBA.Black);
        m.setFloat("Shininess", 1f);
        return m;
    }

    private Material unlit(Texture tex, ColorRGBA color, BlendMode blend) {
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        if (tex != null) {
            m.setTexture("ColorMap", tex);
        }
        m.setColor("Color", color);
        if (blend != null) {
            m.getAdditionalRenderState().setBlendMode(blend);
            m.getAdditionalRenderState().setDepthWrite(false);
        }
        return m;
    }

    private static ColorRGBA rgb(int hex, float alpha) {
        return new ColorRGBA(((hex >> 16) & 255) / 255f, ((hex >> 8) & 255) / 255f, (hex & 255) / 255f, alpha);
    }

    /** A ball whose poles point up, so it can spin around the Y axis. */
    private Geometry ball(String name, float radius, int detail, Material mat) {
        Sphere mesh = new Sphere(detail, detail * 2, radius);
        mesh.setTextureMode(Sphere.TextureMode.Projected);
        Geometry g = new Geometry(name, mesh);
        g.rotate(-FastMath.HALF_PI, 0, 0);
        g.setMaterial(mat);
        return g;
    }

    /** A soft light picture that always faces the camera. */
    private Node glowCard(Texture tex, float size, ColorRGBA color) {
        Geometry g = new Geometry("glow", new Quad(size, size));
        g.setLocalTranslation(-size / 2, -size / 2, 0);
        g.setMaterial(unlit(tex, color, BlendMode.AlphaAdditive));
        g.setQueueBucket(Bucket.Transparent);
        Node n = new Node("glowCard");
        n.attachChild(g);
        n.addControl(new BillboardControl());
        return n;
    }

    private void buildSky() {
        Sphere mesh = new Sphere(24, 48, 1500f, false, true);
        mesh.setTextureMode(Sphere.TextureMode.Projected);
        sky = new Geometry("sky", mesh);
        sky.setMaterial(unlit(Textures.sky().toTexture(true), ColorRGBA.White, null));
        sky.setQueueBucket(Bucket.Sky);
        sky.setCullHint(Spatial.CullHint.Never);
        sky.rotate(0.4f, 0, 0.3f);
        rootNode.attachChild(sky);
    }

    private void buildLights() {
        PointLight sunLight = new PointLight();
        sunLight.setPosition(Vector3f.ZERO);
        sunLight.setColor(ColorRGBA.White.mult(1.15f));
        rootNode.addLight(sunLight);
        rootNode.addLight(new AmbientLight(new ColorRGBA(0.27f, 0.27f, 0.33f, 1f)));
    }

    private void buildBodies() {
        // The Sun shines by itself, so it is not lit and it glows.
        Body sun = new Body();
        sun.tex = Textures.sun();
        sun.radius = 7.8f;
        sun.spinPeriod = 45;
        Texture sunTex = sun.tex.toTexture(true);
        Material sunMat = unlit(sunTex, ColorRGBA.White, null);
        sunMat.setTexture("GlowMap", sunTex);
        finishBody(sun, sunMat, 0);
        sun.node.attachChild(glowCard(glowTex, sun.radius * 7f, new ColorRGBA(1f, 0.62f, 0.2f, 0.85f)));

        //          picture             size  far    year   day   tilt   glow
        planet(Textures.mercury(), 0.8f, 19.5f, 12.7f, 40, 0.5f, 0);
        planet(Textures.venus(), 1.8f, 28.6f, 22.4f, -60, 3, 0xf2d28a);
        Body earth = planet(Textures.earth(), 2.0f, 39.0f, 30f, 10, 23.4f, 0x58a6ff);
        planet(Textures.mars(), 1.2f, 49.4f, 43.8f, 10.5f, 25, 0xff8a5c);
        Body jupiter = planet(Textures.jupiter(), 5.0f, 71.5f, 132f, 6, 3, 0xf0c9a0);
        Body saturn = planet(Textures.saturn(), 4.0f, 97.5f, 228f, 7, 26.7f, 0xf1dfa8);
        Body uranus = planet(Textures.uranus(), 3.0f, 117f, 428f, 9, 97.8f, 0xa8f0f0);
        planet(Textures.neptune(), 2.8f, 136.5f, 642f, 9, 28, 0x6f9bff);

        // Earth gets drifting clouds.
        Material cloudMat = lit(Textures.clouds().toTexture(true), ColorRGBA.White);
        cloudMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        Geometry clouds = ball("clouds", earth.radius * 1.02f, 40, cloudMat);
        clouds.setQueueBucket(Bucket.Transparent);
        Node cloudSpin = new Node("cloudSpin");
        cloudSpin.attachChild(clouds);
        earth.spin.attachChild(cloudSpin);

        // Rings.
        Texture ringTex = Textures.ring().toTexture(false);
        ring(saturn, 1.35f, 2.35f, ringTex, new ColorRGBA(1, 1, 1, 0.95f));
        ring(uranus, 1.65f, 1.9f, ringTex, new ColorRGBA(0.8f, 0.95f, 1f, 0.3f));

        // Moons:    around   size   far   time  colour
        moon(earth, 0.5f, 3.6f, 7f, 0xffffff);
        moon(bodies.get(4), 0.22f, 2.1f, 3.5f, 0xb0a090);
        moon(bodies.get(4), 0.16f, 2.9f, 6f, 0xa09080);
        moon(jupiter, 0.42f, 6.6f, 5f, 0xf2dc6a); // Io
        moon(jupiter, 0.36f, 7.8f, 8f, 0xe8e2d0); // Europa
        moon(jupiter, 0.58f, 9.3f, 12f, 0xb8a890); // Ganymede
        moon(jupiter, 0.52f, 11f, 18f, 0x8a7d70); // Callisto
        moon(saturn, 0.55f, 11.2f, 14f, 0xe8a850); // Titan
        jupiter.clear = 12.6f;
        jupiter.viewDist = 42;
    }

    private Body planet(Tex tex, float radius, float dist, float year, float day, float tiltDeg, int haloColor) {
        Body b = new Body();
        b.tex = tex;
        b.radius = radius;
        b.dist = dist;
        b.period = year;
        b.spinPeriod = day;
        b.angle = rnd.nextFloat() * FastMath.TWO_PI;
        finishBody(b, lit(tex.toTexture(true), ColorRGBA.White), tiltDeg);
        if (haloColor != 0) {
            b.node.attachChild(glowCard(haloTex, radius * 2 / 0.6f * 0.99f, rgb(haloColor, 0.38f)));
        }
        if (dist > 0) {
            rootNode.attachChild(orbitLine(dist));
        }
        return b;
    }

    private void finishBody(Body b, Material mat, float tiltDeg) {
        b.index = bodies.size();
        b.clear = b.radius * 1.6f + 0.6f;
        b.viewDist = Math.max(b.radius * 5.5f, b.clear * 3.2f);
        b.node = new Node("body" + b.index);
        b.tilt = new Node("tilt");
        b.tilt.rotate(0, 0, tiltDeg * FastMath.DEG_TO_RAD);
        b.spin = new Node("spin");
        b.spin.attachChild(ball("ball", b.radius, 48, mat));
        b.tilt.attachChild(b.spin);
        b.node.attachChild(b.tilt);
        rootNode.attachChild(b.node);
        bodies.add(b);
    }

    private void ring(Body b, float inner, float outer, Texture tex, ColorRGBA color) {
        float r0 = b.radius * inner, r1 = b.radius * outer;
        int seg = 96;
        FloatBuffer pos = BufferUtils.createFloatBuffer((seg + 1) * 6);
        FloatBuffer uv = BufferUtils.createFloatBuffer((seg + 1) * 4);
        ShortBuffer idx = BufferUtils.createShortBuffer(seg * 6);
        for (int i = 0; i <= seg; i++) {
            float a = FastMath.TWO_PI * i / seg, c = FastMath.cos(a), s = FastMath.sin(a);
            pos.put(c * r0).put(0).put(s * r0).put(c * r1).put(0).put(s * r1);
            uv.put(0.002f).put(0.5f).put(0.998f).put(0.5f);
            if (i < seg) {
                int k = i * 2;
                idx.put((short) k).put((short) (k + 1)).put((short) (k + 2));
                idx.put((short) (k + 1)).put((short) (k + 3)).put((short) (k + 2));
            }
        }
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, pos);
        mesh.setBuffer(Type.TexCoord, 2, uv);
        mesh.setBuffer(Type.Index, 3, idx);
        mesh.updateBound();
        Geometry g = new Geometry("ring", mesh);
        Material m = unlit(tex, color, BlendMode.Alpha);
        m.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        g.setMaterial(m);
        g.setQueueBucket(Bucket.Transparent);
        b.tilt.attachChild(g);
        b.clear = Math.max(b.clear, r1 + 1.2f);
        b.viewDist = Math.max(b.viewDist, b.clear * 3.2f);
    }

    private void moon(Body b, float radius, float dist, float period, int color) {
        Moon m = new Moon();
        m.period = period;
        m.pivot = new Node("moonPivot");
        m.pivot.rotate(0.12f + rnd.nextFloat() * 0.15f, rnd.nextFloat() * FastMath.TWO_PI, 0);
        Geometry g = ball("moon", radius, 20, lit(moonTex, rgb(color, 1)));
        g.setLocalTranslation(dist, 0, 0);
        m.pivot.attachChild(g);
        b.node.attachChild(m.pivot);
        b.moons.add(m);
    }

    private Geometry orbitLine(float radius) {
        int n = 180;
        FloatBuffer pos = BufferUtils.createFloatBuffer(n * 3);
        for (int i = 0; i < n; i++) {
            float a = FastMath.TWO_PI * i / n;
            pos.put(FastMath.cos(a) * radius).put(0).put(FastMath.sin(a) * radius);
        }
        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.LineLoop);
        mesh.setBuffer(Type.Position, 3, pos);
        mesh.updateBound();
        mesh.updateCounts();
        Geometry g = new Geometry("orbit", mesh);
        g.setMaterial(unlit(null, new ColorRGBA(0.55f, 0.7f, 1f, 0.28f), BlendMode.Alpha));
        g.setQueueBucket(Bucket.Transparent);
        return g;
    }

    /** Hundreds of little rocks between Mars and Jupiter. */
    private void buildBelt() {
        belt = new Node("belt");
        Material mat = lit(null, new ColorRGBA(0.62f, 0.56f, 0.5f, 1));
        Sphere rock = new Sphere(5, 6, 1f);
        for (int i = 0; i < 520; i++) {
            Geometry g = new Geometry("rock", rock);
            g.setMaterial(mat);
            float a = rnd.nextFloat() * FastMath.TWO_PI;
            float r = 59.5f + (float) rnd.nextGaussian() * 1.9f;
            g.setLocalTranslation(FastMath.cos(a) * r, (float) rnd.nextGaussian() * 0.7f, FastMath.sin(a) * r);
            float s = 0.12f + rnd.nextFloat() * rnd.nextFloat() * 0.4f;
            g.setLocalScale(s * (0.6f + rnd.nextFloat()), s * (0.6f + rnd.nextFloat()), s * (0.6f + rnd.nextFloat()));
            g.rotate(rnd.nextFloat() * 6, rnd.nextFloat() * 6, rnd.nextFloat() * 6);
            belt.attachChild(g);
        }
        GeometryBatchFactory.optimize(belt);
        rootNode.attachChild(belt);
    }

    /** The rocket is made of simple shapes. Its nose points along +Z. */
    private void buildRocket() {
        rocket = new Node("rocket");
        Material white = lit(null, new ColorRGBA(0.96f, 0.96f, 1f, 1));
        Material red = lit(null, new ColorRGBA(0.95f, 0.16f, 0.14f, 1));
        Material blue = lit(null, new ColorRGBA(0.25f, 0.7f, 1f, 1));
        Material dark = lit(null, new ColorRGBA(0.25f, 0.25f, 0.3f, 1));

        Geometry body = new Geometry("body", new Cylinder(2, 24, 0.24f, 0.9f, true));
        body.setMaterial(white);
        rocket.attachChild(body);

        Geometry nose = new Geometry("nose", new Dome(10, 24, 0.24f));
        nose.setMaterial(red);
        nose.setLocalScale(1, 2.1f, 1);
        nose.rotate(FastMath.HALF_PI, 0, 0);
        nose.setLocalTranslation(0, 0, 0.45f);
        rocket.attachChild(nose);

        Geometry nozzle = new Geometry("nozzle", new Cylinder(2, 16, 0.16f, 0.14f, true));
        nozzle.setMaterial(dark);
        nozzle.setLocalTranslation(0, 0, -0.5f);
        rocket.attachChild(nozzle);

        for (int i = 0; i < 4; i++) {
            Node arm = new Node("fin");
            arm.rotate(0, 0, i * FastMath.HALF_PI + FastMath.QUARTER_PI);
            Geometry fin = new Geometry("fin", new Box(0.025f, 0.17f, 0.2f));
            fin.setMaterial(red);
            fin.setLocalTranslation(0, 0.33f, -0.3f);
            fin.rotate(0.35f, 0, 0);
            arm.attachChild(fin);
            rocket.attachChild(arm);
        }
        for (int i = 0; i < 2; i++) {
            Geometry window = new Geometry("window", new Sphere(12, 16, 0.1f));
            window.setMaterial(blue);
            window.setLocalTranslation(0, i == 0 ? 0.19f : -0.19f, 0.15f);
            rocket.attachChild(window);
        }
        // its own little lamp, so the rocket is never lost in the dark
        rocket.addLight(new AmbientLight(new ColorRGBA(0.5f, 0.5f, 0.5f, 1)));
        rootNode.attachChild(rocket);

        flame = new ParticleEmitter("flame", ParticleMesh.Type.Triangle, 300);
        flame.setMaterial(particleMaterial());
        flame.setStartColor(new ColorRGBA(1f, 0.85f, 0.3f, 0.9f));
        flame.setEndColor(new ColorRGBA(1f, 0.2f, 0.05f, 0f));
        flame.setGravity(0, 0, 0);
        flame.setLowLife(0.25f);
        flame.setHighLife(0.7f);
        flame.getParticleInfluencer().setVelocityVariation(0.25f);
        rootNode.attachChild(flame);

        Body b = bodies.get(rocketBody);
        placeBodies(0);
        rocketPos.set(b.pos).addLocal(b.clear, 0, 0);
    }

    private Material particleMaterial() {
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        m.setTexture("Texture", glowTex);
        return m;
    }

    // ------------------------------------------------------------------ things the buttons do

    /** Fly to a body (0 = Sun … 8 = Neptune) or look at everything (-1). */
    void select(int index) {
        selected = index;
        hudDirty = true;
        if (index < 0) {
            distGoal = 230;
            pitchGoal = 0.5f;
            return;
        }
        pitchGoal = 0.32f;
        Body b = bodies.get(index);
        distGoal = b.viewDist;
        if (index != rocketBody || traveling) {
            rocketBody = index;
            traveling = true;
            travelT = 0;
            travelStart.set(rocketPos);
            float d = rocketPos.distance(b.pos);
            travelDur = FastMath.clamp(d / 45f, 1.6f, 4.5f);
            travelLift = d * 0.3f;
        }
    }

    void home() {
        tour = false;
        select(-1);
    }

    void pick(int index) {
        tour = false;
        select(index);
    }

    void step(int dir) {
        tour = false;
        select(Math.floorMod(Math.max(selected, dir > 0 ? -1 : 0) + dir, bodies.size()));
    }

    void togglePause() {
        paused = !paused;
        hudDirty = true;
        hud.toast(paused ? "Pause" : (lang == 1 ? "Weiter!" : "Go!"));
    }

    void changeSpeed(int dir) {
        speedIdx = Math.max(0, Math.min(SPEEDS.length - 1, speedIdx + dir));
        if (paused) {
            paused = false;
            hudDirty = true;
        }
        hud.toast((lang == 1 ? "Tempo ×" : "Speed ×") + SPEED_NAMES[speedIdx]);
    }

    void toggleTour() {
        tour = !tour;
        hudDirty = true;
        if (tour) {
            tourTimer = 0;
            select((selected + 1) % bodies.size());
            hud.toast(lang == 1 ? "Raketen-Tour!" : "Rocket tour!");
        }
    }

    void toggleLang() {
        lang = 1 - lang;
        hudDirty = true;
    }

    void spawnComet() {
        Comet c = new Comet();
        float a = rnd.nextFloat() * FastMath.TWO_PI;
        c.pos.set(FastMath.cos(a) * 190, (rnd.nextFloat() - 0.5f) * 60, FastMath.sin(a) * 190);
        Vector3f side = c.pos.cross(Vector3f.UNIT_Y).normalizeLocal();
        Vector3f aim = side.mult((rnd.nextBoolean() ? 1 : -1) * (16 + rnd.nextFloat() * 24));
        c.vel.set(aim).subtractLocal(c.pos).normalizeLocal().multLocal(26);

        c.node = new Node("comet");
        c.node.attachChild(ball("ice", 0.5f, 10, unlit(null, new ColorRGBA(0.85f, 0.95f, 1f, 1), null)));
        c.node.attachChild(glowCard(glowTex, 9f, new ColorRGBA(0.6f, 0.85f, 1f, 0.9f)));
        c.node.setLocalTranslation(c.pos);
        rootNode.attachChild(c.node);

        c.tail = new ParticleEmitter("tail", ParticleMesh.Type.Triangle, 500);
        c.tail.setMaterial(particleMaterial());
        c.tail.setStartColor(new ColorRGBA(0.7f, 0.9f, 1f, 0.75f));
        c.tail.setEndColor(new ColorRGBA(0.3f, 0.5f, 1f, 0f));
        c.tail.setStartSize(2.4f);
        c.tail.setEndSize(0.4f);
        c.tail.setLowLife(1.5f);
        c.tail.setHighLife(3.5f);
        c.tail.setParticlesPerSec(110);
        c.tail.setGravity(0, 0, 0);
        c.tail.getParticleInfluencer().setVelocityVariation(0.2f);
        c.node.attachChild(c.tail);
        comets.add(c);
    }

    // ------------------------------------------------------------------ every frame

    @Override
    public void simpleUpdate(float tpf) {
        tpf = Math.min(tpf, 0.05f);
        float sim = paused ? 0 : tpf * SPEEDS[speedIdx];

        placeBodies(sim);
        belt.rotate(0, FastMath.TWO_PI / 90f * sim, 0);
        updateComets(tpf, sim);
        updateRocket(tpf);
        updateCamera(tpf);

        if (tour) {
            tourTimer += tpf;
            if (tourTimer > 11) {
                tourTimer = 0;
                select((selected + 1) % bodies.size());
            }
        }
        if (hudDirty) {
            hudDirty = false;
            hud.rebuild();
        }
        hud.update(tpf);

        if (shotDir != null) {
            shotTime += tpf;
            if (shotFrames < 0 && shotTime > 8) { // long enough for the rocket to arrive
                stateManager.getState(ScreenshotAppState.class).takeScreenshot();
                shotFrames = 0;
            } else if (shotFrames >= 0 && ++shotFrames > 6) {
                stop();
            }
        }
    }

    private void placeBodies(float sim) {
        for (Body b : bodies) {
            if (b.period > 0) {
                b.angle += FastMath.TWO_PI / b.period * sim;
                b.pos.set(FastMath.cos(b.angle) * b.dist, 0, -FastMath.sin(b.angle) * b.dist);
                b.node.setLocalTranslation(b.pos);
            }
            b.spin.rotate(0, FastMath.TWO_PI / b.spinPeriod * sim, 0);
            for (Moon m : b.moons) {
                m.pivot.rotate(0, FastMath.TWO_PI / m.period * sim, 0);
            }
        }
        Spatial cloudSpin = bodies.get(3).spin.getChild("cloudSpin");
        if (cloudSpin != null) {
            cloudSpin.rotate(0, 0.05f * sim, 0);
        }
    }

    private void updateComets(float tpf, float sim) {
        cometTimer -= tpf;
        if (cometTimer < 0) {
            cometTimer = 35 + rnd.nextFloat() * 30;
            spawnComet();
        }
        for (Iterator<Comet> it = comets.iterator(); it.hasNext();) {
            Comet c = it.next();
            int steps = Math.max(1, (int) Math.ceil(sim / 0.01f));
            float dt = sim / steps;
            for (int i = 0; i < steps; i++) { // the Sun pulls the comet around
                float r2 = Math.max(c.pos.lengthSquared(), 30f);
                float pull = -7000f / (r2 * FastMath.sqrt(r2)) * dt;
                c.vel.addLocal(c.pos.x * pull, c.pos.y * pull, c.pos.z * pull);
                c.pos.addLocal(c.vel.x * dt, c.vel.y * dt, c.vel.z * dt);
            }
            c.node.setLocalTranslation(c.pos);
            // the tail always blows away from the Sun
            c.tail.getParticleInfluencer().setInitialVelocity(c.pos.normalize().multLocal(9f));
            float r = c.pos.length();
            if (r < bodies.get(0).radius || r > 300) {
                c.node.removeFromParent();
                it.remove();
            }
        }
    }

    private void updateRocket(float tpf) {
        Body b = bodies.get(rocketBody);
        Vector3f before = rocketPos.clone();
        if (traveling) {
            travelT = Math.min(1, travelT + tpf / travelDur);
            float s = travelT * travelT * (3 - 2 * travelT);
            Vector3f in = travelStart.subtract(b.pos);
            in.y = 0;
            if (in.lengthSquared() < 0.01f) {
                in.set(1, 0, 0);
            }
            in.normalizeLocal();
            Vector3f end = b.pos.add(in.mult(b.clear));
            rocketPos.interpolateLocal(travelStart, end, s);
            rocketPos.y += FastMath.sin(FastMath.PI * s) * travelLift;
            if (travelT >= 1) {
                traveling = false;
                orbitAngle = FastMath.atan2(in.z, in.x);
            }
        } else {
            orbitAngle += tpf * 0.9f;
            rocketPos.set(FastMath.cos(orbitAngle) * b.clear, FastMath.sin(orbitAngle * 0.5f) * b.clear * 0.18f,
                    FastMath.sin(orbitAngle) * b.clear).addLocal(b.pos);
        }
        Vector3f move = rocketPos.subtract(before);
        if (move.lengthSquared() > 1e-8f) {
            move.normalizeLocal();
            Quaternion want = new Quaternion();
            want.lookAt(move, Vector3f.UNIT_Y);
            rocketRot.slerp(want, 1 - FastMath.exp(-7 * tpf));
            rocketRot.normalizeLocal();
        }
        float wantScale = 0.38f + 0.1f * b.radius;
        rocketScale += (wantScale - rocketScale) * (1 - FastMath.exp(-2 * tpf));
        rocket.setLocalTranslation(rocketPos);
        rocket.setLocalRotation(rocketRot);
        rocket.setLocalScale(rocketScale);

        Vector3f back = rocketRot.mult(Vector3f.UNIT_Z).negateLocal();
        flame.setLocalTranslation(rocketPos.add(back.mult(0.6f * rocketScale)));
        flame.getParticleInfluencer().setInitialVelocity(back.mult((traveling ? 5f : 2.2f) * rocketScale));
        flame.setStartSize(0.5f * rocketScale);
        flame.setEndSize(0.1f * rocketScale);
        flame.setParticlesPerSec(traveling ? 220 : 110);
    }

    private void updateCamera(float tpf) {
        // What to look at: everything, the flying rocket, or the chosen body.
        Vector3f target;
        int key;
        if (selected < 0) {
            target = Vector3f.ZERO;
            key = -1;
        } else if (traveling && rocketBody == selected) {
            target = rocketPos;
            key = 100;
        } else {
            target = bodies.get(selected).pos;
            key = selected;
        }
        if (key != focusKey) { // changed target: keep the view where it is and glide over
            offset.set(focus).subtractLocal(target);
            focusKey = key;
        }
        offset.multLocal(FastMath.exp(-3.5f * tpf));
        focus.set(target).addLocal(offset);

        float ease = 1 - FastMath.exp(-3f * tpf);
        dist += (distGoal - dist) * ease;
        if (!Float.isNaN(pitchGoal)) {
            pitch += (pitchGoal - pitch) * ease;
            if (Math.abs(pitchGoal - pitch) < 0.005f) {
                pitchGoal = Float.NaN;
            }
        }
        float cp = FastMath.cos(pitch);
        Vector3f eye = new Vector3f(cp * FastMath.sin(yaw), FastMath.sin(pitch), cp * FastMath.cos(yaw))
                .multLocal(dist).addLocal(focus);
        cam.setLocation(eye);
        cam.lookAt(focus, Vector3f.UNIT_Y);
        sky.setLocalTranslation(eye);
    }

    /**
     * Leave at once (Esc or closing the window). There is nothing to save, and the slow, tidy
     * shutdown crashes inside the WSL graphics driver.
     */
    @Override
    public void stop(boolean waitFor) {
        Runtime.getRuntime().halt(0);
    }

    @Override
    public void requestClose(boolean esc) {
        Runtime.getRuntime().halt(0);
    }

    @Override
    public void reshape(int w, int h) {
        super.reshape(w, h);
        if (hud != null && w > 0 && h > 0) {
            cam.setFrustumPerspective(45f, (float) w / h, 0.5f, 6000f);
            hudDirty = true;
        }
    }

    // ------------------------------------------------------------------ mouse and keyboard

    private void click(float x, float y) {
        if (hud.click(x, y)) {
            return;
        }
        Vector2f m = new Vector2f(x, y);
        Vector3f from = cam.getWorldCoordinates(m, 0f);
        Vector3f dir = cam.getWorldCoordinates(m, 1f).subtractLocal(from).normalizeLocal();
        int best = -1;
        float bestT = Float.MAX_VALUE;
        for (Body b : bodies) {
            Vector3f oc = b.pos.subtract(from);
            float t = oc.dot(dir);
            if (t <= 0) {
                continue;
            }
            float miss = oc.subtractLocal(dir.mult(t)).length();
            // generous, so small fingers on a small planet still hit
            if (miss < Math.max(b.radius * 1.25f, t * 0.028f) && t < bestT) {
                best = b.index;
                bestT = t;
            }
        }
        if (best >= 0) {
            pick(best);
        }
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent e) {
        if (e.isPressed()) {
            dragging = true;
            dragMoved = 0;
            pressOnHud = hud.hit(e.getX(), e.getY());
        } else if (dragging) {
            dragging = false;
            if (dragMoved < 8 && e.getButtonIndex() == 0) {
                click(e.getX(), e.getY());
            }
        }
    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent e) {
        if (dragging && !pressOnHud) {
            dragMoved += Math.abs(e.getDX()) + Math.abs(e.getDY());
            if (dragMoved >= 8) {
                yaw -= e.getDX() * 0.006f;
                pitch = FastMath.clamp(pitch - e.getDY() * 0.006f, -1.45f, 1.45f);
                pitchGoal = Float.NaN;
            }
        }
        if (e.getDeltaWheel() != 0) {
            float steps = FastMath.clamp(e.getDeltaWheel() / 120f, -4, 4);
            float min = selected < 0 ? 25 : bodies.get(selected).radius * 2.3f;
            distGoal = FastMath.clamp(distGoal * FastMath.exp(-0.16f * steps), min, 600);
        }
    }

    @Override
    public void onKeyEvent(KeyInputEvent e) {
        if (!e.isPressed() || e.isRepeating()) {
            return;
        }
        int k = e.getKeyCode();
        if (k >= KeyInput.KEY_1 && k <= KeyInput.KEY_8) {
            pick(k - KeyInput.KEY_1 + 1);
        } else if (k == KeyInput.KEY_0 || k == KeyInput.KEY_9) {
            pick(0);
        } else if (k == KeyInput.KEY_SPACE) {
            togglePause();
        } else if (k == KeyInput.KEY_RIGHT) {
            step(1);
        } else if (k == KeyInput.KEY_LEFT) {
            step(-1);
        } else if (k == KeyInput.KEY_UP) {
            changeSpeed(1);
        } else if (k == KeyInput.KEY_DOWN) {
            changeSpeed(-1);
        } else if (k == KeyInput.KEY_T) {
            toggleTour();
        } else if (k == KeyInput.KEY_H) {
            home();
        } else if (k == KeyInput.KEY_C) {
            spawnComet();
        } else if (k == KeyInput.KEY_L) {
            toggleLang();
        }
    }

    @Override
    public void beginInput() {
    }

    @Override
    public void endInput() {
    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent e) {
    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent e) {
    }

    @Override
    public void onTouchEvent(TouchEvent e) {
    }
}
