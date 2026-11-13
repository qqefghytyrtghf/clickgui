package dev.async;

import net.minecraft.client.input.MouseInput;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.widget.SliderWidget;

import java.util.Locale;

public final class AsyncScreen extends Screen {

    private int ACCENT;
    private int ACCENT_LIGHT;
    private int ACCENT_DARK;

    private int PANEL;
    private int HEADER;
    private int CARD;
    private int CARD_HOVER;
    private int BUTTON;
    private int BUTTON_HOVER;
    private int BORDER;

    private static final int WHITE = 0xFFF8F0FF;
    private static final int MUTED = 0xFFA69AB5;
    private static final int GREEN = 0xFFBCA9D8;
    private static final int RED = 0xFF8F7BAA;

    private enum Page {
        HOME, COMBAT, HUD, SETTINGS
    }

    private final TickRate tickRate;
    private final AsyncConfig config;
    private final AsyncAppearanceConfig appearance;

    private Page page = Page.HOME;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    private int contentX;
    private int contentWidth;

    private int cardHeight;
    private int cardGap;
    private int firstCardY;
    private int secondCardY;
    private int thirdCardY;
    private int rightCardX;
    private int cardWidth;

    private int currentMouseX;
    private int currentMouseY;

    private boolean listeningForKey;
    private boolean listeningForMenuKey;
    private boolean distanceError;

    private DistanceSlider distanceSlider;
    private SizeSlider sizeSlider;
    private boolean sizeDragging;

    private static final Identifier GUI_FONT = Identifier.of("async", "gentle");

    /** Нежный шрифт ClickGUI (Quicksand Light). */
    private static Text soft(String value) {
        return Text.literal(value).styled(style ->
                style.withFont(new StyleSpriteSource.Font(GUI_FONT)));
    }


    public AsyncScreen(TickRate tickRate, AsyncConfig config) {
        super(soft("Async"));
        this.tickRate = tickRate;
        this.config = config;
        this.appearance = new AsyncAppearanceConfig();
        updatePalette();
    }

    private void updatePalette() {
        ACCENT = appearance.getAccentColor();
        ACCENT_LIGHT = blend(ACCENT, 0xFFFFFFFF, 0.48F);
        ACCENT_DARK = blend(ACCENT, 0xFF090610, 0.58F);

        PANEL = blend(0xFF100C19, ACCENT, 0.12F);
        HEADER = blend(0xFF171020, ACCENT, 0.20F);
        CARD = blend(0xFF171220, ACCENT, 0.16F);
        CARD_HOVER = blend(0xFF281936, ACCENT, 0.34F);
        BUTTON = blend(0xFF21172D, ACCENT, 0.24F);
        BUTTON_HOVER = blend(0xFF39234C, ACCENT, 0.48F);
        BORDER = blend(0xFF493D58, ACCENT, 0.62F);
    }

