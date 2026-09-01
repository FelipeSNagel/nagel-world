import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;

public final class GenerateTextures {
    private static final int SIZE = 64;
    private static final Color OUTLINE = new Color(12, 14, 15);
    private static final Color DARK_METAL = new Color(37, 42, 44);
    private static final Color METAL = new Color(78, 87, 89);
    private static final Color HIGHLIGHT = new Color(145, 155, 154);
    private static final Color WOOD = new Color(112, 69, 38);
    private static final Color WOOD_LIGHT = new Color(158, 102, 52);
    private static final Color OLIVE = new Color(65, 78, 58);

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Uso: GenerateTextures <pasta>");
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        pistol(output.resolve("pistol.png"));
        shotgun(output.resolve("shotgun.png"));
        rifle(output.resolve("rifle.png"));
        sniper(output.resolve("sniper.png"));
        cartridge(output.resolve("light_ammo.png"), new Color(218, 163, 58), 13);
        shell(output.resolve("shell.png"));
        cartridge(output.resolve("rifle_ammo.png"), new Color(190, 128, 48), 18);
        cartridge(output.resolve("sniper_ammo.png"), new Color(155, 95, 42), 23);
        material(output.resolve("gun_dark.png"), DARK_METAL, new Color(24, 27, 28));
        material(output.resolve("gun_metal.png"), METAL, HIGHLIGHT);
        material(output.resolve("gun_wood.png"), WOOD, WOOD_LIGHT);
        material(output.resolve("gun_olive.png"), OLIVE, new Color(91, 105, 73));
        material(output.resolve("gun_scope.png"), new Color(25, 34, 36), new Color(59, 112, 124));
        zombie(output.resolve("zombie.png"));
    }

    private static Graphics2D canvas(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        return graphics;
    }

    private static void pistol(Path file) throws IOException {
        BufferedImage image = transparent();
        Graphics2D g = canvas(image);
        rect(g, OUTLINE, 9, 20, 46, 15);
        rect(g, DARK_METAL, 11, 22, 42, 10);
        rect(g, HIGHLIGHT, 14, 23, 28, 2);
        rect(g, new Color(20, 22, 23), 47, 24, 8, 6);
        rect(g, OUTLINE, 21, 32, 18, 7);
        rect(g, METAL, 24, 33, 11, 3);
        Polygon grip = new Polygon(new int[] {31, 43, 38, 26}, new int[] {34, 34, 57, 57}, 4);
        g.setColor(OUTLINE); g.fillPolygon(grip);
        Polygon gripInner = new Polygon(new int[] {32, 40, 36, 28}, new int[] {37, 37, 54, 54}, 4);
        g.setColor(new Color(74, 51, 35)); g.fillPolygon(gripInner);
        rect(g, WOOD_LIGHT, 31, 40, 7, 2);
        rect(g, new Color(185, 49, 36), 13, 27, 3, 3);
        finish(g, image, file);
    }

    private static void shotgun(Path file) throws IOException {
        BufferedImage image = transparent();
        Graphics2D g = canvas(image);
        rect(g, OUTLINE, 3, 22, 58, 13);
        rect(g, DARK_METAL, 5, 24, 53, 7);
        rect(g, HIGHLIGHT, 8, 25, 44, 2);
        rect(g, OUTLINE, 18, 31, 25, 9);
        rect(g, WOOD, 20, 32, 20, 6);
        rect(g, WOOD_LIGHT, 22, 33, 15, 2);
        Polygon stock = new Polygon(new int[] {4, 22, 22, 9}, new int[] {31, 31, 42, 47}, 4);
        g.setColor(OUTLINE); g.fillPolygon(stock);
        Polygon stockInner = new Polygon(new int[] {7, 20, 19, 10}, new int[] {33, 33, 39, 43}, 4);
        g.setColor(WOOD); g.fillPolygon(stockInner);
        rect(g, OUTLINE, 36, 37, 8, 15);
        rect(g, WOOD, 38, 39, 4, 11);
        rect(g, new Color(161, 42, 30), 56, 24, 4, 7);
        finish(g, image, file);
    }

    private static void rifle(Path file) throws IOException {
        BufferedImage image = transparent();
        Graphics2D g = canvas(image);
        rect(g, OUTLINE, 4, 20, 57, 15);
        rect(g, DARK_METAL, 6, 23, 53, 8);
        rect(g, METAL, 19, 21, 26, 12);
        rect(g, HIGHLIGHT, 22, 23, 18, 2);
        rect(g, OUTLINE, 23, 14, 19, 8);
        rect(g, new Color(32, 38, 36), 25, 16, 15, 4);
        rect(g, new Color(67, 126, 137), 37, 17, 3, 2);
        Polygon stock = new Polygon(new int[] {5, 22, 23, 10}, new int[] {29, 29, 40, 45}, 4);
        g.setColor(OUTLINE); g.fillPolygon(stock);
        Polygon stockInner = new Polygon(new int[] {8, 20, 20, 11}, new int[] {31, 31, 37, 41}, 4);
        g.setColor(OLIVE); g.fillPolygon(stockInner);
        Polygon magazine = new Polygon(new int[] {31, 42, 39, 29}, new int[] {32, 32, 52, 48}, 4);
        g.setColor(OUTLINE); g.fillPolygon(magazine);
        Polygon magazineInner = new Polygon(new int[] {33, 39, 37, 32}, new int[] {35, 35, 48, 46}, 4);
        g.setColor(DARK_METAL); g.fillPolygon(magazineInner);
        rect(g, new Color(182, 48, 34), 57, 24, 4, 6);
        finish(g, image, file);
    }

    private static void sniper(Path file) throws IOException {
        BufferedImage image = transparent();
        Graphics2D g = canvas(image);
        rect(g, OUTLINE, 2, 24, 60, 12);
        rect(g, DARK_METAL, 4, 27, 56, 6);
        rect(g, HIGHLIGHT, 22, 28, 30, 2);
        rect(g, OUTLINE, 18, 14, 32, 11);
        rect(g, new Color(28, 35, 35), 20, 16, 28, 6);
        rect(g, new Color(62, 120, 132), 43, 17, 4, 4);
        rect(g, METAL, 29, 22, 3, 5);
        Polygon stock = new Polygon(new int[] {4, 27, 27, 10}, new int[] {32, 32, 40, 48}, 4);
        g.setColor(OUTLINE); g.fillPolygon(stock);
        Polygon stockInner = new Polygon(new int[] {7, 24, 24, 11}, new int[] {34, 34, 38, 44}, 4);
        g.setColor(WOOD); g.fillPolygon(stockInner);
        rect(g, OUTLINE, 31, 34, 10, 19);
        rect(g, WOOD, 34, 36, 5, 14);
        rect(g, new Color(188, 50, 35), 59, 27, 3, 5);
        finish(g, image, file);
    }

    private static BufferedImage transparent() {
        return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private static void rect(Graphics2D g, Color color, int x, int y, int width, int height) {
        g.setColor(color);
        g.fillRect(x, y, width, height);
    }

    private static void finish(Graphics2D g, BufferedImage image, Path file) throws IOException {
        g.dispose();
        ImageIO.write(image, "png", file.toFile());
    }

    private static void material(Path file, Color base, Color highlight) throws IOException {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas(image);
        rect(g, base, 0, 0, 16, 16);
        rect(g, highlight, 0, 0, 16, 3);
        rect(g, base.darker(), 0, 13, 16, 3);
        for (int x = 1; x < 16; x += 4) rect(g, new Color(255, 255, 255, 22), x, 3, 1, 10);
        finish(g, image, file);
    }

    private static void cartridge(Path file, Color brass, int height) throws IOException {
        BufferedImage image = transparent();
        Graphics2D g = canvas(image);
        int x = 27;
        int y = 28 - Math.max(0, height - 13) / 2;
        rect(g, new Color(68, 55, 35), x - 2, y - 4, 12, height + 8);
        rect(g, brass, x, y, 8, height);
        rect(g, new Color(242, 225, 172), x + 2, y - 3, 4, 4);
        rect(g, new Color(112, 76, 31), x - 1, y + height - 2, 10, 4);
        finish(g, image, file);
    }

    private static void shell(Path file) throws IOException {
        BufferedImage image = transparent();
        Graphics2D g = canvas(image);
        rect(g, new Color(68, 30, 24), 24, 20, 16, 30);
        rect(g, new Color(160, 43, 35), 27, 22, 10, 24);
        rect(g, new Color(224, 171, 57), 24, 44, 16, 7);
        rect(g, new Color(239, 195, 78), 26, 18, 12, 4);
        finish(g, image, file);
    }

    private static void zombie(Path file) throws IOException {
        BufferedImage image = transparent();
        Random random = new Random(90210L);
        Color skin = new Color(151, 143, 133);
        Color skinDark = new Color(107, 101, 96);
        Color shirt = new Color(58, 70, 74);
        Color shirtDark = new Color(36, 43, 46);
        Color pants = new Color(54, 49, 45);
        Color pantsDark = new Color(35, 32, 30);

        skinPart(image, random, skin, skinDark, 8, 0, 8, 8);
        skinPart(image, random, skinDark, skin, 16, 0, 8, 8);
        for (int x = 0; x <= 24; x += 8) skinPart(image, random, skin, skinDark, x, 8, 8, 8);
        bodyPart(image, random, shirt, shirtDark, 20, 16, 8, 4);
        bodyPart(image, random, shirtDark, shirt, 28, 16, 8, 4);
        bodyPart(image, random, shirt, shirtDark, 16, 20, 24, 12);
        bodyPart(image, random, skin, skinDark, 40, 16, 16, 16);
        bodyPart(image, random, pants, pantsDark, 0, 16, 16, 16);
        bodyPart(image, random, skin, skinDark, 32, 48, 16, 16);
        bodyPart(image, random, pants, pantsDark, 16, 48, 16, 16);

        Graphics2D g = canvas(image);
        rect(g, new Color(230, 219, 176), 9, 10, 2, 1);
        rect(g, new Color(191, 55, 45), 13, 10, 2, 1);
        rect(g, new Color(78, 30, 28), 10, 14, 5, 1);
        rect(g, new Color(118, 30, 28), 24, 21, 4, 7);
        rect(g, new Color(177, 45, 37), 25, 22, 2, 3);
        rect(g, new Color(104, 29, 27), 45, 24, 3, 5);
        rect(g, new Color(94, 28, 27), 5, 24, 2, 6);
        rect(g, new Color(76, 24, 23), 18, 53, 3, 6);
        finish(g, image, file);
    }

    private static void skinPart(BufferedImage image, Random random, Color base, Color shadow,
                                 int x, int y, int width, int height) {
        noisyPart(image, random, base, shadow, x, y, width, height, 0.24);
    }

    private static void bodyPart(BufferedImage image, Random random, Color base, Color shadow,
                                 int x, int y, int width, int height) {
        noisyPart(image, random, base, shadow, x, y, width, height, 0.18);
    }

    private static void noisyPart(BufferedImage image, Random random, Color base, Color shadow,
                                  int x, int y, int width, int height, double chance) {
        for (int py = y; py < y + height; py++) {
            for (int px = x; px < x + width; px++) {
                double roll = random.nextDouble();
                Color color = roll < chance ? shadow : roll > 0.92 ? base.brighter() : base;
                image.setRGB(px, py, color.getRGB());
            }
        }
    }
}
