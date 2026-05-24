package trupp.ware.command;

import net.minecraft.network.protocol.game.ServerboundChatPacket;
import trupp.ware.command.commands.BindCommand;
import trupp.ware.command.commands.ConfigCommand;
import trupp.ware.command.commands.ToggleCommand;
import trupp.ware.event.Event;
import trupp.ware.event.events.EventPacket;


import java.util.ArrayList;
import java.util.List;

public class CommandManager {

    public static CommandManager getInstance = new CommandManager();
    public List<Command> commands = new ArrayList<>();

    // Register your commands here
    public void start() {
        commands.add(new ToggleCommand());
        commands.add(new ConfigCommand());
        commands.add(new BindCommand());
    }

    public void onEvent(Event e) {
        if (!(e instanceof EventPacket eventPacket)) return;


        if (eventPacket.getPacket() instanceof ServerboundChatPacket chatPacket) {
            String message = chatPacket.message();


            if (!message.startsWith(".")) return;


            String[] split = message.substring(1).split(" ");
            if (split.length == 0) return;

            String commandName = split[0];
            String[] args = new String[split.length - 1];

            if (split.length > 1) {
                System.arraycopy(split, 1, args, 0, split.length - 1);
            }


            for (Command command : commands) {
                if (command.getName().equalsIgnoreCase(commandName)) {
                    command.execute(args);
                    break;
                }
            }
            e.canceled = true;
        }
    }
}
