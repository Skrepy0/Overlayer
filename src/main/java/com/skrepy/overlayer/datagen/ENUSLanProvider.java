package com.skrepy.overlayer.datagen;

import com.skrepy.overlayer.Overlayer;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ENUSLanProvider extends LanguageProvider {
    public ENUSLanProvider(PackOutput output) {
        super(output, Overlayer.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("overlayer.test", "Data Generation Test");
    }
}
