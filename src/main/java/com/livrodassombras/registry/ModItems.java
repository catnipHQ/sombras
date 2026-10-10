package com.livrodassombras.registry;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.item.EspadaDeSangueItem;
import com.livrodassombras.item.LivroDasSombrasItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, LivroDasSombras.MOD_ID);

    public static final RegistryObject<Item> LIVRO_DAS_SOMBRAS = ITEMS.register("livro_das_sombras",
            () -> new LivroDasSombrasItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ESPADA_DE_SANGUE = ITEMS.register("espada_de_sangue",
            () -> new EspadaDeSangueItem(new Item.Properties()));
}
