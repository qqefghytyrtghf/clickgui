package dev.async;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class AsyncClient implements ClientModInitializer {

    private static AsyncConfig config;
    private static TickRate tickRate;

    @Override
    public void onInitializeClient() {
        Path configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("async.json");

        config = new AsyncConfig(configPath);
        config.load();

        tickRate = new TickRate(config);

        AsyncHud.register();

        AsyncKeyHandler keyHandler =
                new AsyncKeyHandler(tickRate, config);

        ClientTickEvents.END_CLIENT_TICK.register(tickRate::onEndTick);
        ClientTickEvents.END_CLIENT_TICK.register(keyHandler::onEndTick);
    }

    public static AsyncConfig getConfig() {
        return config;
    }

    public static TickRate getTickRate() {
        return tickRate;
    }
}
