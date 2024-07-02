package com.tss.malefic.content.mobeffects;

import com.mojang.logging.LogUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;


public class Sticky extends MobEffect {
    public Sticky() {
        super(MobEffectCategory.BENEFICIAL, 11468664);
    }

    @Override
    public void applyEffectTick(LivingEntity m, int level) {

    }

    @Override
    public boolean isDurationEffectTick(int duration, int level) {
        return false;
    }


}
