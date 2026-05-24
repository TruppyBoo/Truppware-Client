/*
 * Copyright (c) 2014-2026 TruppWare and contributors.
 *
 * This source code is subject to the terms of the GNU General
 * Public License, version 3. If a copy of the GPL was not distributed with this
 * file, you can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package trupp.ware.event.events;

import trupp.ware.event.Event;
import net.minecraft.world.entity.LivingEntity;

public class EventKnockback extends Event {

    private final LivingEntity entity; // The entity receiving knockback
    private double x;
    private double y;
    private double z;

    private final double defaultX;
    private final double defaultY;
    private final double defaultZ;

    public EventKnockback(LivingEntity entity, double x, double y, double z) {
        super("EventKnockback");
        this.entity = entity;
        this.x = x;
        this.y = y;
        this.z = z;
        this.defaultX = x;
        this.defaultY = y;
        this.defaultZ = z;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    // Current knockback values (modifiable)
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }

    // Default knockback values (read-only)
    public double getDefaultX() { return defaultX; }
    public double getDefaultY() { return defaultY; }
    public double getDefaultZ() { return defaultZ; }

}
