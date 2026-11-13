package dev.async;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class AsyncColorPickerScreen extends Screen {

    private static final Identifier GUI_FONT = Identifier.of("async", "gentle");

    private static Text soft(String value) {
        return Text.literal(value).styled(style ->
                style.withFont(new StyleSpriteSource.Font(GUI_FONT)));
    }

    private static final int BACKGROUND = 0xD0080611;
    private static final int PANEL = 0xFF100C19;
    private static final int CARD = 0xFF191323;
    private static final int WHITE = 0xFFF8F0FF;
    private static final int MUTED = 0xFFA69AB5;
    private static final int BORDER = 0xFF493D58;

    private final Screen parent;
    private final AsyncAppearanceConfig appearance;

    private final int originalColor;
    private int selectedColor;

    private float hue;
    private float saturation;
    private float value;

    private int panelX;
    private int panelY;
    private int squareX;
    private int squareY;
    private int squareWidth;
    private int squareHeight;
    private int hueX;
    private int hueY;
    private int hueWidth;
    private int hueHeight;

    private TextFieldWidget hexField;
    private boolean updatingHex;

    public AsyncColorPickerScreen(Screen parent, AsyncAppearanceConfig appearance) {
        super(soft("Async Color Picker"));
        this.parent = parent;
        this.appearance = appearance;
        this.originalColor = appearance.getAccentColor();
        this.selectedColor = originalColor;
        updateHsvFromColor(selectedColor);
    }

    @Override
    protected void init() {
        clearChildren();

        int panelWidth = 320;
        int panelHeight = 238;
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        squareX = panelX + 20;
        squareY = panelY + 52;
        squareWidth = 176;
        squareHeight = 120;

        hueX = panelX + 207;
        hueY = squareY;
        hueWidth = 16;
        hueHeight = squareHeight;

        hexField = new TextFieldWidget(
                textRenderer,
                panelX + 231,
                panelY + 137,
                72,
                20,
                soft("HEX")
        );
        hexField.setMaxLength(7);
        hexField.setText(hexString(selectedColor));
        hexField.setChangedListener(this::onHexChanged);
        addDrawableChild(hexField);

        addDrawableChild(ButtonWidget.builder(soft("APPLY"), button -> {
            appearance.setAccentColor(selectedColor);
            if (client != null) {
                client.setScreen(parent);
            }
        }).dimensions(panelX + 66, panelY + 198, 88, 22).build());

        addDrawableChild(ButtonWidget.builder(soft("CANCEL"), button -> {
            if (client != null) {
                client.setScreen(parent);
            }
        }).dimensions(panelX + 166, panelY + 198, 88, 22).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, BACKGROUND);

        context.fill(panelX, panelY, panelX + 320, panelY + 238, PANEL);
        drawBorder(context, panelX, panelY, 320, 238, BORDER);

        context.fill(panelX + 1, panelY + 1, panelX + 319, panelY + 3,
                appearance.getAccentColor());

        context.drawTextWithShadow(textRenderer,
                soft("CLICKGUI COLOR"), panelX + 18, panelY + 15, WHITE);
        context.drawText(textRenderer,
                soft("Choose any RGB color"), panelX + 18, panelY + 30,
                MUTED, false);

        drawSaturationValueSquare(context);
        drawHueBar(context);

        context.drawText(textRenderer, soft("PREVIEW"),
                panelX + 231, panelY + 52, MUTED, false);
        context.fill(panelX + 231, panelY + 66,
                panelX + 303, panelY + 112, selectedColor);
        drawBorder(context, panelX + 231, panelY + 66, 72, 46, BORDER);

        context.drawText(textRenderer, soft("HEX"),
                panelX + 231, panelY + 123, MUTED, false);

        int markerX = squareX + Math.round(saturation * (squareWidth - 1));
        int markerY = squareY + Math.round((1.0F - value) * (squareHeight - 1));
        context.fill(markerX - 3, markerY - 1, markerX + 4, markerY + 2, 0xFFFFFFFF);
        context.fill(markerX - 1, markerY - 3, markerX + 2, markerY + 4, 0xFFFFFFFF);

        int hueMarkerY = hueY + Math.round((hue / 360.0F) * (hueHeight - 1));
        context.fill(hueX - 2, hueMarkerY - 1,
                hueX + hueWidth + 2, hueMarkerY + 2, 0xFFFFFFFF);

        context.drawText(textRenderer,
                soft("Right-click CLICKGUI COLOR to open this picker"),
                panelX + 18, panelY + 181, MUTED, false);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawSaturationValueSquare(DrawContext context) {
        int hueColor = hsvToRgb(hue, 1.0F, 1.0F);
        for (int py = 0; py < squareHeight; py += 2) {
            float v = 1.0F - (float) py / (squareHeight - 1);
            for (int px = 0; px < squareWidth; px += 2) {
                float s = (float) px / (squareWidth - 1);
                int color = hsvToRgb(hue, s, v);
                context.fill(squareX + px, squareY + py,
                        Math.min(squareX + squareWidth, squareX + px + 2),
                        Math.min(squareY + squareHeight, squareY + py + 2),
                        color);
            }
        }
        drawBorder(context, squareX, squareY, squareWidth, squareHeight, BORDER);
    }

    private void drawHueBar(DrawContext context) {
        for (int py = 0; py < hueHeight; py++) {
            float h = (float) py / (hueHeight - 1) * 360.0F;
            context.fill(hueX, hueY + py, hueX + hueWidth, hueY + py + 1,
                    hsvToRgb(h, 1.0F, 1.0F));
        }
        drawBorder(context, hueX, hueY, hueWidth, hueHeight, BORDER);
    }

    private void onHexChanged(String text) {
        if (updatingHex || text == null) {
            return;
        }

        String hex = text.trim().replace("#", "");
        if (hex.length() != 6) {
            return;
        }

        try {
            selectedColor = 0xFF000000 | Integer.parseInt(hex, 16);
            updateHsvFromColor(selectedColor);
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateHexField() {
        if (hexField == null) {
            return;
        }

        updatingHex = true;
        hexField.setText(hexString(selectedColor));
        updatingHex = false;
    }

    private String hexString(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0x00FFFFFF);
    }

    private void updateHsvFromColor(int color) {
        float r = ((color >> 16) & 255) / 255.0F;
        float g = ((color >> 8) & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        value = max;
        saturation = max == 0.0F ? 0.0F : delta / max;

        if (delta == 0.0F) {
            hue = 0.0F;
        } else if (max == r) {
            hue = 60.0F * (((g - b) / delta) % 6.0F);
        } else if (max == g) {
            hue = 60.0F * (((b - r) / delta) + 2.0F);
        } else {
            hue = 60.0F * (((r - g) / delta) + 4.0F);
        }

        if (hue < 0.0F) {
            hue += 360.0F;
        }
    }

    private int hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1.0F - Math.abs((h / 60.0F) % 2.0F - 1.0F));
        float m = v - c;

        float r;
        float g;
        float b;

        if (h < 60.0F) {
            r = c; g = x; b = 0.0F;
        } else if (h < 120.0F) {
            r = x; g = c; b = 0.0F;
        } else if (h < 180.0F) {
            r = 0.0F; g = c; b = x;
        } else if (h < 240.0F) {
            r = 0.0F; g = x; b = c;
        } else if (h < 300.0F) {
            r = x; g = 0.0F; b = c;
        } else {
            r = c; g = 0.0F; b = x;
        }

        int ri = Math.round((r + m) * 255.0F);
        int gi = Math.round((g + m) * 255.0F);
        int bi = Math.round((b + m) * 255.0F);
        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            double x = click.x();
            double y = click.y();

            if (x >= squareX && x < squareX + squareWidth
                    && y >= squareY && y < squareY + squareHeight) {
                saturation = clamp01((float) ((x - squareX) / squareWidth));
                value = 1.0F - clamp01((float) ((y - squareY) / squareHeight));
                selectedColor = hsvToRgb(hue, saturation, value);
                updateHexField();
                return true;
            }

            if (x >= hueX && x < hueX + hueWidth
                    && y >= hueY && y < hueY + hueHeight) {
                hue = clamp01((float) ((y - hueY) / hueHeight)) * 360.0F;
                if (hue >= 360.0F) {
                    hue = 359.9F;
                }
                selectedColor = hsvToRgb(hue, saturation, value);
                updateHexField();
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (client != null) {
                client.setScreen(parent);
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
