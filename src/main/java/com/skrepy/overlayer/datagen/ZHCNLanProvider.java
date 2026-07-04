package com.skrepy.overlayer.datagen;

import com.skrepy.overlayer.Overlayer;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ZHCNLanProvider extends LanguageProvider {

    public ZHCNLanProvider(PackOutput output) {
        super(output, Overlayer.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add("overlayer.test", "数据生成测试");
    }
}
