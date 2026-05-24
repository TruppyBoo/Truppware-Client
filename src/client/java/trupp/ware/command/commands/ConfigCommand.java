package trupp.ware.command.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import trupp.ware.command.Command;
import trupp.ware.config.ConfigManager;

public class ConfigCommand extends Command {
    public ConfigCommand() {
        super("Config");
    }


    @Override
    public void execute(String args[]){

        if (args.length < 2) return;

        String type = args[0];
        String name = args[1];

        if(type.equalsIgnoreCase("save")){
            ConfigManager.save(name);
            Minecraft mc = Minecraft.getInstance();
            mc.gui.getChat().addMessage(
                    Component.literal("saved " + name)
            );
        }

        if(type.equalsIgnoreCase("load")){
            ConfigManager.load(name);
            Minecraft mc = Minecraft.getInstance();
            mc.gui.getChat().addMessage(
                    Component.literal("loaded " + name)
            );
        }

    }


}
