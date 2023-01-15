package com.minelittlepony.unicopia.client.sound;

import java.util.function.Predicate;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

public class LoopingSoundInstance<T extends Entity> extends FadeOutSoundInstance {

    private final T source;

    private final Predicate<T> shouldPlay;

    public LoopingSoundInstance(T source, Predicate<T> shouldPlay, SoundEvent soundEvent, float volume, float pitch, Random random) {
        super(soundEvent, source.getSoundCategory(), volume, random);
        this.source = source;
        this.shouldPlay = shouldPlay;
        this.pitch = pitch;
        this.attenuationType = SoundInstance.AttenuationType.NONE;
    }

    public LoopingSoundInstance<T> setFadeIn() {
        float volume = this.volume;
        this.volume = 0.001F;
        setTargetVolume(volume);
        return this;
    }

    @Override
    protected boolean shouldKeepPlaying() {
        if (source.isRemoved() || !shouldPlay.test(source)) {
            return false;
        }

        x = source.getX();
        y = source.getY();
        z = source.getZ();
        return true;
    }
}
