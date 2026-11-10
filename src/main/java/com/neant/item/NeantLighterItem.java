package com.neant.item;

import com.neant.server.Powers;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Briquet du Neant : une vague de flammes qui s'etend et embrase tout, TNT comprise. */
public class NeantLighterItem extends Item {
    public static final int COOLDOWN = 20 * 7;

    public NeantLighterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canBeDepleted() {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Vec3 center = Vec3.atCenterOf(ctx.getClickedPos().relative(ctx.getClickedFace()));
        boolean ok = PowerUse.tryActivate(this, (ServerLevel level, ServerPlayer sp, ItemStack s, InteractionHand h) -> {
            Powers.fireWave(level, sp, center);
            return true;
        }, COOLDOWN, ctx.getLevel(), player, ctx.getHand());
        return ok ? InteractionResult.sidedSuccess(ctx.getLevel().isClientSide()) : InteractionResult.FAIL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return PowerUse.use(this, Powers::lighterAir, COOLDOWN, level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        PowerUse.tooltip(this, COOLDOWN, tip);
    }
}
