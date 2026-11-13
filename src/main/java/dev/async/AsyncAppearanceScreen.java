package dev.async;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class AsyncAppearanceScreen extends Screen {

    private static final Identifier GUI_FONT = Identifier.of("async", "gentle");

    private static Text soft(String value) {
        return Text.literal(value).styled(style ->
                style.withFont(new StyleSpriteSource.Font(GUI_FONT)));
    }

    private static final int BACKGROUND = 0xB8080611;
    private static final int PANEL = 0xFF100C19;
    private static final int CARD = 0xFF191323;
    private static final int WHITE = 0xFFF8F0FF;
    private static final int MUTED = 0xFFA69AB5;

    private final AsyncConfig config;
    private final AsyncAppearanceConfig appearance;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private boolean listeningForMenuKey;

    public AsyncAppearanceScreen(AsyncConfig config) {
        super(soft("Async ClickGUI Appearance"));
        this.config = config;
        this.appearance = new AsyncAppearanceConfig();
    }

    @Override
    protected void init() {
        clearChildren();

        float scale = appearance.getScale();
        panelWidth = Math.round(430 * scale);
        panelHeight = Math.round(290 * scale);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int centerX = width / 2;
        int buttonY = panelY + Math.round(126 * scale);
        int buttonWidth = Math.min(190, panelWidth - 48);

        addDrawableChild(ButtonWidget.builder(
                        colorButtonText(),
                        button -> {
                            if (client != null) {
                                client.setScreen(new AsyncColorPickerScreen(this, appearance));
                            }
                        })
                .dimensions(centerX - buttonWidth / 2, buttonY,
                        buttonWidth, 22)
                .build());

        int sizeY = buttonY + 34;
        int sizeButtonWidth = 62;
        addDrawableChild(ButtonWidget.builder(soft("−"),
                        button -> {
                            appearance.changeScale(-0.05F);
                            init();
                        })
                .dimensions(centerX - 80, sizeY, sizeButtonWidth, 22)
                .build());

        addDrawableChild(ButtonWidget.builder(sizeButtonText(),
                        button -> { })
                .dimensions(centerX - 12, sizeY, 72, 22)
                .build());

        addDrawableChild(ButtonWidget.builder(soft("+"),
                        button -> {
                            appearance.changeScale(0.05F);
                            init();
                        })
                .dimensions(centerX + 66, sizeY, sizeButtonWidth, 22)
                .build());

        int keyY = sizeY + 34;
        addDrawableChild(ButtonWidget.builder(menuKeyButtonText(),
                        button -> {
                            listeningForMenuKey = true;
                            button.setMessage(soft("PRESS A KEY..."));
                        })
                .dimensions(centerX - buttonWidth / 2, keyY,
                        buttonWidth, 22)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, BACKGROUND);

        int accent = appearance.getAccentColor();

        context.fill(panelX - 10, panelY - 10,
                panelX + panelWidth + 10, panelY + panelHeight + 10,
                (accent & 0x00FFFFFF) | 0x33000000);

        context.fill(panelX, panelY,
                panelX + panelWidth, panelY + panelHeight, PANEL);

        context.fill(panelX, panelY,
                panelX + panelWidth, panelY + 2, accent);
        context.fill(panelX, panelY + panelHeight - 2,
                panelX + panelWidth, panelY + panelHeight, accent);
        context.fill(panelX, panelY,
                panelX + 2, panelY + panelHeight, accent);
        context.fill(panelX + panelWidth - 2, panelY,
                panelX + panelWidth, panelY + panelHeight, accent);

        context.fill(panelX + 16, panelY + 16,
                panelX + panelWidth - 16, panelY + 58, CARD);

        context.drawTextWithShadow(textRenderer,
                soft("ASYNC  /  CLICKGUI"),
                panelX + 28, panelY + 25, WHITE);

        context.drawText(textRenderer,
                soft("APPEARANCE SETTINGS"),
                panelX + 28, panelY + 41, accent, false);

        context.drawTextWithShadow(textRenderer,
                soft("ClickGUI Color"),
                panelX + 28, panelY + 78, WHITE);

        context.drawText(textRenderer,
                soft("Choose any RGB color"),
                panelX + 28, panelY + 91, MUTED, false);

        context.drawTextWithShadow(textRenderer,
                soft("Size ClickGUI"),
                panelX + 28, panelY + 132, WHITE);

        context.drawText(textRenderer,
                soft("Adjust from 50% to 125%"),
                panelX + 28, panelY + 145, MUTED, false);

        context.drawTextWithShadow(textRenderer,
                soft("Menu key"),
                panelX + 28, panelY + 166, WHITE);

        context.drawText(textRenderer,
                soft("Change the key used to open this menu"),
                panelX + 28, panelY + 179, MUTED, false);

        context.fill(panelX + 18, panelY + panelHeight - 31,
                panelX + panelWidth - 18, panelY + panelHeight - 30,
                accent & 0x66FFFFFF);

        String footer = listeningForMenuKey
                ? "PRESS A KEY  •  ESC TO CANCEL"
                : "ESC  •  CLOSE MENU";

        context.drawCenteredTextWithShadow(textRenderer,
                soft(footer), width / 2,
                panelY + panelHeight - 20, accent);

        super.render(context, mouseX, mouseY, delta);

        if (listeningForMenuKey) {
            context.drawCenteredTextWithShadow(textRenderer,
                    soft("Waiting for a new menu key..."),
                    width / 2, panelY + panelHeight + 14, WHITE);
        }
    }

    private Text colorButtonText() {
        return soft(String.format(Locale.ROOT,
                "COLOR: #%06X", appearance.getAccentColor() & 0xFFFFFF));
    }

    private Text sizeButtonText() {
        return soft(String.format(Locale.ROOT,
                "%.0f%%", appearance.getScale() * 100.0F));
    }

    private Text menuKeyButtonText() {
        return soft("MENU KEY: " + keyName(config.getMenuKey()));
    }

    private String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) {
            return "NONE";
        }

        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) {
            return name.toUpperCase(Locale.ROOT);
        }

        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R SHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "L SHIFT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            default -> "KEY " + key;
        };
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (listeningForMenuKey) {
            int key = input.key();

            if (key == GLFW.GLFW_KEY_ESCAPE) {
                listeningForMenuKey = false;
                init();
                return true;
            }

            if (key == GLFW.GLFW_KEY_BACKSPACE
                    || key == GLFW.GLFW_KEY_DELETE) {
                key = GLFW.GLFW_KEY_UNKNOWN;
            }

            config.setMenuKey(key);
            listeningForMenuKey = false;
            init();
            return true;
        }

        return super.keyPressed(input);
    }
}
