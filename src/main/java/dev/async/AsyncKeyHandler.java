package dev.async;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public final class AsyncKeyHandler {
    private final TickRate tickRate;
    private final AsyncConfig config;

    private boolean menuKeyDown;
    private boolean toggleKeyDown;

    public AsyncKeyHandler(TickRate tickRate, AsyncConfig config) {
        this.tickRate = tickRate;
        this.config = config;
    }

    public void onEndTick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            menuKeyDown = false;
            toggleKeyDown = false;
            return;
        }

        long window = client.getWindow().getHandle();

        int configuredMenuKey = config.getMenuKey();
        boolean menuDown = configuredMenuKey >= 0
                && GLFW.glfwGetKey(window, configuredMenuKey) == GLFW.GLFW_PRESS;

        boolean openMenu = menuDown && !menuKeyDown;
        menuKeyDown = menuDown;

        if (openMenu && client.currentScreen == null) {
            tickRate.suppressNextAttack();
            client.setScreen(new AsyncScreen(tickRate, config));
        }

        int key = config.getToggleKey();
        boolean down = config.isKeyToggleEnabled()
                && key >= 0
                && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;

        if (down && !toggleKeyDown && client.currentScreen == null) {
            tickRate.toggleEnabled();
        }

        toggleKeyDown = down;
    }
}
