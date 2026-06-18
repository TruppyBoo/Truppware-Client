package trupp.ware.event.events;

import trupp.ware.event.Event;

/**
 * Fired from {@code LocalPlayer.sendPosition()} — i.e. once per tick, right before the player's
 * movement/flying packet is sent (PRE) and right after (POST). This is the correct place to run
 * combat: rotations set here are carried by the very next flying packet, and attack/use packets sent
 * here land in the right order relative to it (what anticheats expect), instead of EventTick which
 * fires at the very start of the client tick, before input and movement.
 */
public class EventUpdate extends Event {
    public EventUpdate() {
        super("EventUpdate");
    }
}
