package com.minelittlepony.unicopia.entity.player;

import com.minelittlepony.unicopia.ability.magic.Levelled;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.sound.*;
import net.minecraft.util.math.MathHelper;

class PlayerLevelStore implements Levelled.LevelStore {

    private final Pony pony;

    private final TrackedData<Integer> dataEntry;

    private final boolean upgradeMana;

    private final SoundEvent levelUpSound;

    PlayerLevelStore(Pony pony, TrackedData<Integer> dataEntry, boolean upgradeMana, SoundEvent levelUpSound) {
        this.pony = pony;
        this.dataEntry = dataEntry;
        this.upgradeMana = upgradeMana;
        this.levelUpSound = levelUpSound;
        pony.getEntity().getDataTracker().startTracking(dataEntry, 0);
    }

    @Override
    public void add(int levels) {
        if (levels > 0) {
            if (upgradeMana) {
                pony.getMagicalReserves().getMana().add(pony.getMagicalReserves().getMana().getMax() / 2);
            }
//            pony.getReferenceWorld().playSound(null, pony.getOrigin(), levelUpSound, SoundCategory.PLAYERS, 1, 2);
        }
        Levelled.LevelStore.super.add(levels);
    }

    @Override
    public int getMax() {
        return 900;
    }

    @Override
    public int get() {
        return pony.getEntity().getDataTracker().get(dataEntry);
    }

    @Override
    public void set(int level) {
        pony.getEntity().getDataTracker().set(dataEntry, MathHelper.clamp(level, 0, getMax()));
    }
}
