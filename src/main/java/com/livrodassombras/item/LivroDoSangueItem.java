package com.livrodassombras.item;

import com.livrodassombras.client.ClientHooks;
import com.livrodassombras.ritual.BookPath;
import com.livrodassombras.ritual.MinorRitual;
import com.livrodassombras.ritual.MinorRituals;
import com.livrodassombras.ritual.RitualBook;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Upgraded Livro das Sombras. Right-click opens the book: I. Invocações (weapons and the Ser de Sangue)
 * or II. Rituais menores. A chosen section stays locked (the book reopens on it) until "Back" is used.
 * Choosing a ritual charges the book (an eye opens on its cover): right-click then casts it
 * (costing one of the book's uses), Shift + right-click cancels it. The R wheel picks from the locked section.
 */
public class LivroDoSangueItem extends LimitedBookItem {
    public LivroDoSangueItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack book = player.getItemInHand(hand);
        MinorRitual charged = RitualBook.charged(book);

        if (charged == null) {
            if (level.isClientSide) {
                int section = RitualBook.section(book);
                BookPath path = this.path();
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openBook(path, section));
            }
            return InteractionResultHolder.consume(book);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                MinorRituals.cancel(player, book);
            } else {
                MinorRituals.cast(serverPlayer, book, hand, charged);
            }
        }
        return InteractionResultHolder.sidedSuccess(book, level.isClientSide);
    }

    /** Which book this is: the Livro do Tempo overrides it. */
    public BookPath path() {
        return BookPath.BLOOD;
    }

    @Override
    public Component getName(ItemStack stack) {
        MinorRitual charged = RitualBook.charged(stack);
        return charged == null
                ? super.getName(stack)
                : Component.translatable(this.getDescriptionId() + ".carregado", charged.displayName())
                        .withStyle(this.path().color());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return RitualBook.charged(stack) != null || super.isFoil(stack);
    }

    /** Charging, casting and cooldowns change the stack's data; don't make the hand bob every time. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        MinorRitual charged = RitualBook.charged(stack);
        if (charged != null) {
            tooltip.add(Component.translatable("tooltip.livro_das_sombras.ritual", charged.displayName())
                    .withStyle(this.path().color()));
            tooltip.add(Component.translatable("tooltip.livro_das_sombras.ritual.uso").withStyle(ChatFormatting.GRAY));
        }
    }
}
