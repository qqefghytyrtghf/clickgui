package dev.async;

import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AsyncAppearanceConfig {
    private final Path path = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("async-appearance.properties");

    private static final String[] ACCENT_NAMES = {
            "Фиолетовый", "Синий", "Бирюзовый", "Морская Волна",
            "Зелёный", "Лаймовый", "Жёлтый", "Оранжевый",
            "Красный", "Розовый", "Пурпурный", "Серый"
    };

    private static final int[] ACCENT_COLORS = {
            0xFF8E6BB8, // Фиолетовый
            0xFF5685D8, // Синий
            0xFF42B6C8, // Бирюзовый
            0xFF3DAE9A, // Морская Волна
            0xFF65A86B, // Зелёный
            0xFFA2B957, // Лаймовый
            0xFFC6A64B, // Жёлтый
            0xFFCC8045, // Оранжевый
            0xFFC45B65, // Красный
            0xFFD16F9F, // Розовый
            0xFFA65BC4, // Пурпурный
            0xFF8993A5  // Серый
    };

    private int accentColor = ACCENT_COLORS[0];
    private float scale = 1.0F;

    public AsyncAppearanceConfig() {
        load();
    }

    public int getAccentColor() {
        return accentColor;
    }

    public String getAccentColorName() {
        int index = findAccentIndex(accentColor);
        return index >= 0 ? ACCENT_NAMES[index] : ACCENT_NAMES[0];
    }

    public float getScale() {
        return scale;
    }

    public void setAccentColor(int color) {
        accentColor = 0xFF000000 | (color & 0x00FFFFFF);
        save();
    }

    public void nextAccentColor() {
        int currentIndex = findAccentIndex(accentColor);
        if (currentIndex < 0) {
            currentIndex = 0;
        }

        accentColor = ACCENT_COLORS[(currentIndex + 1) % ACCENT_COLORS.length];
        save();
    }

    private int findAccentIndex(int color) {
        int rgb = color & 0x00FFFFFF;
        for (int i = 0; i < ACCENT_COLORS.length; i++) {
            if ((ACCENT_COLORS[i] & 0x00FFFFFF) == rgb) {
                return i;
            }
        }
        return -1;
    }

    public void setScale(float value) {
        scale = Math.max(0.50F, Math.min(1.25F, value));
        scale = Math.round(scale * 100.0F) / 100.0F;
        save();
    }

    public void changeScale(float amount) {
        scale = Math.max(0.50F, Math.min(1.25F, scale + amount));
        scale = Math.round(scale * 100.0F) / 100.0F;
        save();
    }

    private void load() {
        if (!Files.exists(path)) {
            save();
            return;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);

            String color = properties.getProperty("accentColor");
            if (color != null) {
                accentColor = 0xFF000000
                        | ((int) Long.parseLong(color, 16) & 0x00FFFFFF);
            }

            String savedScale = properties.getProperty("scale");
            if (savedScale != null) {
                float parsed = Float.parseFloat(savedScale);
                if (Float.isFinite(parsed)) {
                    scale = Math.max(0.50F, Math.min(1.25F, parsed));
                }
            }
        } catch (Exception ignored) {
            accentColor = ACCENT_COLORS[0];
            scale = 1.0F;
        }
    }

    private void save() {
        Properties properties = new Properties();
        properties.setProperty("accentColor",
                String.format(java.util.Locale.ROOT, "%06X", accentColor & 0x00FFFFFF));
        properties.setProperty("scale", Float.toString(scale));

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                properties.store(writer, "Async ClickGUI appearance");
            }
        } catch (Exception ignored) {
        }
    }
}