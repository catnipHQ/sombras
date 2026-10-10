package com.livrodassombras.registry;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.block.BloodStainBlock;
import com.livrodassombras.block.RitualCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, LivroDasSombras.MOD_ID);

    public static final RegistryObject<Block> CALDEIRAO_RITUALISTICO = BLOCKS.register("caldeirao_ritualistico",
            () -> new RitualCauldronBlock(BlockBehaviour.Properties.copy(Blocks.CAULDRON)
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(RitualCauldronBlock.LIT) ? 9 : 0)));

    /** Decoration left by the cauldron's blood price. No item; it goes away when the ritual ends. */
    public static final RegistryObject<Block> MANCHA_DE_SANGUE = BLOCKS.register("mancha_de_sangue",
            () -> new BloodStainBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .instabreak()
                    .noLootTable()
                    .replaceable()
                    .sound(SoundType.SLIME_BLOCK)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
}
