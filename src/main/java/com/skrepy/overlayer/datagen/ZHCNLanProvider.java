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

        add("overlayer.configuration.titleScreenBtnXOffset", "主界面按钮X轴偏移");
        add("overlayer.configuration.titleScreenBtnYOffset", "主界面按钮Y轴偏移");
        add("overlayer.configuration.optionsScreenBtnXOffset", "设置界面按钮X轴偏移");
        add("overlayer.configuration.optionsScreenBtnYOffset", "设置界面按钮Y轴偏移");

        add("overlayer.screen.common.save", "保存");
        add("overlayer.screen.common.cancel", "取消");
        add("overlayer.screen.common.delete", "删除");
        add("overlayer.screen.common.select_pic", "选择图片文件");
        add("overlayer.screen.common.delete_all", "§c清空§r");

        add("overlayer.screen.button.select_file.tooltip", "选择图片文件");
        add("overlayer.screen.button.config.tooltip", "Overlayer配置");

        add("overlayer.screen.settings_page.title", "Overlayer");
        add("overlayer.screen.settings_page.button.add", "添加");
        add("overlayer.screen.settings_page.button.clear", "§c清空列表§r");
        add("overlayer.screen.delete_confirm.title", "确认删除");
        add("overlayer.screen.delete_confirm.meg", "确定要删除这个图片实例吗？");
        add("overlayer.screen.delete_all_confirm.title", "确认清空");
        add("overlayer.screen.delete_all_confirm.meg", "§6确定要清空所有图片实例吗, 此过程不可逆？§r");

        add("overlayer.screen.image_edit.title", "编辑图片实例");
        add("overlayer.screen.image_edit.button.mode", "模式: ");
        add("overlayer.screen.image_edit.button.mode.always", "总是");
        add("overlayer.screen.image_edit.button.mode.ingame", "仅游戏中");
        add("overlayer.screen.image_edit.button.mode.not_ingame", "仅非游戏中");
        add("overlayer.screen.image_edit.button.mode.disable", "§c禁用§r");
        add("overlayer.screen.image_edit.label.preview", "预览");
        add("overlayer.screen.image_edit.label.layer", "图层:");
        add("overlayer.screen.image_edit.slide.zoom", "缩放: ");
        add("overlayer.screen.image_edit.slide.alpha", "透明度: ");
        add("overlayer.screen.image_edit.slide.rotation", "旋转: ");

        add("overlayer.toast.warning.unsupported_format.title", "不支持的格式");
        add("overlayer.toast.warning.unsupported_format.meg", "Overlayer暂不支持%s格式");
        add("overlayer.toast.warning.invalid_path.title", "无效路径");
        add("overlayer.toast.warning.invalid_path.meg.no_extension", "没有显式指定的图片格式");
        add("overlayer.toast.warning.invalid_path.meg.no_file", "此路径下的文件不存在!");
        add("overlayer.toast.warning.invalid_path.meg.empty_path", "图片路径不能为空!");

        add("overlayer.screen.unsaved.title", "未保存");
        add("overlayer.screen.unsaved.meg", "系统不会保存当前您作出的更改, 是否要退出?");

    }
}
