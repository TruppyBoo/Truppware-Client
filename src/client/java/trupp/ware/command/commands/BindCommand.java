package trupp.ware.command.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import trupp.ware.command.Command;
import trupp.ware.truppware.module.Manager;
import trupp.ware.truppware.module.Module;

import java.util.HashMap;
import java.util.Map;

public class BindCommand extends Command {

    private static final Map<String, Integer> KEY_MAP = new HashMap<>();

    static {
        // Letters
        for (char c = 'A'; c <= 'Z'; c++) {
            KEY_MAP.put(String.valueOf(c), GLFW.GLFW_KEY_A + (c - 'A'));
        }

        // Numbers
        for (int i = 0; i <= 9; i++) {
            KEY_MAP.put(String.valueOf(i), GLFW.GLFW_KEY_0 + i);
        }

        // Common special keys
        KEY_MAP.put("SPACE", GLFW.GLFW_KEY_SPACE);
        KEY_MAP.put("SHIFT", GLFW.GLFW_KEY_LEFT_SHIFT);
        KEY_MAP.put("CTRL", GLFW.GLFW_KEY_LEFT_CONTROL);
        KEY_MAP.put("ALT", GLFW.GLFW_KEY_LEFT_ALT);
        KEY_MAP.put("TAB", GLFW.GLFW_KEY_TAB);
        KEY_MAP.put("ENTER", GLFW.GLFW_KEY_ENTER);
        KEY_MAP.put("ESC", GLFW.GLFW_KEY_ESCAPE);
        KEY_MAP.put("UP", GLFW.GLFW_KEY_UP);
        KEY_MAP.put("DOWN", GLFW.GLFW_KEY_DOWN);
        KEY_MAP.put("LEFT", GLFW.GLFW_KEY_LEFT);
        KEY_MAP.put("RIGHT", GLFW.GLFW_KEY_RIGHT);
    }

    public BindCommand() {
        super("bind");
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();

        if (args.length < 2) {
            mc.player.displayClientMessage(
                    Component.literal("§cUsage: .bind <module> <key>"), false
            );
            return;
        }

        String moduleName = args[0];
        String keyName = args[1].toUpperCase();

        // FIND MODULE IGNORING CASE
        Module targetModule = null;
        for (Module m : Manager.trupp.modules) {
            if (m.name.equalsIgnoreCase(moduleName)) {
                targetModule = m;
                break;
            }
        }

        if (targetModule == null) {
            mc.player.displayClientMessage(
                    Component.literal("§cModule not found: " + moduleName), false
            );
            return;
        }

        int keyCode;

        // Unbind
        if (keyName.equals("NONE")) {
            keyCode = -1;
        } else if (KEY_MAP.containsKey(keyName)) {
            keyCode = KEY_MAP.get(keyName);
        } else {
            mc.player.displayClientMessage(
                    Component.literal("§cInvalid key: " + keyName), false
            );
            return;
        }

        targetModule.setKey(keyCode);

        String bindText = keyCode == -1 ? "Unbound" : keyName;
        mc.player.displayClientMessage(
                Component.literal("§aModule §f" + targetModule.name + " §abound to §f" + bindText), false
        );
    }
}
