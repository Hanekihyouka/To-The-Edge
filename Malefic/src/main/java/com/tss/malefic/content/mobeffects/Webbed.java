package com.tss.malefic.content.mobeffects;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;


public class Webbed extends MobEffect {
    public Webbed() {
        super(MobEffectCategory.BENEFICIAL, 	14075068);
    }

    @Override
    public void applyEffectTick(LivingEntity m, int level) {

        Level serverLevel = m.getCommandSenderWorld();
        if(!(m instanceof Mob)){return;}
        Mob s = (Mob)m;

        if(!serverLevel.isClientSide()&&s.isAggressive()){
            //LogUtils.getLogger().info("Web!");
            Player p = serverLevel.getNearestPlayer(m,7.5+5*(level-1));
            if(p==null){return;}
            //LogUtils.getLogger().info("Web!"+p.getName());
            BlockPos bp = p.blockPosition();
            double randomWeb = 0.3;
            if(serverLevel.isEmptyBlock(bp)){
                serverLevel.setBlockAndUpdate(bp, Blocks.COBWEB.defaultBlockState());
                return;
            }
            bp= p.blockPosition().above();
            if(Math.random()<randomWeb&&serverLevel.isEmptyBlock(bp)){
                serverLevel.setBlockAndUpdate(bp, Blocks.COBWEB.defaultBlockState());
                return;
            }
            bp= p.blockPosition().west();
            if(Math.random()<randomWeb&&serverLevel.isEmptyBlock(bp)){
                serverLevel.setBlockAndUpdate(bp, Blocks.COBWEB.defaultBlockState());
                return;
            }
            bp= p.blockPosition().east();
            if(Math.random()<randomWeb&&serverLevel.isEmptyBlock(bp)){
                serverLevel.setBlockAndUpdate(bp, Blocks.COBWEB.defaultBlockState());
                return;
            }
            bp= p.blockPosition().north();
            if(Math.random()<randomWeb&&serverLevel.isEmptyBlock(bp)){
                serverLevel.setBlockAndUpdate(bp, Blocks.COBWEB.defaultBlockState());
                return;
            }
            bp= p.blockPosition().south();
            if(Math.random()<randomWeb&&serverLevel.isEmptyBlock(bp)){
                serverLevel.setBlockAndUpdate(bp, Blocks.COBWEB.defaultBlockState());
                return;
            }
            bp= p.blockPosition().below();
            if(Math.random()<randomWeb&&serverLevel.isEmptyBlock(bp)){
                serverLevel.setBlockAndUpdate(bp, Blocks.COBWEB.defaultBlockState());
                return;
            }

        }

    }

    @Override
    public boolean isDurationEffectTick(int duration, int level) {
        if(duration%40==0){
            return true;
        }
        return false;
    }


}
