package trupp.ware.config;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import trupp.ware.truppware.module.Manager;
import trupp.ware.truppware.module.Module;
import trupp.ware.truppware.module.settings.BooleanSetting;
import trupp.ware.truppware.module.settings.ModeSetting;
import trupp.ware.truppware.module.settings.NumberSetting;
import trupp.ware.truppware.module.settings.Setting;

import java.io.*;
import java.util.List;

public class ConfigManager {

    private static File CONFIG_FOLDER;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static void initFolder() {
        if (CONFIG_FOLDER != null) return;

        File gameDir = Minecraft.getInstance().gameDirectory;
        CONFIG_FOLDER = new File(gameDir, "config/truppware");
        CONFIG_FOLDER.mkdirs();
    }

    public static void save(String name) {
        initFolder();

        try {
            File file = new File(CONFIG_FOLDER, name + ".json");
            JsonObject root = new JsonObject();

            for (Module m : Manager.trupp.modules) {
                JsonObject obj = new JsonObject();
                obj.addProperty("toggled", m.getToggled());
                obj.addProperty("key", m.getKey());

                for (Setting s : m.getSettings()) {
                    if (s instanceof BooleanSetting b)
                        obj.addProperty(s.getName(), b.getValue());
                    else if (s instanceof NumberSetting n)
                        obj.addProperty(s.getName(), n.getNum());
                    else if (s instanceof ModeSetting mode)
                        obj.addProperty(s.getName(), mode.getCurrentMode());
                }

                root.add(m.getName(), obj);
            }

            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(root, writer);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void autoSave() {

            save("latest");

    }

    public static void load(String name) {
        initFolder();

        File file = new File(CONFIG_FOLDER, name + ".json");
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            for (Module m : Manager.trupp.modules) {
                if (!root.has(m.getName())) continue;

                JsonObject obj = root.getAsJsonObject(m.getName());
                if (obj.has("toggled")) m.setToggled(obj.get("toggled").getAsBoolean());
                if (obj.has("key")) m.setKey(obj.get("key").getAsInt());

                for (Setting s : m.getSettings()) {
                    if (!obj.has(s.getName())) continue;

                    if (s instanceof BooleanSetting b)
                        b.setEnabled(obj.get(s.getName()).getAsBoolean());
                    else if (s instanceof NumberSetting n)
                        n.setValue(obj.get(s.getName()).getAsDouble());
                    else if (s instanceof ModeSetting mode)
                        mode.currentMode = obj.get(s.getName()).getAsString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



