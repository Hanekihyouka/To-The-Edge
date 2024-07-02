package com.tss.malefic.content.mobeffects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;


public class SwiftFuse extends MobEffect {
    //Unused
    public SwiftFuse() {
        super(MobEffectCategory.BENEFICIAL, 11468664);
    }

    @Override
    public void applyEffectTick(LivingEntity m, int level) {
        if(m instanceof Creeper){
            Creeper c = (Creeper)m;
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int level) {
        return true;
    }


}
