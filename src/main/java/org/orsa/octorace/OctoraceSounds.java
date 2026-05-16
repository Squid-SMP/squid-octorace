package org.orsa.octorace;

import eu.pb4.polymer.core.api.other.PolymerSoundEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import static org.orsa.octorace.Octorace.id;

public class OctoraceSounds {
    public static final SoundEvent BOOST_PAD = register("boost_pad");
    public static final SoundEvent JUMP_PAD = register("jump_pad");
    public static final SoundEvent SPEED_PAD = register("speed_pad");

    private static SoundEvent register(String name) {
        Identifier identifier = id(name);
        SoundEvent event = SoundEvent.createVariableRangeEvent(identifier);
        Registry.register(BuiltInRegistries.SOUND_EVENT, identifier, event);
        PolymerSoundEvent.registerOverlay(event, (SoundEvent) null);
        return event;
    }

    public static void initialize() {}
}