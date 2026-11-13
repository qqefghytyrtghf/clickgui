
package dev.async;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AsyncConfig {
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private final Path path;

    private boolean enabled = false;
    private TargetMode targetMode = TargetMode.MOBS;
    private TickMode attackMode = TickMode.NORMAL;
    private TickWeapon weaponMode = TickWeapon.WEAPONS_ONLY;
    private boolean keyToggleEnabled = true;
    private int toggleKey = GLFW.GLFW_KEY_R;
    private int menuKey = GLFW.GLFW_KEY_L;
    private double attackDistance = 3.0;
    private boolean showStatus = true;

    private int hudX = -1;
    private int hudY = -1;

    public AsyncConfig(Path path) {
        this.path = path;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public TargetMode getTargetMode() {
        return targetMode;
    }

    public TickMode getAttackMode() {
        return attackMode;
    }

    public TickWeapon getWeaponMode() {
        return weaponMode;
    }

    public boolean isKeyToggleEnabled() {
        return keyToggleEnabled;
    }

    public int getToggleKey() {
        return toggleKey;
    }

    public int getMenuKey() {
        return menuKey;
    }

    public double getAttackDistance() {
        return attackDistance;
    }

    public boolean isShowStatus() {
        return showStatus;
    }

    public int getHudX() {
        return hudX;
    }

    public int getHudY() {
        return hudY;
    }

    public void setHudPosition(int x, int y) {
        hudX = Math.max(0, x);
        hudY = Math.max(0, y);
        save();
    }

    public void setEnabled(boolean value) {
        enabled = value;
        save();
    }

    public void setTargetMode(TargetMode value) {
        targetMode = value;
        save();
    }

    public void setAttackMode(TickMode value) {
        attackMode = value;
        save();
    }

    public void setWeaponMode(TickWeapon value) {
        weaponMode = value;
        save();
    }

    public void setKeyToggleEnabled(boolean value) {
        keyToggleEnabled = value;
        save();
    }

    public void setToggleKey(int value) {
        toggleKey = isValidKey(value) ? value : GLFW.GLFW_KEY_R;
        save();
    }

    public void setMenuKey(int value) {
        menuKey = isValidKey(value) ? value : GLFW.GLFW_KEY_L;
        save();
    }

    public void setAttackDistance(double value) {
        if (Double.isFinite(value) && value >= 0.5 && value <= 4.0) {
            attackDistance = value;
            save();
        }
    }

    public void setShowStatus(boolean value) {
        showStatus = value;
        save();
    }

    private static boolean isValidKey(int key) {
        return key == GLFW.GLFW_KEY_UNKNOWN
                || (key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST);
    }

    public void load() {
        if (!Files.exists(path)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();

            if (o.has("enabled")) {
                enabled = o.get("enabled").getAsBoolean();
            }

            if (o.has("mode")) {
                targetMode = parse(TargetMode.class,
                        o.get("mode").getAsString(), TargetMode.MOBS);
            }

            if (o.has("targetMode")) {
                targetMode = parse(TargetMode.class,
                        o.get("targetMode").getAsString(), targetMode);
            }

            if (o.has("attackMode")) {
                attackMode = parse(TickMode.class,
                        o.get("attackMode").getAsString(), TickMode.NORMAL);
            }

            if (o.has("weaponMode")) {
                weaponMode = parse(TickWeapon.class,
                        o.get("weaponMode").getAsString(), TickWeapon.WEAPONS_ONLY);
            }

            if (o.has("keyToggleEnabled")) {
                keyToggleEnabled = o.get("keyToggleEnabled").getAsBoolean();
            }

            if (o.has("toggleKey")) {
                toggleKey = o.get("toggleKey").getAsInt();
            }

            if (o.has("menuKey")) {
                menuKey = o.get("menuKey").getAsInt();
            }

            if (o.has("attackDistance")) {
                double loadedDistance = o.get("attackDistance").getAsDouble();
                if (Double.isFinite(loadedDistance)
                        && loadedDistance >= 0.5
                        && loadedDistance <= 4.0) {
                    attackDistance = loadedDistance;
                }
            }

            if (o.has("showStatus")) {
                showStatus = o.get("showStatus").getAsBoolean();
            }

            if (o.has("hudX")) {
                hudX = Math.max(0, o.get("hudX").getAsInt());
            }

            if (o.has("hudY")) {
                hudY = Math.max(0, o.get("hudY").getAsInt());
            }

            if (!isValidKey(toggleKey)) {
                toggleKey = GLFW.GLFW_KEY_R;
            }

            if (!isValidKey(menuKey)) {
                menuKey = GLFW.GLFW_KEY_L;
            }
        } catch (Exception ignored) {
            enabled = false;
            targetMode = TargetMode.MOBS;
            attackMode = TickMode.NORMAL;
            weaponMode = TickWeapon.WEAPONS_ONLY;
            keyToggleEnabled = true;
            toggleKey = GLFW.GLFW_KEY_R;
            menuKey = GLFW.GLFW_KEY_L;
            attackDistance = 3.0;
            showStatus = true;
            hudX = -1;
            hudY = -1;
        }

        save();
    }

    private static <T extends Enum<T>> T parse(
            Class<T> type,
            String value,
            T fallback
    ) {
        try {
            return Enum.valueOf(type, value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void save() {
        JsonObject o = new JsonObject();

        o.addProperty("enabled", enabled);
        o.addProperty("targetMode", targetMode.name());
        o.addProperty("attackMode", attackMode.name());
        o.addProperty("weaponMode", weaponMode.name());
        o.addProperty("keyToggleEnabled", keyToggleEnabled);
        o.addProperty("toggleKey", toggleKey);
        o.addProperty("menuKey", menuKey);
        o.addProperty("attackDistance", attackDistance);
        o.addProperty("showStatus", showStatus);
        o.addProperty("hudX", hudX);
        o.addProperty("hudY", hudY);

        Path parent = path.getParent();
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");

        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(o, writer);
            }

            try {
                Files.move(tmp, path,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
        }
    }
}
