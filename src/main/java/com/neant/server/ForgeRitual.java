package com.neant.server;

import com.neant.net.VfxPacket;
import com.neant.net.VfxType;
import com.neant.registry.ModBlocks;
import com.neant.registry.ModItems;
import com.neant.registry.ModSounds;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Rituel de la forge : les ingredients flottent et tournent, puis un eclair forge l'objet. */
public final class ForgeRitual implements ServerEffects.Timed {
    public static final int DURATION = 70;
    private static final Set<BlockPos> ACTIVE = new HashSet<>();

    private final BlockPos pos;
    private final ServerPlayer player;
    private final ForgeRecipes.Recipe recipe;
    private final ItemEntity baseEntity;
    private final List<ItemEntity> shardEntities;
    private int age;
    private boolean finished;

    private ForgeRitual(BlockPos pos, ServerPlayer player, ForgeRecipes.Recipe recipe, ItemEntity baseEntity,
                        List<ItemEntity> shardEntities) {
        this.pos = pos.immutable();
        this.player = player;
        this.recipe = recipe;
        this.baseEntity = baseEntity;
        this.shardEntities = shardEntities;
    }

    public static void tryStart(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (ACTIVE.contains(pos)) {
            return;
        }
        AABB box = new AABB(pos.above()).inflate(1.4D, 0.8D, 1.4D);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive);
        Item shardItem = ModItems.VOID_SHARD.get();
        List<ItemEntity> shardEntities = new ArrayList<>();
        int shards = 0;
        for (ItemEntity e : items) {
            if (e.getItem().is(shardItem)) {
                shardEntities.add(e);
                shards += e.getItem().getCount();
            }
        }
        for (ForgeRecipes.Recipe r : ForgeRecipes.all()) {
            Item base = r.base().get();
            ItemEntity baseEntity = null;
            for (ItemEntity e : items) {
                if (e.getItem().is(base)) {
                    baseEntity = e;
                    break;
                }
            }
            if (baseEntity == null) {
                continue;
            }
            if (shards < r.shards()) {
                player.displayClientMessage(Component.translatable("message.neant.forge.missing",
                        r.shards(), shards, base.getDescription()), true);
                return;
            }
            ACTIVE.add(pos.immutable());
            ServerEffects.add(level, new ForgeRitual(pos, player, r, baseEntity, shardEntities));
            return;
        }
        player.displayClientMessage(Component.translatable("message.neant.forge.help"), true);
    }

    private List<ItemEntity> involved() {
        List<ItemEntity> all = new ArrayList<>();
        all.add(baseEntity);
        all.addAll(shardEntities);
        return all;
    }

    @Override
    public boolean tick(ServerLevel level) {
        if (!level.getBlockState(pos).is(ModBlocks.VOID_FORGE.get())) {
            return true;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        if (age == 0) {
            VfxPacket.of(VfxType.RITUAL, center.add(0, 0.5, 0)).duration(DURATION + 30).send(level);
            ModSounds.play(level, center, ModSounds.FORGE_CHARGE, 4.0F, 1.0F);
        }

        List<ItemEntity> all = involved();
        float charge = Math.min(1.0F, age / 50.0F);
        for (int i = 0; i < all.size(); i++) {
            ItemEntity e = all.get(i);
            if (!e.isAlive()) {
                continue;
            }
            double ang = age * (0.12D + 0.2D * charge) + i * (Math.PI * 2.0D / Math.max(1, all.size()));
            double radius = i == 0 ? 0.0D : 0.9D * (1.0D - 0.55D * charge);
            double y = 1.4D + 0.9D * charge + Math.sin(age * 0.2D + i) * 0.08D;
            e.setNoGravity(true);
            e.setDeltaMovement(Vec3.ZERO);
            e.setPos(center.x + Math.cos(ang) * radius, pos.getY() + y, center.z + Math.sin(ang) * radius);
            e.setPickUpDelay(40);
            e.hurtMarked = true;
        }
        age++;
        finished = age >= DURATION;
        return finished;
    }

    @Override
    public void end(ServerLevel level) {
        ACTIVE.remove(pos);
        List<ItemEntity> all = involved();
        for (ItemEntity e : all) {
            if (e.isAlive()) {
                e.setNoGravity(false);
                e.setPickUpDelay(10);
            }
        }
        if (!finished || !level.getBlockState(pos).is(ModBlocks.VOID_FORGE.get())) {
            return;
        }

        // on consomme les ingredients
        if (baseEntity.isAlive()) {
            ItemStack s = baseEntity.getItem();
            s.shrink(1);
            if (s.isEmpty()) {
                baseEntity.discard();
            } else {
                baseEntity.setItem(s);
            }
        }
        int need = recipe.shards();
        for (ItemEntity e : shardEntities) {
            if (need <= 0 || !e.isAlive()) {
                continue;
            }
            ItemStack s = e.getItem();
            int take = Math.min(need, s.getCount());
            s.shrink(take);
            need -= take;
            if (s.isEmpty()) {
                e.discard();
            } else {
                e.setItem(s);
            }
        }

        Vec3 center = Vec3.atCenterOf(pos);
        ItemStack result = new ItemStack(recipe.result().get());
        ItemEntity out = new ItemEntity(level, center.x, pos.getY() + 1.6D, center.z, result);
        out.setDeltaMovement(0.0D, 0.45D, 0.0D);
        out.setPickUpDelay(30);
        out.setUnlimitedLifetime();
        out.setInvulnerable(true);
        level.addFreshEntity(out);

        VfxPacket.of(VfxType.SMITE, center.add(0, 0.5, 0)).duration(34).param(1.4F).color(0xB06CFF).send(level);
        VfxPacket.of(VfxType.PULSE, center.add(0, 0.6, 0)).duration(40).param(10.0F).color(0xB06CFF).send(level);
        ModSounds.play(level, center, ModSounds.FORGE_DONE, 5.0F, 1.0F);
        ModSounds.play(level, center, ModSounds.BOOM, 4.0F, 1.3F);
        for (net.minecraft.world.entity.LivingEntity e : Abilities.around(level, player, center, 7.0D)) {
            Abilities.knock(e, center, 1.2D, 0.5D);
        }
        player.displayClientMessage(Component.translatable("message.neant.forge.done", result.getHoverName()), true);
    }
}
