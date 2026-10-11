package com.livrodassombras.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Key bindings (changeable in Options > Controls, category "Veritates"). */
public final class ModKeys {
    public static final String CATEGORY = "key.categories.livro_das_sombras";

    /** Opens the selection wheel of the Livro do Sangue's locked section. Default: R. */
    public static final KeyMapping WHEEL = new KeyMapping("key.livro_das_sombras.roda",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    private ModKeys() {}
}
