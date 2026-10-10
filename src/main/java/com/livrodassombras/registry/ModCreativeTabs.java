package com.livrodassombras.registry;

import com.livrodassombras.LivroDasSombras;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** The mod's own creative tab. Add new items to displayItems(...) below. */
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LivroDasSombras.MOD_ID);

    public static final RegistryObject<CreativeModeTab> LIVRO_DAS_SOMBRAS_TAB = TABS.register("livro_das_sombras",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.livro_das_sombras"))
                    .icon(() -> new ItemStack(ModItems.LIVRO_DAS_SOMBRAS.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.LIVRO_DAS_SOMBRAS.get());
                        output.accept(ModItems.LIVRO_DO_SANGUE.get());
                        output.accept(ModItems.LIVRO_DO_TEMPO.get());
                        output.accept(ModItems.CALDEIRAO_RITUALISTICO.get());
                        output.accept(ModItems.ESPADA_DE_SANGUE.get());
                        output.accept(ModItems.LANCA_DE_SANGUE.get());
                        output.accept(ModItems.MARTELO_DE_SANGUE.get());
                        output.accept(ModItems.ARCO_DE_SANGUE.get());
                        output.accept(ModItems.FOICE_DO_TEMPO.get());
                        output.accept(ModItems.RELOGIO_NEGRO.get());
                        // Future content goes here.
                    })
                    .build());
}
