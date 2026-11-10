package com.neant.item;

import com.neant.server.Powers;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Pelle du Neant : avale des blobs entiers de terre, clic droit = trou noir qui efface une sphere de terrain. */
public class NeantShovelItem extends ShovelItem {
    public static final int COOLDOWN = 20 * 11;

    public NeantShovelItem(Properties properties) {
        super(Tiers.NETHERITE, 1.5F, -3.0F, properties);
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
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float base = super.getDestroySpeed(stack, state);
        return base > 1.0F ? base * 3.0F : base;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer sp) || player.isShiftKeyDown()) {
            return false;
        }
        return Powers.shovelBreak(sp, pos);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return PowerUse.use(this, Powers::voidHole, COOLDOWN, level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        PowerUse.tooltip(this, COOLDOWN, tip);
    }
}
