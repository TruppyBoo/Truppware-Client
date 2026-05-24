package trupp.ware.command.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import trupp.ware.command.Command;
import trupp.ware.truppware.module.Manager;
import trupp.ware.truppware.module.Module;

public class ToggleCommand extends Command {


    public ToggleCommand() {
        super("Toggle");
    }


    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a module name!");
            return;
        }

        String targetName = args[0];

        for (Module m : Manager.trupp.modules) {
            if (m.name.equalsIgnoreCase(targetName)) {
                m.toggle();
                Minecraft mc = Minecraft.getInstance();
                mc.gui.getChat().addMessage(
                        Component.literal("toggled " + name)
                );
                return;
            }
        }
        Minecraft mc = Minecraft.getInstance();
        mc.gui.getChat().addMessage(
                Component.literal("could not find module"));
    }

}
