package dev.async;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class AsyncHud {
    private static boolean initialized = false;
    private static boolean lastEnabled = false;
    private static long notificationUntil = 0L;

    private static int hudX = -1;
    private static int hudY = -1;

    private static boolean dragging = false;
    private static boolean wasLeftDown = false;
    private static double dragOffsetX;
    private static double dragOffsetY;

    private static final float TEXT_SCALE = 1.0f;

    private AsyncHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(AsyncHud::render);
    }

    private static void render(
            DrawContext context,
            net.minecraft.client.render.RenderTickCounter tickCounter
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        AsyncConfig config = AsyncClient.getConfig();

        if (config == null || client.player == null || client.world == null) {
            initialized = false;
            dragging = false;
            wasLeftDown = false;
            return;
        }

        boolean enabled = config.isEnabled();
        long now = System.currentTimeMillis();

        if (!initialized) {
            lastEnabled = enabled;
            initialized = true;
        } else if (enabled != lastEnabled) {
            lastEnabled = enabled;
            notificationUntil = now + 700L;
        }

        if (!config.isShowStatus()) {
            dragging = false;
            wasLeftDown = false;
            return;
        }

        Text status = Text.literal(
                enabled ? "TriggerBot On" : "TriggerBot Off"
        );

        int originalWidth = client.textRenderer.getWidth(status);
        int originalHeight = client.textRenderer.fontHeight;

        int textWidth = Math.max(1, (int) Math.ceil(originalWidth * TEXT_SCALE));
        int textHeight = Math.max(1, (int) Math.ceil(originalHeight * TEXT_SCALE));

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        if (hudX < 0 || hudY < 0) {
            hudX = config.getHudX();
            hudY = config.getHudY();

            if (hudX < 0 || hudY < 0) {
                hudX = (screenWidth - textWidth) / 2;
                hudY = screenHeight / 2 + 14;
            }
        }

        hudX = Math.max(0, Math.min(hudX, screenWidth - textWidth));
        hudY = Math.max(0, Math.min(hudY, screenHeight - textHeight));

        boolean chatOpen = client.currentScreen instanceof ChatScreen;

        long window = client.getWindow().getHandle();
        boolean leftDown = GLFW.glfwGetMouseButton(
                window,
                GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) == GLFW.GLFW_PRESS;

        double mouseX = client.mouse.getX()
                * screenWidth / (double) client.getWindow().getWidth();

        double mouseY = client.mouse.getY()
                * screenHeight / (double) client.getWindow().getHeight();

        if (chatOpen) {
            boolean hovering = mouseX >= hudX
                    && mouseX <= hudX + textWidth
                    && mouseY >= hudY
                    && mouseY <= hudY + textHeight;

            if (leftDown && !wasLeftDown && hovering) {
                dragging = true;
                dragOffsetX = mouseX - hudX;
                dragOffsetY = mouseY - hudY;
            }

            if (!leftDown && dragging) {
                dragging = false;
                config.setHudPosition(hudX, hudY);
            }

            if (dragging && leftDown) {
                hudX = (int) (mouseX - dragOffsetX);
                hudY = (int) (mouseY - dragOffsetY);

                hudX = Math.max(0, Math.min(hudX, screenWidth - textWidth));
                hudY = Math.max(0, Math.min(hudY, screenHeight - textHeight));
            }
        } else {
            if (dragging) {
                config.setHudPosition(hudX, hudY);
            }
            dragging = false;
        }

        wasLeftDown = leftDown;

        boolean showInGame = now <= notificationUntil;

        if (!chatOpen && !showInGame) {
            return;
        }

        int color = enabled ? 0xFF69F0AE : 0xFFFF6B6B;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(hudX, hudY);
        context.getMatrices().scale(TEXT_SCALE, TEXT_SCALE);

        context.drawTextWithShadow(
                client.textRenderer,
                status,
                0,
                0,
                color
        );

        context.getMatrices().popMatrix();
    }
}
