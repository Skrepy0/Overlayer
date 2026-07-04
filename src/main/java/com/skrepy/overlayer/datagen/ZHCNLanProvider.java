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
        add("overlayer.configuration.title", "Overlayer");

        add("overlayer.screen.common.save", "保存");
        add("overlayer.screen.common.cancel", "取消");
        add("overlayer.screen.common.delete", "删除");
        add("overlayer.screen.common.select_pic", "选择图片文件");
        add("overlayer.screen.common.delete_all", "清空");

        add("overlayer.screen.settings_page.title", "Overlayer");
        add("overlayer.screen.settings_page.button.add", "添加");
        add("overlayer.screen.settings_page.button.clear", "清空列表");
        add("overlayer.screen.delete_confirm.title", "确认删除");
        add("overlayer.screen.delete_confirm.meg", "确定要删除这个图片实例吗？");
        add("overlayer.screen.delete_all_confirm.title", "确认清空");
        add("overlayer.screen.delete_all_confirm.meg", "确定要清空所有图片实例吗, 此过程不可逆？");

        add("overlayer.screen.image_edit.title", "编辑图片实例");
        add("overlayer.screen.image_edit.button.mode", "模式:");
        add("overlayer.screen.image_edit.button.mode.always", "总是");
        add("overlayer.screen.image_edit.button.mode.ingame", "仅游戏中");
        add("overlayer.screen.image_edit.button.mode.not_ingame", "仅非游戏中");
        add("overlayer.screen.image_edit.button.mode.disable", "禁用");
        add("overlayer.screen.image_edit.label.preview", "预览");
        add("overlayer.screen.image_edit.label.layer", "图层:");
        add("overlayer.screen.image_edit.slide.zoom", "缩放: ");
        add("overlayer.screen.image_edit.slide.alpha", "透明度: ");

    }
}