    private int blend(int first, int second, float amount) {
        int a = (int) (((first >>> 24) & 255) * (1.0F - amount) + ((second >>> 24) & 255) * amount);
        int r = (int) (((first >>> 16) & 255) * (1.0F - amount) + ((second >>> 16) & 255) * amount);
        int g = (int) (((first >>> 8) & 255) * (1.0F - amount) + ((second >>> 8) & 255) * amount);
        int b = (int) ((first & 255) * (1.0F - amount) + (second & 255) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    protected void init() {
        clearChildren();

        panelWidth = Math.min(880, width - 28);
        panelWidth = Math.max(300, panelWidth);

        panelHeight = Math.min(560, height - 24);
        panelHeight = Math.max(300, panelHeight);

        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        contentX = panelX + 22;
        contentWidth = panelWidth - 44;

        cardGap = 10;
        cardWidth = (contentWidth - cardGap) / 2;
        rightCardX = contentX + cardWidth + cardGap;

        cardHeight = panelHeight < 480 ? 60 : 70;

        firstCardY = panelY + 139;
        secondCardY = firstCardY + cardHeight + cardGap;
        thirdCardY = secondCardY + cardHeight + cardGap;

        int tabGap = 7;
        int tabWidth = (contentWidth - tabGap * 3) / 4;
        int tabY = panelY + 65;

        addNavigationButton("Main", Page.HOME, contentX, tabY, tabWidth);
        addNavigationButton("TriggerBot", Page.COMBAT,
                contentX + tabWidth + tabGap, tabY, tabWidth);
        addNavigationButton("HUD", Page.HUD,
                contentX + (tabWidth + tabGap) * 2, tabY, tabWidth);
        addNavigationButton("Настройки ClickGUI", Page.SETTINGS,
                contentX + (tabWidth + tabGap) * 3, tabY, tabWidth);

        switch (page) {
            case HOME -> buildHome();
            case COMBAT -> buildCombat();
            case HUD -> buildHud();
            case SETTINGS -> buildSettings();
        }
    }

    private void addNavigationButton(
            String label,
            Page target,
            int x,
            int y,
            int buttonWidth
    ) {
        addButton(soft(label), x, y, buttonWidth, 29,
                button -> changePage(target));
    }

    private void changePage(Page newPage) {
        page = newPage;
        distanceSlider = null;
        sizeSlider = null;
        sizeDragging = false;
        distanceError = false;
        listeningForKey = false;
        listeningForMenuKey = false;
        init();
    }

    private int controlY(int cardY) {
        return cardY + cardHeight - 27;
    }

    private void buildHome() {
        addButton(
                enabledText(),
                contentX + 12,
                firstCardY + cardHeight - 34,
                contentWidth - 24,
                25,
                button -> {
                    tickRate.toggleEnabled();
                    button.setMessage(enabledText());
                }
        );
    }

    private void buildCombat() {
        int buttonHeight = 23;

        addButton(attackText(), contentX + 9,
                controlY(firstCardY), cardWidth - 18, buttonHeight,
                button -> {
                    tickRate.toggleAttackMode();
                    button.setMessage(attackText());
                });

        addButton(targetText(), rightCardX + 9,
                controlY(firstCardY), cardWidth - 18, buttonHeight,
                button -> {
                    tickRate.toggleTargetMode();
                    button.setMessage(targetText());
                });

        addButton(weaponText(), contentX + 9,
                controlY(secondCardY), cardWidth - 18, buttonHeight,
                button -> {
                    tickRate.toggleWeaponMode();
                    button.setMessage(weaponText());
                });

        int distanceY = secondCardY + cardHeight - 24;
        int distanceX = rightCardX + 9;
        int applyWidth = 66;
        int applyX = rightCardX + cardWidth - applyWidth - 9;

        addButton(
                soft("Применить"),
                applyX,
                distanceY,
                applyWidth,
                buttonHeight,
                button -> applyDistance()
        );

        int sliderX = distanceX;
        int sliderRight = applyX - 7;
        int sliderWidth = Math.max(1, sliderRight - sliderX);

        distanceSlider = new DistanceSlider(
                sliderX,
                distanceY,
                sliderWidth,
                buttonHeight,
                config.getAttackDistance()
        );

        addDrawableChild(distanceSlider);

        addButton(keyToggleText(), contentX + 9,
                controlY(thirdCardY), cardWidth - 18, buttonHeight,
                button -> {
                    tickRate.toggleKeyToggleEnabled();
                    button.setMessage(keyToggleText());
                });

        addButton(triggerKeyText(), rightCardX + 9,
                controlY(thirdCardY), cardWidth - 18, buttonHeight,
                button -> {
                    listeningForKey = true;
                    listeningForMenuKey = false;
                        button.setMessage(soft("Нажмите клавишу..."));
                });
    }

    private void buildHud() {
        addButton(statusText(), contentX + 9,
                controlY(firstCardY), cardWidth - 18, 23,
                button -> {
                    config.setShowStatus(!config.isShowStatus());
                    button.setMessage(statusText());
                });
    }

    private void buildSettings() {
        addButton(soft("Цвет интерфейса: " + appearance.getAccentColorName()),
                contentX + 9, controlY(firstCardY), contentWidth - 18, 23,
                button -> {
                    appearance.nextAccentColor();
                    updatePalette();
                    button.setMessage(soft("Цвет интерфейса: "
                            + appearance.getAccentColorName()));
                });

        sizeSlider = new SizeSlider(
                contentX + 9,
                secondCardY + cardHeight - 24,
                contentWidth - 18,
                23,
                appearance.getScale()
        );

        addDrawableChild(sizeSlider);

        addButton(menuKeyText(), contentX + 9,
                controlY(thirdCardY), contentWidth - 18, 23,
                button -> {
                    listeningForMenuKey = true;
                    listeningForKey = false;
                        button.setMessage(soft("Нажмите клавишу..."));
                });
    }

    private void addButton(
            Text message,
            int x,
            int y,
            int buttonWidth,
            int buttonHeight,
            ButtonWidget.PressAction action
    ) {
        addDrawableChild(
                ButtonWidget.builder(message, action)
                        .dimensions(x, y, buttonWidth, buttonHeight)
                        .build()
        );
    }

    private void applyDistance() {
        if (distanceSlider == null) return;

        config.setAttackDistance(distanceSlider.getDistance());
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();

        if (listeningForKey || listeningForMenuKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listeningForKey = false;
                listeningForMenuKey = false;
                init();
                return true;
            }

            int key = (
                    keyCode == GLFW.GLFW_KEY_BACKSPACE
                            || keyCode == GLFW.GLFW_KEY_DELETE
            ) ? GLFW.GLFW_KEY_UNKNOWN : keyCode;

            if (listeningForMenuKey) {
                config.setMenuKey(key);
                listeningForMenuKey = false;
            } else {
                tickRate.setToggleKey(key);
                listeningForKey = false;
            }

            init();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        int x = panelX;
        int y = panelY;
        int right = x + panelWidth;
        int bottom = y + panelHeight;

        double scale = appearance.getScale();

        double scaledMouseX = width / 2.0
                + (mouseX - width / 2.0) / scale;

        double scaledMouseY = height / 2.0
                + (mouseY - height / 2.0) / scale;

        currentMouseX = (int) scaledMouseX;
        currentMouseY = (int) scaledMouseY;

        context.getMatrices().pushMatrix();

        context.getMatrices().translate(
                width / 2.0f,
                height / 2.0f
        );

        context.getMatrices().scale(
                (float) scale,
                (float) scale
        );

        context.getMatrices().translate(
                -width / 2.0f,
                -height / 2.0f
        );

        drawRounded(context, x, y, panelWidth, panelHeight, 12, PANEL);
        context.fill(x + 2, y + 2, x + panelWidth - 2, y + 50, HEADER);

        drawHeader(context, x, y, right);

        drawRounded(context,
                contentX - 7, y + 59,
                contentWidth + 14, 39, 7, HEADER);

        drawPageHeading(context, y);

        switch (page) {
            case HOME -> drawHome(context);
            case COMBAT -> drawCombat(context);
            case HUD -> drawHud(context);
            case SETTINGS -> drawSettings(context);
        }

        drawFooter(context, x, y, right, bottom);

        super.render(
                context,
                (int) scaledMouseX,
                (int) scaledMouseY,
                delta
        );

        drawCustomButtons(context);

        if (page == Page.COMBAT && distanceSlider != null) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    soft(String.format(
                            Locale.ROOT,
                            "Дистанция: %.1f",
                            distanceSlider.getDistance()
                    )),
                    distanceSlider.getX() + distanceSlider.getWidth() / 2,
                    distanceSlider.getY() - 13,
                    WHITE
            );
        }

        if (page == Page.SETTINGS && sizeSlider != null) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    soft("Размер: " + sizeSlider.getPercent() + "%"),
                    sizeSlider.getX() + sizeSlider.getWidth() / 2,
                    sizeSlider.getY() - 13,
                    WHITE
            );
        }

        if (distanceError && page == Page.COMBAT) {
            context.drawTextWithShadow(
                    textRenderer,
                    soft("Допустимое значение: 0.5–4.0"),
                    rightCardX + 10,
                    secondCardY + cardHeight + 2,
                    RED
            );
        }

        if (listeningForKey || listeningForMenuKey) {
            drawKeyListening(context);
        }

        context.getMatrices().popMatrix();
    }

    private void drawSimpleBorder(
            DrawContext context,
            int x,
            int y,
            int w,
            int h
    ) {
        context.fill(x, y, x + w, y + 1, BORDER);
        context.fill(x, y + h - 1, x + w, y + h, BORDER);
        context.fill(x, y, x + 1, y + h, BORDER);
        context.fill(x + w - 1, y, x + w, y + h, BORDER);
    }

    private void drawHeader(
            DrawContext context,
            int x,
            int y,
            int right
    ) {
        drawRounded(context,
                x + 17, y + 10, 20, 20, 6,
                ACCENT_DARK);

        context.drawCenteredTextWithShadow(
                textRenderer,
                soft("A"),
                x + 27,
                y + 16,
                ACCENT_LIGHT
        );

        context.drawTextWithShadow(
                textRenderer,
                soft("Async"),
                x + 45,
                y + 10,
                WHITE
        );

        context.drawText(
                textRenderer,
                soft("ClickGUI"),
                x + 45,
                y + 24,
                ACCENT_LIGHT,
                false
        );

        String version = "v2.0";

        context.drawText(
                textRenderer,
                soft(version),
                right - textRenderer.getWidth(version) - 19,
                y + 16,
                ACCENT_LIGHT,
                false
        );

        context.drawText(
                textRenderer,
                soft("TriggerClient"),
                x + 19,
                y + 39,
                MUTED,
                false
        );
    }

    private void drawPageHeading(DrawContext context, int y) {
        String title = switch (page) {
            case HOME -> "Main";
            case COMBAT -> "TriggerBot";
            case HUD -> "HUD";
            case SETTINGS -> "Настройки ClickGUI";
        };

        String subtitle = switch (page) {
            case HOME -> "Управление модом и его функциями.";
            case COMBAT -> "Настройка параметров боя.";
            case HUD -> "Настройка отображения интерфейса.";
            case SETTINGS -> "Настройки внешнего вида Async.";
        };

        context.drawTextWithShadow(
                textRenderer,
                soft(title),
                contentX,
                y + 110,
                ACCENT_LIGHT
        );

        context.drawText(
                textRenderer,
                soft(subtitle),
                contentX,
                y + 124,
                MUTED,
                false
        );
    }

    private void drawHome(DrawContext context) {
        drawControlCard(context,
                contentX, firstCardY, contentWidth, cardHeight,
                "TriggerBot", "Текущее состояние модуля",
                tickRate.isEnabled() ? GREEN : RED);

        int overviewY = secondCardY + 5;

        context.drawText(
                textRenderer,
                soft("Состояние системы"),
                contentX,
                overviewY,
                ACCENT_LIGHT,
                false
        );

        context.drawText(
                textRenderer,
                attackText(),
                contentX,
                overviewY + 15,
                MUTED,
                false
        );

        context.drawText(
                textRenderer,
                targetText(),
                contentX + 145,
                overviewY + 15,
                MUTED,
                false
        );

        context.drawText(
                textRenderer,
                weaponText(),
                contentX + 285,
                overviewY + 15,
                MUTED,
                false
        );

        String distance = String.format(
                Locale.ROOT,
                "%.1f",
                config.getAttackDistance()
        );

        context.drawText(
                textRenderer,
                soft("Дистанция атаки: " + distance),
                contentX,
                overviewY + 29,
                ACCENT_LIGHT,
                false
        );
    }

    private void drawCombat(DrawContext context) {
        drawControlCard(context,
                contentX, firstCardY, cardWidth, cardHeight,
                "Режим", "Выбранный режим атаки", ACCENT);

        drawControlCard(context,
                rightCardX, firstCardY, cardWidth, cardHeight,
                "Цель", "Выбранная цель", ACCENT);

        drawControlCard(context,
                contentX, secondCardY, cardWidth, cardHeight,
                "Режим оружия", "Выбранное оружие", ACCENT);

        drawControlCard(context,
                rightCardX, secondCardY, cardWidth, cardHeight,
                "Дистанция атаки", "Допустимо: 0.5–4.0", ACCENT);

        drawControlCard(context,
                contentX, thirdCardY, cardWidth, cardHeight,
                "Переключение клавишей", "Включение и выключение",
                tickRate.isKeyToggleEnabled() ? GREEN : RED);

        drawControlCard(context,
                rightCardX, thirdCardY, cardWidth, cardHeight,
                "Клавиша активации", "Назначенная клавиша", ACCENT);
    }

    private void drawHud(DrawContext context) {
        drawControlCard(context,
                contentX, firstCardY, cardWidth, cardHeight,
                "Отображение статуса", "Показывать надпись вкл./выкл.",
                config.isShowStatus() ? GREEN : RED);
    }

    private void drawSettings(DrawContext context) {
        String color = String.format(
                Locale.ROOT,
                "#%06X",
                appearance.getAccentColor() & 0xFFFFFF
        );

        String size = Math.round(
                appearance.getScale() * 100.0F
        ) + "%";

        drawControlCard(context,
                contentX, firstCardY, contentWidth, cardHeight,
                "Цвет ClickGUI", "Выбранный цвет: " + color, ACCENT);

        drawControlCard(context,
                contentX, secondCardY, contentWidth, cardHeight,
                "Размер ClickGUI", "Текущий размер: " + size, ACCENT);

        drawControlCard(context,
                contentX, thirdCardY, contentWidth, cardHeight,
                "Клавиша меню", "Клавиша открытия меню", ACCENT);
    }

    private void drawControlCard(
            DrawContext context,
            int x,
            int y,
            int w,
            int h,
            String title,
            String subtitle,
            int accent
    ) {
        boolean hovered = mouseInside(x, y, w, h);

        drawRounded(context, x, y, w, h, 8,
                hovered ? CARD_HOVER : CARD);

        context.drawTextWithShadow(
                textRenderer,
                soft(title),
                x + 11,
                y + 8,
                WHITE
        );

        context.drawText(
                textRenderer,
                soft(subtitle),
                x + 11,
                y + 21,
                MUTED,
                false
        );
    }

    private void drawFooter(
            DrawContext context,
            int x,
            int y,
            int right,
            int bottom
    ) {
        drawRounded(context,
                x + 19, bottom - 24, 8, 8, 3,
                tickRate.isEnabled() ? GREEN : RED);

        String status = tickRate.isEnabled()
                ? "Мод включён"
                : "Мод выключен";

        context.drawText(
                textRenderer,
                soft(status),
                x + 34,
                bottom - 24,
                WHITE,
                false
        );

        String minecraft = " by qqef";

        context.drawText(
                textRenderer,
                soft(minecraft),
                x + panelWidth / 2
                        - textRenderer.getWidth(minecraft) / 2,
                bottom - 24,
                MUTED,
                false
        );

        String footer = "Async v2.0";

        context.drawText(
                textRenderer,
                soft(footer),
                right - textRenderer.getWidth(footer) - 19,
                bottom - 24,
                ACCENT_LIGHT,
                false
        );
    }

    private void drawCustomButtons(DrawContext context) {
        for (var child : children()) {
            if (!(child instanceof ButtonWidget button)) {
                continue;
            }

            int bx = button.getX();
            int by = button.getY();
            int bw = button.getWidth();
            int bh = button.getHeight();

            boolean hovered = button.isHovered();
            boolean selected = isSelected(button);

            int fill = hovered ? BUTTON_HOVER : BUTTON;
            int edge = hovered || selected ? ACCENT : BORDER;

            drawRounded(context,
                    bx - 1, by - 1,
                    bw + 2, bh + 2,
                    6, edge);

            drawRounded(context,
                    bx, by, bw, bh, 5, fill);

            if (selected) {
                drawRounded(context,
                        bx + 5, by + 5,
                        3, bh - 10, 2,
                        ACCENT_LIGHT);
            }

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    button.getMessage(),
                    bx + bw / 2,
                    by + (bh - 8) / 2,
                    hovered || selected ? ACCENT_LIGHT : WHITE
            );
        }
    }

    private void drawKeyListening(DrawContext context) {
        String message = "Нажмите клавишу  |  ESC — отмена";

        int messageWidth = textRenderer.getWidth(message) + 30;
        int messageX = (width - messageWidth) / 2;
        int messageY = height - 37;

        drawRounded(context,
                messageX, messageY,
                messageWidth, 21,
                6, PANEL);

        drawSimpleBorder(context,
                messageX, messageY,
                messageWidth, 21);

        context.drawCenteredTextWithShadow(
                textRenderer,
                soft(message),
                width / 2,
                messageY + 7,
                ACCENT_LIGHT
        );
    }

    private void drawRounded(
            DrawContext context,
            int x,
            int y,
            int w,
            int h,
            int radius,
            int color
    ) {
        if (w <= 0 || h <= 0) return;

        int r = Math.min(radius, Math.min(w / 2, h / 2));

        for (int row = 0; row < h; row++) {
            int inset = 0;

            if (row < r) {
                int dy = r - row;
                inset = r - (int) Math.sqrt(
                        Math.max(0, r * r - dy * dy));
            } else if (row >= h - r) {
                int dy = row - (h - r - 1);
                inset = r - (int) Math.sqrt(
                        Math.max(0, r * r - dy * dy));
            }

            context.fill(
                    x + inset,
                    y + row,
                    x + w - inset,
                    y + row + 1,
                    color
            );
        }
    }

    private boolean mouseInside(int x, int y, int w, int h) {
        return currentMouseX >= x
                && currentMouseX <= x + w
                && currentMouseY >= y
                && currentMouseY <= y + h;
    }

    private boolean isSelected(ButtonWidget button) {
        String message = button.getMessage().getString();

        return switch (page) {
            case HOME -> message.equals("Main");
            case COMBAT -> message.equals("TriggerBot");
            case HUD -> message.equals("HUD");
            case SETTINGS -> message.equals("Настройки ClickGUI");
        };
    }

    private Text enabledText() {
        return soft(
                tickRate.isEnabled()
                        ? "TriggerBot: Вкл."
                        : "TriggerBot: Выкл."
        );
    }

    private Text attackText() {
        String mode = switch (tickRate.getAttackMode()) {
            case NORMAL -> "Комбо";
            case FAST -> "Криты";
        };

        return soft("Режим: " + mode);
    }

    private Text targetText() {
        return soft(
                "Цель: " + (
                        tickRate.getMode() == TargetMode.MOBS
                                ? "Мобы"
                                : "Игроки"
                )
        );
    }

    private Text weaponText() {
        return soft(
                "Оружие: " + (
                        tickRate.getWeaponMode()
                                == TickWeapon.WEAPONS_ONLY
                                ? "Только оружие"
                                : "Любой предмет"
                )
        );
    }

    private Text keyToggleText() {
        return soft(
                "Переключение: "
                        + (tickRate.isKeyToggleEnabled() ? "Вкл." : "Выкл.")
        );
    }

    private Text triggerKeyText() {
        return soft(
                "Клавиша: " + keyName(tickRate.getToggleKey())
        );
    }

    private Text menuKeyText() {
        return soft(
                "Клавиша меню: " + keyName(config.getMenuKey())
        );
    }

    private Text statusText() {
        return soft(
                "Статус: "
                        + (config.isShowStatus() ? "Вкл." : "Выкл.")
        );
    }

    private String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) {
            return "Не назначена";
        }

        String name = GLFW.glfwGetKeyName(key, 0);

        if (name != null) {
            return name.toUpperCase(Locale.ROOT);
        }

        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "Правый Shift";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "Левый Shift";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "Правый Ctrl";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "Левый Ctrl";
            case GLFW.GLFW_KEY_SPACE -> "Пробел";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            default -> "Клавиша " + key;
        };
    }

    private Click transformClick(Click click) {
        double scale = appearance.getScale();

        double x = width / 2.0
                + (click.x() - width / 2.0) / scale;

        double y = height / 2.0
                + (click.y() - height / 2.0) / scale;

        return new Click(x, y,
                new MouseInput(click.button(), 0));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Click transformed = transformClick(click);
        boolean result = super.mouseClicked(transformed, doubled);

        if (sizeSlider != null
                && sizeSlider.isMouseOver(transformed.x(), transformed.y())) {
            sizeDragging = true;
        }

        return result;
    }

    @Override
    public boolean mouseReleased(Click click) {
        boolean result = super.mouseReleased(transformClick(click));

        if (sizeDragging) {
            sizeDragging = false;

            if (sizeSlider != null) {
                sizeSlider.commit();
            }
        }

        return result;
    }

    @Override
    public boolean mouseDragged(
            Click click,
            double deltaX,
            double deltaY
    ) {
        double scale = appearance.getScale();

        return super.mouseDragged(
                transformClick(click),
                deltaX / scale,
                deltaY / scale
        );
    }

    @Override
    public void close() {
        tickRate.suppressNextAttack();
        super.close();
    }

    private final class DistanceSlider extends SliderWidget {

        private DistanceSlider(
                int x,
                int y,
                int width,
                int height,
                double distance
        ) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Text.empty(),
                    (distance - 0.5) / 3.5
            );

            updateMessage();
        }

        @Override
        public void renderWidget(
                DrawContext context,
                int mouseX,
                int mouseY,
                float delta
        ) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();

            int background = isHovered()
                    ? BUTTON_HOVER : BUTTON;

            drawRounded(
                    context,
                    x, y, width, height,
                    6, background
            );

            int trackLeft = x + 7;
            int trackRight = x + width - 7;
            int trackWidth = Math.max(1, trackRight - trackLeft);
            int trackY = y + height - 5;

            int filledWidth = (int) Math.round(trackWidth * value);

            drawRounded(
                    context,
                    trackLeft, trackY,
                    trackWidth, 3,
                    2, BORDER
            );

            if (filledWidth > 0) {
                drawRounded(
                        context,
                        trackLeft, trackY,
                        filledWidth, 3,
                        2, ACCENT
                );
            }

            int thumbX = trackLeft + filledWidth - 4;

            thumbX = Math.max(
                    trackLeft - 1,
                    Math.min(trackRight - 7, thumbX)
            );

            drawRounded(
                    context,
                    thumbX - 1, y + 3,
                    10, height - 7,
                    4, ACCENT_DARK
            );

            drawRounded(
                    context,
                    thumbX, y + 4,
                    8, height - 9,
                    3, ACCENT_LIGHT
            );
        }

        private double getDistance() {
            int step = (int) Math.round(value * 35.0);
            return 0.5 + step * 0.1;
        }

        @Override
        protected void updateMessage() {
            setMessage(soft(String.format(
                    Locale.ROOT,
                    "Дистанция: %.1f",
                    getDistance()
            )));
        }

        @Override
        protected void applyValue() {
            double distance = getDistance();

            config.setAttackDistance(distance);
        }

        private void setDistance(double distance) {
            double normalized = (distance - 0.5) / 3.5;
            setValue(normalized);
            updateMessage();
        }
    }

    private final class SizeSlider extends SliderWidget {

        private static final int STEPS = 15;

        private SizeSlider(
                int x,
                int y,
                int width,
                int height,
                double scale
        ) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Text.empty(),
                    (scale - 0.5) / 0.75
            );

            updateMessage();
        }

        @Override
        public void renderWidget(
                DrawContext context,
                int mouseX,
                int mouseY,
                float delta
        ) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();

            int background = isHovered()
                    ? BUTTON_HOVER : BUTTON;

            drawRounded(
                    context,
                    x, y, width, height,
                    6, background
            );

            int trackLeft = x + 7;
            int trackRight = x + width - 7;
            int trackWidth = Math.max(1, trackRight - trackLeft);
            int trackY = y + height - 5;

            int filledWidth = (int) Math.round(trackWidth * value);

            drawRounded(
                    context,
                    trackLeft, trackY,
                    trackWidth, 3,
                    2, BORDER
            );

            if (filledWidth > 0) {
                drawRounded(
                        context,
                        trackLeft, trackY,
                        filledWidth, 3,
                        2, ACCENT
                );
            }

            int thumbX = trackLeft + filledWidth - 4;

            thumbX = Math.max(
                    trackLeft - 1,
                    Math.min(trackRight - 7, thumbX)
            );

            drawRounded(
                    context,
                    thumbX - 1, y + 3,
                    10, height - 7,
                    4, ACCENT_DARK
            );

            drawRounded(
                    context,
                    thumbX, y + 4,
                    8, height - 9,
                    3, ACCENT_LIGHT
            );
        }

        private int getPercent() {
            int step = (int) Math.round(value * STEPS);
            return 50 + step * 5;
        }

        private void commit() {
            appearance.setScale(getPercent() / 100.0F);
        }

        @Override
        protected void updateMessage() {
            setMessage(soft("Размер: " + getPercent() + "%"));
        }

        @Override
        protected void applyValue() {
            // While the mouse is held the GUI is not rescaled, because
            // rescaling under the cursor makes the thumb jump around.
            // The scale is applied on release (see mouseReleased).
            if (!sizeDragging) {
                commit();
            }
        }
    }
}