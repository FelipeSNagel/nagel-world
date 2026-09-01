import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class GenerateTextures {
    private static final int SIZE = 64;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Uso: GenerateTextures <pasta>");
        }
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        weapon(output.resolve("pistol.png"), 10, 25, 47, 7, 24, 8, new Color(48, 52, 55), new Color(147, 105, 56));
        weapon(output.resolve("shotgun.png"), 5, 24, 55, 6, 29, 8, new Color(66, 70, 68), new Color(119, 72, 42));
        weapon(output.resolve("rifle.png"), 5, 22, 55, 8, 26, 10, new Color(45, 52, 47), new Color(75, 91, 61));
        sniper(output.resolve("sniper.png"));
        cartridge(output.resolve("light_ammo.png"), new Color(218, 163, 58), 13);
        shell(output.resolve("shell.png"));
        cartridge(output.resolve("rifle_ammo.png"), new Color(190, 128, 48), 18);
        cartridge(output.resolve("sniper_ammo.png"), new Color(155, 95, 42), 23);
    }

    private static Graphics2D canvas(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        return graphics;
    }

    private static void weapon(Path file, int x, int y, int width, int height, int gripX, int gripWidth,
                               Color metal, Color accent) throws IOException {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas(image);
        g.setColor(new Color(17, 19, 20));
        g.fillRect(x - 2, y - 2, width + 4, height + 4);
        g.setColor(metal);
        g.fillRect(x, y, width, height);
        g.setColor(new Color(118, 127, 128));
        g.fillRect(x + 4, y + 1, Math.max(5, width - 12), 2);
        g.setColor(accent);
        g.fillRect(gripX, y + height, gripWidth, 17);
        g.setColor(new Color(63, 43, 29));
        g.fillRect(gripX + 2, y + height + 3, Math.max(2, gripWidth - 4), 11);
        g.setColor(new Color(26, 27, 27));
        g.fillRect(x + width - 4, y + 1, 6, Math.max(3, height - 2));
        g.fillRect(gripX - 5, y + height + 1, 7, 4);
        g.dispose();
        ImageIO.write(image, "png", file.toFile());
    }

    private static void sniper(Path file) throws IOException {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas(image);
        g.setColor(new Color(18, 20, 20));
        g.fillRect(3, 27, 58, 9);
        g.setColor(new Color(50, 58, 53));
        g.fillRect(5, 29, 53, 5);
        g.setColor(new Color(117, 82, 46));
        g.fillRect(8, 35, 25, 8);
        g.fillRect(24, 41, 9, 13);
        g.setColor(new Color(23, 25, 25));
        g.fillRect(21, 20, 28, 7);
        g.fillRect(27, 18, 16, 2);
        g.setColor(new Color(71, 89, 82));
        g.fillRect(24, 22, 22, 3);
        g.setColor(new Color(51, 122, 132));
        g.fillRect(45, 21, 3, 5);
        g.setColor(new Color(140, 145, 143));
        g.fillRect(35, 29, 3, 3);
        g.dispose();
        ImageIO.write(image, "png", file.toFile());
    }

    private static void cartridge(Path file, Color brass, int height) throws IOException {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas(image);
        int x = 27;
        int y = 28 - Math.max(0, height - 13) / 2;
        g.setColor(new Color(68, 55, 35));
        g.fillRect(x - 2, y - 4, 12, height + 8);
        g.setColor(brass);
        g.fillRect(x, y, 8, height);
        g.setColor(new Color(232, 220, 186));
        g.fillRect(x + 2, y - 3, 4, 4);
        g.setColor(new Color(112, 76, 31));
        g.fillRect(x - 1, y + height - 2, 10, 4);
        g.dispose();
        ImageIO.write(image, "png", file.toFile());
    }

    private static void shell(Path file) throws IOException {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas(image);
        g.setColor(new Color(68, 30, 24));
        g.fillRect(24, 20, 16, 30);
        g.setColor(new Color(155, 45, 38));
        g.fillRect(27, 22, 10, 24);
        g.setColor(new Color(211, 157, 52));
        g.fillRect(24, 44, 16, 7);
        g.fillRect(26, 18, 12, 4);
        g.dispose();
        ImageIO.write(image, "png", file.toFile());
    }
}

