package com.livrodassombras.registry;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.block.RitualCauldronBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LivroDasSombras.MOD_ID);

    @SuppressWarnings("DataFlowIssue") // the data fixer type is allowed to be null
    public static final RegistryObject<BlockEntityType<RitualCauldronBlockEntity>> CALDEIRAO_RITUALISTICO =
            BLOCK_ENTITIES.register("caldeirao_ritualistico", () -> BlockEntityType.Builder
                    .of(RitualCauldronBlockEntity::new, ModBlocks.CALDEIRAO_RITUALISTICO.get())
                    .build(null));
}
