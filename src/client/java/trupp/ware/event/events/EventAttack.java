package trupp.ware.event.events;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import trupp.ware.event.Event;


public class EventAttack extends Event {

    public Player player;
    public Entity entity;

    public EventAttack(Player player, Entity entity) {
        super("EventAttack");
        this.player = player;
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public Player getPlayer() {
        return player;
    }
}
