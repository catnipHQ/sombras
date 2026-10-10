package com.livrodassombras.registry;

import com.livrodassombras.LivroDasSombras;
import com.livrodassombras.item.BloodBowItem;
import com.livrodassombras.item.BloodHammerItem;
import com.livrodassombras.item.BloodSpearItem;
import com.livrodassombras.item.EspadaDeSangueItem;
import com.livrodassombras.item.LivroDasSombrasItem;
import com.livrodassombras.item.LivroDoSangueItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, LivroDasSombras.MOD_ID);

    // Books
    public static final RegistryObject<Item> LIVRO_DAS_SOMBRAS = ITEMS.register("livro_das_sombras",
            () -> new LivroDasSombrasItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> LIVRO_DO_SANGUE = ITEMS.register("livro_do_sangue",
            () -> new LivroDoSangueItem(new Item.Properties().stacksTo(1)));

    // Summoned weapons
    public static final RegistryObject<Item> ESPADA_DE_SANGUE = ITEMS.register("espada_de_sangue",
            () -> new EspadaDeSangueItem(new Item.Properties()));

    public static final RegistryObject<Item> LANCA_DE_SANGUE = ITEMS.register("lanca_de_sangue",
            () -> new BloodSpearItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MARTELO_DE_SANGUE = ITEMS.register("martelo_de_sangue",
            () -> new BloodHammerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ARCO_DE_SANGUE = ITEMS.register("arco_de_sangue",
            () -> new BloodBowItem(new Item.Properties().stacksTo(1)));
}
