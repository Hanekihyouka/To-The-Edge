package com.tss.malefic.handler;

import com.mojang.logging.LogUtils;
import com.tss.malefic.Config;
import com.tss.malefic.Malefic;
import com.tss.malefic.content.mobeffects.Sticky;
import com.tss.malefic.content.mobeffects.Webbed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jline.utils.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

public class EventHandler {
    /*@SubscribeEvent
    public void pickupItem(EntityItemPickupEvent e){
        System.out.println("Item picked up!");
        e.getItem().kill();
        e.setCanceled(true);
    }*/
    private float timeBuffer=0;
    private float customTick=0;

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e){
        if(e.phase.equals(TickEvent.Phase.START)){
            customTick++;
            if(customTick>20){
                customTick=0;
            }

            ServerLevel serverLevel = e.getServer().getLevel(Level.OVERWORLD);
            float dayTime = Math.floorMod(serverLevel.getDayTime(),24000);
            float normalizedTime = 0;
            float timeMultiplier = 1;
            if(dayTime>=6000&&dayTime<18000){
                normalizedTime = (18000f-dayTime)/12000f;
            } else if(dayTime<6000){
                normalizedTime = (dayTime+6000f)/12000f;
            } else {
                normalizedTime = (dayTime-18000f)/12000f;
            }
            timeMultiplier = (float) ((1-2.7*Math.pow(normalizedTime,2)+1.8*Math.pow(normalizedTime,3)));
            timeMultiplier= Math.max((float) Math.pow(timeMultiplier,4),0.025f)/0.292696f;
            timeBuffer+=1/(timeMultiplier*20);
            while (timeBuffer>=1){
                timeBuffer--;
                serverLevel.setDayTime(serverLevel.getDayTime()+1);
            }
        }
    }

    @SubscribeEvent
    public void entityTick(LivingEvent.LivingTickEvent e){
        if(customTick==0 && !e.getEntity().getCommandSenderWorld().isClientSide()) {
            LivingEntity m = e.getEntity();
            if(m.hasEffect(MobEffects.NIGHT_VISION)){
                m.removeEffect(MobEffects.DARKNESS);
                m.removeEffect(MobEffects.CONFUSION);
            }
            if(m.hasEffect(MobEffects.BLINDNESS)){
                m.removeEffect(MobEffects.NIGHT_VISION);
            }
            if(m.getType().equals(EntityType.PLAYER)&&m.isAlive()){
                if(m.getCommandSenderWorld().equals(Objects.requireNonNull(m.getCommandSenderWorld().getServer()).getLevel(Level.NETHER))){
                    if(m.hasEffect(MobEffects.FIRE_RESISTANCE)){
                        if(m.isOnFire()){
                            m.getCombatTracker().recordDamage(m.damageSources().onFire(), 1);
                            m.setHealth(m.getHealth() - 1);
                            m.gameEvent(GameEvent.ENTITY_DAMAGE);
                            m.animateHurt(0);
                            m.getCommandSenderWorld().playSound(null,m.blockPosition(),SoundEvents.PLAYER_HURT_ON_FIRE, SoundSource.PLAYERS );
                            if(m.getHealth()<=0){m.die(m.damageSources().onFire());}

                        }
                    } else {
                        if(!m.isOnFire()){
                            m.setSecondsOnFire(3);
                        }
                        m.getCombatTracker().recordDamage(m.damageSources().onFire(), 5);
                        m.setHealth(m.getHealth() - 5);
                        m.gameEvent(GameEvent.ENTITY_DAMAGE);
                        m.animateHurt(0);
                        m.getCommandSenderWorld().playSound(null,m.blockPosition(),SoundEvents.PLAYER_HURT_ON_FIRE, SoundSource.PLAYERS );
                        if(m.getHealth()<=0){m.die(m.damageSources().onFire());}
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void entityDamageEvent(LivingDamageEvent e){
        if(!e.getEntity().getCommandSenderWorld().isClientSide()) {
            LivingEntity m = e.getEntity();
            DamageSource ds = e.getSource();
            float damage = e.getAmount();

            //挖掘疲劳加伤
            if (damage >= 2 && m.hasEffect(MobEffects.DIG_SLOWDOWN)) {
                damage += Objects.requireNonNull(m.getEffect(MobEffects.DIG_SLOWDOWN)).getAmplifier() * 8;
                m.removeEffect(MobEffects.DIG_SLOWDOWN);
                //LogUtils.getLogger().info("postfatigue " + damage);
            }

            //凋零百分比血量伤害
            if (damage >= 1 && ds.is(DamageTypes.WITHER)) {
                damage = m.getHealth() * 0.05f;
                //LogUtils.getLogger().info("postwither " + damage);
            }

            //虚弱伤害最终乘算
            if (m.hasEffect(MobEffects.WEAKNESS)) {
                damage *= 1 + Objects.requireNonNull(m.getEffect(MobEffects.WEAKNESS)).getAmplifier() * 0.5f;
                //LogUtils.getLogger().info("postweakness " + damage);
            }

            //凋零伤害下限
            if (damage > 0 && ds.is(DamageTypes.WITHER)) {
                damage = Math.max(damage, 1);
            }

            e.setAmount(damage);
        }

    }


    @SubscribeEvent
    public void entityAttackEvent(LivingAttackEvent e){
        if(!e.getEntity().getCommandSenderWorld().isClientSide()) {
            LivingEntity m = e.getEntity();
            DamageSource ds = e.getSource();
            float damage = e.getAmount();


            if(m.invulnerableTime > 0 && !isFrameDamage(ds)) {
                if (m.getType().is(Tags.EntityTypes.BOSSES)||m.isOnFire()) {

                    damage = getDamageAfterArmorAbsorb(m, ds, damage);
                    damage = getDamageAfterMagicAbsorb(m, ds, damage);

                    //挖掘疲劳加伤
                    if (damage>=2&&m.hasEffect(MobEffects.DIG_SLOWDOWN)){
                        damage += Objects.requireNonNull(m.getEffect(MobEffects.DIG_SLOWDOWN)).getAmplifier()*8;
                        m.removeEffect(MobEffects.DIG_SLOWDOWN);
                        LogUtils.getLogger().info("postfatigue "+damage);
                    }

                    //凋零百分比血量伤害
                    if (damage>=1&&ds.is(DamageTypes.WITHER)){
                        damage = m.getHealth()*0.05f;
                        LogUtils.getLogger().info("postwither "+damage);
                    }

                    //虚弱伤害最终乘算
                    if (m.hasEffect(MobEffects.WEAKNESS)){
                        damage *= 1+ Objects.requireNonNull(m.getEffect(MobEffects.WEAKNESS)).getAmplifier()*0.5f;
                        LogUtils.getLogger().info("postweakness "+damage);
                    }

                    //凋零伤害下限
                    if (damage>0&&ds.is(DamageTypes.WITHER)){
                        damage = Math.max(damage,1);
                    }

                    float f1 = Math.max(damage - m.getAbsorptionAmount(), 0.0F);
                    m.setAbsorptionAmount(m.getAbsorptionAmount() - (damage - f1));
                    float f = damage - f1;
                    if (f > 0.0F && f < 3.4028235E37F) {
                        Entity entity = ds.getEntity();
                        if (entity instanceof ServerPlayer serverplayer) {
                            serverplayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(f * 10.0F));
                        }
                    }

                    f1 = ForgeHooks.onLivingDamage(m, ds, f1);
                    if (f1 != 0.0F) {
                        m.getCombatTracker().recordDamage(ds, f1);
                        m.setHealth(m.getHealth() - f1);
                        m.setAbsorptionAmount(m.getAbsorptionAmount() - f1);
                        m.gameEvent(GameEvent.ENTITY_DAMAGE);
                    }
                    m.animateHurt(0);
                    m.playSound(SoundEvents.FIREWORK_ROCKET_BLAST_FAR, 1.0F, m.isBaby() ? (m.getCommandSenderWorld().random.nextFloat() - m.getCommandSenderWorld().random.nextFloat()) * 0.2F + 1.5F : (m.getCommandSenderWorld().random.nextFloat() - m.getCommandSenderWorld().random.nextFloat()) * 0.2F + 1.0F);

                    //LogUtils.getLogger().info("attackInvul" + e.getAmount() + " " + m.getHealth());

                    //m.handleDamageEvent(e.getSource());
                }
            }

        }
    }

    protected boolean isFrameDamage(DamageSource ds){
        return ds.is(DamageTypes.LAVA)||ds.is(DamageTypes.IN_WALL)||ds.is(DamageTypes.IN_FIRE)||ds.is(DamageTypes.HOT_FLOOR)||ds.is(DamageTypes.IN_WALL)||ds.is(DamageTypes.DRAGON_BREATH)||ds.is(DamageTypes.CRAMMING)||ds.is(DamageTypes.IN_WALL)||ds.is(DamageTypes.BAD_RESPAWN_POINT);
    }


    protected float getDamageAfterArmorAbsorb(LivingEntity m, DamageSource p_21162_, float p_21163_) {
        if (!p_21162_.is(DamageTypeTags.BYPASSES_ARMOR)) {
            p_21163_ = CombatRules.getDamageAfterAbsorb(p_21163_, (float)m.getArmorValue(), (float)m.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        }
        return p_21163_;
    }

    protected float getDamageAfterMagicAbsorb(LivingEntity m, DamageSource p_21193_, float p_21194_) {
        if (p_21193_.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            return p_21194_;
        } else {
            int k;
            if (m.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !p_21193_.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                k = (m.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
                int j = 25 - k;
                float f = p_21194_ * (float)j;
                float f1 = p_21194_;
                p_21194_ = Math.max(f / 25.0F, 0.0F);
                float f2 = f1 - p_21194_;
                if (f2 > 0.0F && f2 < 3.4028235E37F) {
                    if (m instanceof ServerPlayer) {
                        ((ServerPlayer)m).awardStat(Stats.CUSTOM.get(Stats.DAMAGE_RESISTED), Math.round(f2 * 10.0F));
                    } else if (p_21193_.getEntity() instanceof ServerPlayer) {
                        ((ServerPlayer)p_21193_.getEntity()).awardStat(Stats.CUSTOM.get(Stats.DAMAGE_DEALT_RESISTED), Math.round(f2 * 10.0F));
                    }
                }
            }

            if (p_21194_ <= 0.0F) {
                return 0.0F;
            } else if (p_21193_.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
                return p_21194_;
            } else {
                k = EnchantmentHelper.getDamageProtection(m.getArmorSlots(), p_21193_);
                if (k > 0) {
                    p_21194_ = CombatRules.getDamageAfterMagicAbsorb(p_21194_, (float)k);
                }

                return p_21194_;
            }
        }
    }
   /* @SubscribeEvent
    public void onSmithingTableCraft(Smith e){
        if(e.getInventory() instanceof SmithingMenu){
            LogUtils.getLogger().info("SMITHING!");
        }
        LogUtils.getLogger().info("SMITHING?");


    }*/







    @SubscribeEvent
    public void onMobSpawn(MobSpawnEvent.FinalizeSpawn e){
        if(!e.getLevel().isClientSide()){
            float random = e.getLevel().getRandom().nextFloat();
            float compositeRandom = e.getLevel().getRandom().nextFloat();
            float legendaryCriteria = Config.legendaryChance;
            float eliteCriteria = legendaryCriteria+Config.eliteChance;
            float veteranCriteria = legendaryCriteria+eliteCriteria+Config.veteranChance;
            Holder<Biome> biome = e.getLevel().getBiome(e.getEntity().blockPosition());


            if(e.getEntity().getType().equals(EntityType.ZOMBIE)){
                int mobLevel = 0;
                if(random<veteranCriteria){
                    mobLevel++;
                }
                if(random<eliteCriteria){
                    mobLevel++;
                }
                if(random<legendaryCriteria){
                    mobLevel++;
                }
                if((biome.is(Biomes.SWAMP)|| biome.is(Biomes.MANGROVE_SWAMP)) && compositeRandom<Config.compositeMobChance){
                    spawnMob("zombieGuardian",mobLevel,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                } else if((biome.is(Biomes.PLAINS)|| biome.is(Biomes.SUNFLOWER_PLAINS)
                        ||(biome.is(Biomes.BIRCH_FOREST))||(biome.is(Biomes.TAIGA))||(biome.is(Biomes.SNOWY_TAIGA))||(biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST))||(biome.is(Biomes.FOREST))||(biome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA))||(biome.is(Biomes.OLD_GROWTH_PINE_TAIGA))||(biome.is(Biomes.FLOWER_FOREST))||(biome.is(Biomes.JUNGLE))||(biome.is(Biomes.SPARSE_JUNGLE))||(biome.is(Biomes.BAMBOO_JUNGLE))
                        ||(biome.is(Biomes.LUSH_CAVES))||(biome.is(Biomes.DRIPSTONE_CAVES))
                ) && compositeRandom<Config.compositeMobChance){
                    spawnMob("zombieTower",mobLevel,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                } else {
                    spawnMob("zombie",mobLevel,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                }
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.HUSK)){
                int mobLevel = 0;
                if(random<veteranCriteria){
                    mobLevel++;
                }
                if(random<eliteCriteria){
                    mobLevel++;
                }
                if(random<legendaryCriteria){
                    mobLevel++;
                }
                if(compositeRandom<Config.compositeMobChance){
                    spawnMob("huskBlaze",mobLevel,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                } else {
                    spawnMob("husk",mobLevel,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                }
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.SKELETON)){
                int mobLevel = 0;
                if(random<veteranCriteria){
                    mobLevel++;
                }
                if(random<eliteCriteria){
                    mobLevel++;
                }
                if(random<legendaryCriteria){
                    mobLevel++;
                }
                spawnMob("skeleton",mobLevel,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.WITHER_SKELETON)){
                int mobLevel = 0;
                if(random<veteranCriteria){
                    mobLevel++;
                }
                if(random<eliteCriteria){
                    mobLevel++;
                }
                if(random<legendaryCriteria){
                    mobLevel++;
                }
                if(biome.is(Biomes.END_HIGHLANDS)){
                    spawnMob("phantomSkeleton",3,0,e.getLevel().getLevel(),e.getEntity().blockPosition());

                } else{
                    spawnMob("wither_skeleton",mobLevel,0,e.getLevel().getLevel(),e.getEntity().blockPosition());

                }
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.DROWNED)){
                int mobLevel = 0;
                if(random<veteranCriteria){
                    mobLevel++;
                }
                if(random<eliteCriteria){
                    mobLevel++;
                }
                if(random<legendaryCriteria){
                    mobLevel++;
                }
                spawnMob("drowned",mobLevel,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.ZOMBIFIED_PIGLIN)){
                int mobLevel = 0;
                if(random<veteranCriteria){
                    mobLevel++;
                }
                if(random<eliteCriteria){
                    mobLevel++;
                }
                if(random<legendaryCriteria){
                    mobLevel++;
                }
                spawnMob("zombified_piglin",mobLevel,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.STRAY)){
                int mobLevel = 0;
                if(random<veteranCriteria){
                    mobLevel++;
                }
                if(random<eliteCriteria){
                    mobLevel++;
                }
                if(random<legendaryCriteria){
                    mobLevel++;
                }
                spawnMob("stray",mobLevel,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.WITCH)){
                if(compositeRandom<Config.compositeMobChance){
                    spawnMob("witchBat",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                } else {
                    spawnMob("witch",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                }
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.RAVAGER)){
                if(compositeRandom<Config.compositeMobChance){
                    spawnMob("ravagerStray",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                } else {
                    spawnMob("ravager",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                }
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.PHANTOM)){
                {
                    spawnMob("phantom",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                }
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.SPIDER)){
                if(compositeRandom<Config.compositeMobChance){
                    spawnMob("spiderSkeleton",(int)(random*3)+1,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                } else {
                    spawnMob("spider",(int)(random*3)+1,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                }
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.CAVE_SPIDER)){
                spawnMob("cavespider",(int)(random*3)+1,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.CREEPER)){
                spawnMob("creeper",(int)(random*3)+1,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.ENDERMAN)){
                spawnMob("enderman",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.GUARDIAN)){
                spawnMob("guardian",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.VINDICATOR)){
                spawnMob("vindicator",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.PILLAGER)){
                spawnMob("pillager",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.ILLUSIONER)){
                spawnMob("illusioner",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.BLAZE)){
                spawnMob("blaze",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.VEX)){
                if(biome.is(Biomes.BASALT_DELTAS)||biome.is(Biomes.SOUL_SAND_VALLEY)||biome.is(Biomes.NETHER_WASTES)||biome.is(Biomes.CRIMSON_FOREST)||biome.is(Biomes.WARPED_FOREST)){
                    spawnMob("firevex",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                    e.setSpawnCancelled(true);
                }else{
                    //spawnMob("vex",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                }
            }
            else if(e.getEntity().getType().equals(EntityType.PIGLIN)){
                spawnMob("piglin",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.GHAST)){
                spawnMob("ghast",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.HOGLIN)){
                spawnMob("hoglin",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.PIGLIN_BRUTE)){
                spawnMob("piglin_brute",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.ELDER_GUARDIAN)){
                spawnMob("elder_guardian",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.EVOKER)){
                if(biome.is(Biomes.END_HIGHLANDS)){
                    if(random<0.5){
                        spawnMob("elder_guardian",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                    }else{
                        spawnMob("evokerRavager",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                    }
                }else{
                    spawnMob("evoker",0,0,e.getLevel().getLevel(),e.getEntity().blockPosition());
                }
                e.setSpawnCancelled(true);
            }
            else if(e.getEntity().getType().equals(EntityType.WITHER)){
                WitherBoss m = (WitherBoss) e.getEntity();
                Objects.requireNonNull(e.getEntity().getAttribute(Attributes.MAX_HEALTH)).setBaseValue(3000);
                m.setHealth(3000);
                LogUtils.getLogger().info("WITHERSPAWN!!!!");
                // Mark player
                Player p = m.getCommandSenderWorld().getNearestPlayer(m,64);
                if(p!=null){
                    p.addTag(p.getStringUUID());
                    p.getCombatTracker().recordDamage(m.damageSources().wither(), m.getHealth()-2);
                    p.setHealth(2);
                    p.gameEvent(GameEvent.ENTITY_DAMAGE);
                    p.animateHurt(0);
                    p.getCommandSenderWorld().playSound(null,m.blockPosition(),SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS );
                    p.addEffect(new MobEffectInstance(MobEffects.WITHER,20, 0));

                    m.getCommandSenderWorld().getPlayerByUUID(UUID.fromString(m.getTags().stream().toList().get(0)));

                } else{
                    LogUtils.getLogger().info(e.getX()+":"+e.getZ());

                }






            }
            else if(e.getEntity().getType().equals(EntityType.ENDER_DRAGON)){
                Objects.requireNonNull(e.getEntity().getAttribute(Attributes.MAX_HEALTH)).setBaseValue(15000);
                e.getEntity().setHealth(15000);
            }
        }


    }


    private float getHealthMultiplier(int mobLevel){
        return switch (mobLevel) {
            case 1 -> Config.veteranHealthMultiplier;
            case 2 -> Config.eliteHealthMultiplier;
            case 3 -> Config.legendaryHealthMultiplier;
            default -> 1;
        };
    }
    private float getSpeedMultiplier(int mobLevel){
        return switch (mobLevel) {
            case 1 -> Config.veteranSpeedMultiplier;
            case 2 -> Config.eliteSpeedMultiplier;
            case 3 -> Config.legendarySpeedMultiplier;
            default -> 1;
        };
    }
    private float getAttackAddition(int mobLevel){
        return switch (mobLevel) {
            case 1 -> Config.veteranAttackAddition;
            case 2 -> Config.eliteAttackAddition;
            case 3 -> Config.legendaryAttackAddition;
            default -> 0;
        };
    }
    private float getEnchantLevel(int mobLevel){
        return switch (mobLevel) {
            case 1 -> Config.veteranEnchantMultiplier;
            case 2 -> Config.eliteEnchantMultiplier;
            case 3 -> Config.legendaryEnchantMultiplier;
            default -> 0;
        };
    }
    private ItemStack getItemWithEnchant(Item item, int randomEnchantLevel, RandomEnchantType type){
        ItemStack is = new ItemStack(item);
        int enchantPower = randomEnchantLevel*3;
        int primitiveEnchantPower = enchantPower;
        ArrayList<Enchantment> possibleEnchantments= new ArrayList<>();
        Enchantment selectedEnchantment;
        switch (type){
            case ARMOR:
                possibleEnchantments.add(Enchantments.PROJECTILE_PROTECTION);
                possibleEnchantments.add(Enchantments.ALL_DAMAGE_PROTECTION);
                possibleEnchantments.add(Enchantments.BLAST_PROTECTION);
                possibleEnchantments.add(Enchantments.FIRE_PROTECTION);
                possibleEnchantments.add(Enchantments.UNBREAKING);
                break;
            case CHEST_ARMOR:
                possibleEnchantments.add(Enchantments.PROJECTILE_PROTECTION);
                possibleEnchantments.add(Enchantments.ALL_DAMAGE_PROTECTION);
                possibleEnchantments.add(Enchantments.BLAST_PROTECTION);
                possibleEnchantments.add(Enchantments.FIRE_PROTECTION);
                possibleEnchantments.add(Enchantments.THORNS);
                possibleEnchantments.add(Enchantments.UNBREAKING);

                break;
            case MELEE_WEAPON:
                possibleEnchantments.add(Enchantments.SHARPNESS);
                possibleEnchantments.add(Enchantments.FIRE_ASPECT);
                possibleEnchantments.add(Enchantments.KNOCKBACK);
                possibleEnchantments.add(Enchantments.UNBREAKING);

                break;
            case BOW:
                possibleEnchantments.add(Enchantments.POWER_ARROWS);
                possibleEnchantments.add(Enchantments.FLAMING_ARROWS);
                possibleEnchantments.add(Enchantments.PUNCH_ARROWS);
                possibleEnchantments.add(Enchantments.UNBREAKING);


                break;
        }
        while (primitiveEnchantPower==enchantPower||enchantPower>49*Math.random()){
            selectedEnchantment=null;
            int totalWeight = 0;
            for(Enchantment e : possibleEnchantments){
                totalWeight += e.getRarity().getWeight();
            }
            float randomEnchant = (float) (totalWeight*Math.random());
            for(Enchantment e : possibleEnchantments){
                randomEnchant-= e.getRarity().getWeight();
                if(randomEnchant<0){
                    selectedEnchantment = e;
                    break;
                }
            }
            if(selectedEnchantment!=null){
                is.enchant(selectedEnchantment,getEnchantmentLevel(selectedEnchantment,primitiveEnchantPower));
                ArrayList<Enchantment> removeEnchantmentsList= new ArrayList<>();
                for(Enchantment e : possibleEnchantments){
                    if (!selectedEnchantment.isCompatibleWith(e)){
                        removeEnchantmentsList.add(e);
                    }
                }
                possibleEnchantments.removeAll(removeEnchantmentsList);
            }
            enchantPower/=2;
        }
        is.setDamageValue((int)((float)Math.random()*0.9f*(float)is.getMaxDamage()));
        return is;
    }

    private int getEnchantmentLevel(Enchantment enchantment, int enchantLevel){
        if(enchantment.equals(Enchantments.ALL_DAMAGE_PROTECTION)){
            if (enchantLevel<=12) return 1;
            else if (enchantLevel<=23) return 2;
            else if (enchantLevel<=34) return 3;
            else if (enchantLevel<=45) return 4;
            else return 5;
        }
        if(enchantment.equals(Enchantments.PROJECTILE_PROTECTION)){
            if (enchantLevel<=9) return 1;
            else if (enchantLevel<=15) return 2;
            else if (enchantLevel<=21) return 3;
            else if (enchantLevel<=27) return 4;
            else return 5;
        }
        if(enchantment.equals(Enchantments.FIRE_PROTECTION)){
            if (enchantLevel<=18) return 1;
            else if (enchantLevel<=26) return 2;
            else if (enchantLevel<=34) return 3;
            else if (enchantLevel<=42) return 4;
            else return 5;
        }
        if(enchantment.equals(Enchantments.BLAST_PROTECTION)){
            if (enchantLevel<=13) return 1;
            else if (enchantLevel<=21) return 2;
            else if (enchantLevel<=29) return 3;
            else if (enchantLevel<=37) return 4;
            else return 5;
        }
        if(enchantment.equals(Enchantments.THORNS)){
            if (enchantLevel<=10) return 1;
            else if (enchantLevel<=25) return 2;
            else if (enchantLevel<=35) return 3;
            else if (enchantLevel<=45) return 4;
            else return 5;
        }
        if(enchantment.equals(Enchantments.UNBREAKING)){
            if (enchantLevel<=5) return 1;
            else if (enchantLevel<=15) return 2;
            else if (enchantLevel<=30) return 3;
            else if (enchantLevel<=45) return 4;
            else return 5;
        }
        if(enchantment.equals(Enchantments.SHARPNESS)){
            if (enchantLevel<=12) return 1;
            else if (enchantLevel<=23) return 2;
            else if (enchantLevel<=34) return 3;
            else if (enchantLevel<=45) return 4;
            else return 5;
        }
        if(enchantment.equals(Enchantments.KNOCKBACK)){
            if (enchantLevel<=10) return 1;
            else if (enchantLevel<=20) return 2;
            else if (enchantLevel<=30) return 3;
            else if (enchantLevel<=40) return 4;
            else return 5;
        }
        if(enchantment.equals(Enchantments.FIRE_ASPECT)){
            if (enchantLevel<=10) return 1;
            else if (enchantLevel<=30) return 2;
            else if (enchantLevel<=40) return 3;
            else if (enchantLevel<=50) return 4;
            else return 5;
        }
        if(enchantment.equals(Enchantments.FLAMING_ARROWS)){
            if (enchantLevel<=10) return 1;
            else if (enchantLevel<=30) return 2;
            else if (enchantLevel<=40) return 3;
            else if (enchantLevel<=50) return 4;
            else return 5;
        }
        if(enchantment.equals(Enchantments.POWER_ARROWS)){
            if (enchantLevel<=12) return 1;
            else if (enchantLevel<=23) return 2;
            else if (enchantLevel<=34) return 3;
            else if (enchantLevel<=45) return 4;
            else return 5;
        }
        if(enchantment.equals(Enchantments.PUNCH_ARROWS)){
            if (enchantLevel<=10) return 1;
            else if (enchantLevel<=20) return 2;
            else if (enchantLevel<=30) return 3;
            else if (enchantLevel<=40) return 4;
            else return 5;
        }

        return 1;
    }

    private ItemStack getSpecialDrop(int mobLevel){
        return switch (mobLevel) {
            case 1 -> new ItemStack(Items.EXPERIENCE_BOTTLE);
            case 2 -> new ItemStack(Items.EXPERIENCE_BOTTLE,3);
            case 3 -> new ItemStack(Items.DRAGON_BREATH);
            default -> new ItemStack(Items.AIR);
        };
    }

    private void gearEliteMob(Mob m, int mobLevel, boolean useBow){
        m.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.STONE_BUTTON));
        if(mobLevel==0){
            if(useBow){
                m.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
            }
            return; }
        int enchantLevel = (int) getEnchantLevel(mobLevel);
        Item head = Items.LEATHER_HELMET;
        Item chest = Items.LEATHER_CHESTPLATE;
        Item legs = Items.LEATHER_LEGGINGS;
        Item feet = Items.LEATHER_BOOTS;
        Item mainhand = Items.IRON_SHOVEL;
        switch (mobLevel){
            case 1:
                if(Math.random()>0.5){
                    head = Items.LEATHER_HELMET;
                } else{
                    head = Items.CHAINMAIL_HELMET;
                }
                if(Math.random()>0.5){
                    chest = Items.LEATHER_CHESTPLATE;
                } else{
                    chest = Items.CHAINMAIL_CHESTPLATE;
                }
                if(Math.random()>0.5){
                    legs = Items.LEATHER_LEGGINGS;
                } else{
                    legs = Items.CHAINMAIL_LEGGINGS;
                }
                if(Math.random()>0.5){
                    feet = Items.LEATHER_BOOTS;
                } else{
                    feet = Items.CHAINMAIL_BOOTS;
                }
                if(Math.random()>0.5){
                    mainhand = Items.IRON_SHOVEL;
                } else{
                    mainhand = Items.IRON_SWORD;
                }
                break;

            case 2:
                if(Math.random()>0.6){
                    head = Items.IRON_HELMET;
                } else{
                    head = Items.GOLDEN_HELMET;
                }
                if(Math.random()>0.6){
                    chest = Items.IRON_CHESTPLATE;
                } else{
                    chest = Items.GOLDEN_CHESTPLATE;
                }
                if(Math.random()>0.6){
                    legs = Items.IRON_LEGGINGS;
                } else{
                    legs = Items.GOLDEN_LEGGINGS;
                }
                if(Math.random()>0.6){
                    feet = Items.IRON_BOOTS;
                } else{
                    feet = Items.GOLDEN_BOOTS;
                }
                if(Math.random()>0.6){
                    mainhand = Items.DIAMOND_SHOVEL;
                } else{
                    mainhand = Items.DIAMOND_SWORD;
                }
                break;

            case 3:
                if(Math.random()>0.7){
                    head = Items.DIAMOND_HELMET;
                } else{
                    head = Items.NETHERITE_HELMET;
                }
                if(Math.random()>0.7){
                    chest = Items.DIAMOND_CHESTPLATE;
                } else{
                    chest = Items.NETHERITE_CHESTPLATE;
                }
                if(Math.random()>0.7){
                    legs = Items.DIAMOND_LEGGINGS;
                } else{
                    legs = Items.NETHERITE_LEGGINGS;
                }
                if(Math.random()>0.7){
                    feet = Items.DIAMOND_BOOTS;
                } else{
                    feet = Items.NETHERITE_BOOTS;
                }
                if(Math.random()>0.7){
                    mainhand = Items.NETHERITE_SHOVEL;
                } else{
                    mainhand = Items.NETHERITE_SWORD;
                }
                break;
        }
        if(useBow){
            mainhand = Items.BOW;
        }
        m.setItemSlot(EquipmentSlot.HEAD,getItemWithEnchant(head,enchantLevel, RandomEnchantType.ARMOR));
        m.setItemSlot(EquipmentSlot.CHEST,getItemWithEnchant(chest,enchantLevel, RandomEnchantType.CHEST_ARMOR));
        m.setItemSlot(EquipmentSlot.LEGS,getItemWithEnchant(legs,enchantLevel, RandomEnchantType.ARMOR));
        m.setItemSlot(EquipmentSlot.FEET,getItemWithEnchant(feet,enchantLevel, RandomEnchantType.ARMOR));
        m.setItemSlot(EquipmentSlot.MAINHAND,getItemWithEnchant(mainhand,enchantLevel, RandomEnchantType.MELEE_WEAPON));
        m.setItemSlot(EquipmentSlot.OFFHAND,getSpecialDrop(mobLevel));
        m.setDropChance(EquipmentSlot.HEAD,0.085f);
        m.setDropChance(EquipmentSlot.CHEST,0.085f);
        m.setDropChance(EquipmentSlot.LEGS,0.085f);
        m.setDropChance(EquipmentSlot.FEET,0.085f);
        m.setDropChance(EquipmentSlot.MAINHAND,0.085f);
        m.setGuaranteedDrop(EquipmentSlot.OFFHAND);
    }


    private void spawnMob(String mobType, int mobLevel, int difficulty, ServerLevel level, BlockPos blockPos){
        float healthMultiplier = getHealthMultiplier(mobLevel);
        float speedMultiplier = getSpeedMultiplier(mobLevel);
        float attackAddition = getAttackAddition(mobLevel);
        switch (mobType){
            case "zombie":
            {
                Zombie m = new Zombie(EntityType.ZOMBIE, level);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(2);
                Objects.requireNonNull(m.getAttribute(Attributes.KNOCKBACK_RESISTANCE)).setBaseValue(4);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier *20);
                Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue()*speedMultiplier);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue()+attackAddition);
                gearEliteMob(m,mobLevel,false);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "zombieTower":
            {
                Zombie m = new Zombie(EntityType.ZOMBIE, level);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(2);
                Objects.requireNonNull(m.getAttribute(Attributes.KNOCKBACK_RESISTANCE)).setBaseValue(4);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier *20);
                Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue()*speedMultiplier);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue()+attackAddition);
                m.setBaby(true);
                Zombie m1 = new Zombie(EntityType.ZOMBIE, level);
                Objects.requireNonNull(m1.getAttribute(Attributes.ARMOR)).setBaseValue(2);
                Objects.requireNonNull(m1.getAttribute(Attributes.KNOCKBACK_RESISTANCE)).setBaseValue(4);
                Objects.requireNonNull(m1.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier *20);
                Objects.requireNonNull(m1.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(Objects.requireNonNull(m1.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue()*speedMultiplier);
                Objects.requireNonNull(m1.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(Objects.requireNonNull(m1.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue()+attackAddition);
                m1.setBaby(true);
                Zombie m2 = new Zombie(EntityType.ZOMBIE, level);
                Objects.requireNonNull(m2.getAttribute(Attributes.ARMOR)).setBaseValue(2);
                Objects.requireNonNull(m2.getAttribute(Attributes.KNOCKBACK_RESISTANCE)).setBaseValue(4);
                Objects.requireNonNull(m2.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier *20);
                Objects.requireNonNull(m2.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(Objects.requireNonNull(m2.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue()*speedMultiplier);
                Objects.requireNonNull(m2.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(Objects.requireNonNull(m2.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue()+attackAddition);
                m2.setBaby(true);
                gearEliteMob(m,mobLevel,false);
                gearEliteMob(m1,mobLevel,false);
                gearEliteMob(m2,mobLevel,false);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                m1.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                m2.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
                level.addFreshEntity(m1);
                level.addFreshEntity(m2);
                m2.startRiding(m1);
                m1.startRiding(m);
            }
            break;
            case "zombieGuardian":
            {
                Zombie m = new Zombie(EntityType.ZOMBIE, level);
                m.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.STONE_BUTTON));
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(2);
                Objects.requireNonNull(m.getAttribute(Attributes.KNOCKBACK_RESISTANCE)).setBaseValue(4);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier *20);
                Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue()*speedMultiplier);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue()+attackAddition);
                Guardian m1 = new Guardian(EntityType.GUARDIAN, level);
                Objects.requireNonNull(m1.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(Objects.requireNonNull(m1.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue()*3);
                gearEliteMob(m,mobLevel,false);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                m1.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
                level.addFreshEntity(m1);
                m1.startRiding(m);
            }
            break;
            case "huskBlaze" :
            {
                Husk m = new Husk(EntityType.HUSK, level);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(40);
                Objects.requireNonNull(m.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE)).setBaseValue(0);
                Objects.requireNonNull(m.getAttribute(Attributes.KNOCKBACK_RESISTANCE)).setBaseValue(4);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier *20);
                Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue()*speedMultiplier);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue()+attackAddition);
                gearEliteMob(m,mobLevel,false);
                Blaze m1 = new Blaze(EntityType.BLAZE, level);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(12);
                Objects.requireNonNull(m1.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier * 50);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                m1.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
                level.addFreshEntity(m1);
                m1.startRiding(m);
            }
            break;
            case "husk" :
            {
                Husk m = new Husk(EntityType.HUSK, level);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(40);
                Objects.requireNonNull(m.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE)).setBaseValue(0);
                Objects.requireNonNull(m.getAttribute(Attributes.KNOCKBACK_RESISTANCE)).setBaseValue(4);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier *20);
                Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue()*speedMultiplier);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue()+attackAddition);
                gearEliteMob(m,mobLevel,false);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "skeleton" :
            {
                Skeleton m = new Skeleton(EntityType.SKELETON, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier *20);
                Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue()*speedMultiplier);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue()+attackAddition);
                gearEliteMob(m,mobLevel,true);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "wither_skeleton" :
            {
                WitherSkeleton m = new WitherSkeleton(EntityType.WITHER_SKELETON, level);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(16);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier *60);
                Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue()*speedMultiplier);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue()+attackAddition);
                gearEliteMob(m,mobLevel,true);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "drowned" :
            {
                Drowned m = new Drowned(EntityType.DROWNED, level);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(2);
                Objects.requireNonNull(m.getAttribute(Attributes.KNOCKBACK_RESISTANCE)).setBaseValue(4);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier *20);
                Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue()*speedMultiplier);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue()+attackAddition);
                if(Math.random()<0.03f){
                    m.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.NAUTILUS_SHELL));
                }
                gearEliteMob(m,mobLevel,false);
                if(Math.random()<0.0625f+(0.3125*mobLevel)){
                    m.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.TRIDENT));
                }
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "zombified_piglin" :
            {
                ZombifiedPiglin m = new ZombifiedPiglin(EntityType.ZOMBIFIED_PIGLIN, level);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(4);
                Objects.requireNonNull(m.getAttribute(Attributes.KNOCKBACK_RESISTANCE)).setBaseValue(4);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier *50);
                Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue()*speedMultiplier);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue()+attackAddition);
                gearEliteMob(m,mobLevel,false);
                if(mobLevel==0){
                    m.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.GOLDEN_SWORD));
                }
                m.setAggressive(true);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "stray" :
            {
                Stray m = new Stray(EntityType.STRAY, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier *20);
                Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue()*speedMultiplier);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).getBaseValue()+attackAddition);
                gearEliteMob(m,mobLevel,true);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "witchBat" :
            {
                Witch m = new Witch(EntityType.WITCH, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier *60);
                Bat m1 = new Bat(EntityType.BAT, level);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                m1.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
                level.addFreshEntity(m1);
                m.startRiding(m1);

            }
            break;
            case "witch" :
            {
                Witch m = new Witch(EntityType.WITCH, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(healthMultiplier *60);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "ravagerStray" :
            {
                Ravager m = new Ravager(EntityType.RAVAGER, level);
                Stray m1 = new Stray(EntityType.STRAY, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(450);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                m1.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
                level.addFreshEntity(m1);
                m1.startRiding(m);
            }
            break;
            case "ravager" :
            {
                Ravager m = new Ravager(EntityType.RAVAGER, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(450);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "phantomSkeleton" :
            {
                Phantom m = new Phantom(EntityType.PHANTOM, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(70);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
                WitherSkeleton m1 = new WitherSkeleton(EntityType.WITHER_SKELETON, level);
                Objects.requireNonNull(m1.getAttribute(Attributes.ARMOR)).setBaseValue(12);
                Objects.requireNonNull(m1.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(60);
                gearEliteMob(m1,mobLevel,true);
                m1.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m1);
                m1.startRiding(m);
            }
            break;
            case "phantom" :
            {
                Phantom m = new Phantom(EntityType.PHANTOM, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(70);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "spiderSkeleton" :
            {
                Spider m = new Spider(EntityType.SPIDER, level);
                Skeleton m1 = new Skeleton(EntityType.SKELETON,level);
                m1.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.STONE_BUTTON));
                m1.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
                m1.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);

                switch (mobLevel){
                    case 1:
                        Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(16);
                        m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                        m.addEffect(new MobEffectInstance(Malefic.MOBEFFECT_STICKY.get(),9999999,0));
                        level.addFreshEntity(m);
                        break;
                    case 2:
                        Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(16);
                        m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                        m.addEffect(new MobEffectInstance(Malefic.MOBEFFECT_WEBBED.get(),9999999,0));
                        level.addFreshEntity(m);
                        break;
                    case 3:
                        Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(16);
                        m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                        m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,9999999,3));
                        m.addEffect(new MobEffectInstance(MobEffects.REGENERATION,9999999,3));
                        m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,9999999,3));
                        m.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,9999999,0));
                        level.addFreshEntity(m);
                        break;
                }
                m1.startRiding(m);
            }
            break;
            case "spider" :
            {
                Spider m = new Spider(EntityType.SPIDER, level);
                switch (mobLevel){
                    case 1:
                        Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(16);
                        m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                        m.addEffect(new MobEffectInstance(Malefic.MOBEFFECT_STICKY.get(),9999999,0));
                        level.addFreshEntity(m);
                        break;
                    case 2:
                        Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(16);
                        m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                        m.addEffect(new MobEffectInstance(Malefic.MOBEFFECT_WEBBED.get(),9999999,0));
                        level.addFreshEntity(m);
                        break;
                    case 3:
                        Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(16);
                        m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                        m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,9999999,3));
                        m.addEffect(new MobEffectInstance(MobEffects.REGENERATION,9999999,3));
                        m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,9999999,3));
                        m.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,9999999,0));
                        level.addFreshEntity(m);
                        break;
                }
            }
            break;
            case "cavespider" :
            {
                CaveSpider m = new CaveSpider(EntityType.CAVE_SPIDER, level);
                switch (mobLevel){
                    case 1:
                        Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(16);
                        m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                        m.addEffect(new MobEffectInstance(Malefic.MOBEFFECT_STICKY.get(),9999999,0));
                        level.addFreshEntity(m);
                        break;
                    case 2:
                        Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(16);
                        m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                        m.addEffect(new MobEffectInstance(Malefic.MOBEFFECT_WEBBED.get(),9999999,0));
                        level.addFreshEntity(m);
                        break;
                    case 3:
                        Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(16);
                        m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                        m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,9999999,3));
                        m.addEffect(new MobEffectInstance(MobEffects.REGENERATION,9999999,3));
                        m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,9999999,3));
                        m.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,9999999,0));
                        level.addFreshEntity(m);
                        break;
                }
            }
            break;
            case "creeper" :
            {
                Creeper m = new Creeper(EntityType.CREEPER, level);
                switch (mobLevel){
                    case 1:
                        Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(20);
                        m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                        m.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,12000,0));
                        m.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,12000,1));
                        m.setSilent(true);
                        level.addFreshEntity(m);
                        break;
                    case 2:
                        Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(20);
                        Objects.requireNonNull(m.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(5*m.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue());
                        m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                        //m.addEffect(new MobEffectInstance(Malefic.MOBEFFECT_SWIFTFUSE.get(),9999999,0,true,false,false));
                        m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,12000,1));
                        level.addFreshEntity(m);
                        break;
                    case 3:
                        Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(80);
                        Objects.requireNonNull(m.getAttribute(Attributes.KNOCKBACK_RESISTANCE)).setBaseValue(4*m.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue());
                        Objects.requireNonNull(m.getAttribute(Attributes.FOLLOW_RANGE)).setBaseValue(80);
                        m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                        m.thunderHit(level,new LightningBolt(EntityType.LIGHTNING_BOLT,level));
                        level.addFreshEntity(m);
                        break;
                }
            }
            break;
            case "enderman" :
            {
                EnderMan m = new EnderMan(EntityType.ENDERMAN, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(200);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);

            }
            break;
            case "guardian" :
            {
                Guardian m = new Guardian(EntityType.GUARDIAN, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(30);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(m.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue()*3);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "vindicator" :
            {
                Vindicator m = new Vindicator(EntityType.VINDICATOR, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(50);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(8);
                m.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "pillager" :
            {
                Pillager m = new Pillager(EntityType.PILLAGER, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(50);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(4);
                m.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.CROSSBOW));
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "illusioner" :
            {
                Illusioner m = new Illusioner(EntityType.ILLUSIONER, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(70);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(4);
                m.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "blaze" :
            {
                Blaze m = new Blaze(EntityType.BLAZE, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(50);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(12);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "firevex" :
            {
                Vex m = new Vex(EntityType.VEX, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(14);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(m.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue()*2);
                ItemStack is = new ItemStack(Items.BLAZE_ROD);
                is.enchant(Enchantments.SHARPNESS, (int)(Math.random()*5.5));
                is.enchant(Enchantments.FIRE_ASPECT, 3+(int)(Math.random()*2.5));
                m.setItemSlot(EquipmentSlot.MAINHAND,is);
                m.setDropChance(EquipmentSlot.MAINHAND,0.05f);
                m.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,9999999,0,true,false));
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "vex" :
            {
                // Vex spawn unchanged.
            }
            break;
            case "piglin" :
            {
                Piglin m = new Piglin(EntityType.PIGLIN, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(50);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(8);
                m.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.CROSSBOW));
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "ghast" :
            {
                Ghast m = new Ghast(EntityType.GHAST, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(500);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "hoglin" :
            {

                Hoglin m = new Hoglin(EntityType.HOGLIN, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(450);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(12);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "piglin_brute" :
            {

                PiglinBrute m = new PiglinBrute(EntityType.PIGLIN_BRUTE, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(120);
                Objects.requireNonNull(m.getAttribute(Attributes.ARMOR)).setBaseValue(14);
                m.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.GOLDEN_AXE));
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
            case "elder_guardian" :
            {
                ElderGuardian m = new ElderGuardian(EntityType.ELDER_GUARDIAN, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(660);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(64);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);

            }
            break;
            case "evokerRavager" :
            {

                Evoker m = new Evoker(EntityType.EVOKER, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(300);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(36);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
                Ravager m1 = new Ravager(EntityType.RAVAGER, level);
                Objects.requireNonNull(m1.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(450);
                m1.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m1);
                m.startRiding(m1);
            }
            break;
            case "evoker" :
            {
                Evoker m = new Evoker(EntityType.EVOKER, level);
                Objects.requireNonNull(m.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(300);
                Objects.requireNonNull(m.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(36);
                m.moveTo((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(m);
            }
            break;
        }
    }
}

enum RandomEnchantType {
    ARMOR, CHEST_ARMOR, MELEE_WEAPON, BOW;
}