package dev.visualutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Gracze lecacy na elytrze w poblizu: dystans, kierunek i predkosc (alert z boku ekranu). */
public final class ElytraAlert {

    public record Entry(String name, double dist, String dir, int speed) {}

    private static final double RANGE = 160.0;

    private static List<Entry> entries = List.of();
    private static final Map<UUID, Vec3> LAST = new HashMap<>();
    private static Set<UUID> announced = new HashSet<>();

    private ElytraAlert() {}

    public static List<Entry> entries() {
        return entries;
    }

    public static void tick(Minecraft mc) {
        LocalPlayer me = mc.player;
        if (me == null || mc.level == null || !Config.isOn(Module.ELYTRAALERT)) {
            entries = List.of();
            LAST.clear();
            announced = new HashSet<>();
            return;
        }

        List<Entry> list = new ArrayList<>();
        Set<UUID> now = new HashSet<>();
        String alert = null;
        for (AbstractClientPlayer other : mc.level.players()) {
            if (other == me || !other.isAlive() || !other.isFallFlying()) continue;
            if (Friends.isFriend(other)) continue;
            double dist = other.distanceTo(me);
            if (dist > RANGE) continue;

            Vec3 pos = other.position();
            Vec3 prev = LAST.put(other.getUUID(), pos);
            int speed = prev == null ? 0 : (int) Math.round(pos.distanceTo(prev) * 20.0);

            double yawTo = Math.toDegrees(Math.atan2(-(pos.x - me.getX()), pos.z - me.getZ()));
            double rel = wrap(yawTo - me.getYRot());
            String dir;
            if (Math.abs(rel) < 45) dir = "przed tobą";
            else if (rel >= 45 && rel <= 135) dir = "z prawej";
            else if (rel <= -45 && rel >= -135) dir = "z lewej";
            else dir = "za tobą";

            list.add(new Entry(other.getName().getString(), dist, dir, speed));
            now.add(other.getUUID());
            if (!announced.contains(other.getUUID())) {
                alert = "⚠ " + other.getName().getString() + " leci na elytrze (" + (int) dist + "m, " + dir + ")";
            }
        }
        LAST.keySet().retainAll(now);
        announced = now;
        list.sort(Comparator.comparingDouble(Entry::dist));
        entries = list;

        if (alert != null) {
            me.displayClientMessage(Component.literal(alert), true);
        }
    }

    private static double wrap(double a) {
        a %= 360.0;
        if (a > 180.0) a -= 360.0;
        if (a < -180.0) a += 360.0;
        return a;
    }
}
