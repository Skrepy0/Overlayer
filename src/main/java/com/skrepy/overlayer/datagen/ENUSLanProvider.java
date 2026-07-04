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
        add("overlayer.configuration.title", "Overlayer");

        add("overlayer.screen.common.save", "Save");
        add("overlayer.screen.common.cancel", "Cancel");
        add("overlayer.screen.common.delete", "Delete");
        add("overlayer.screen.common.select_pic", "Select Image File");
        add("overlayer.screen.common.delete_all", "Clear All");

        add("overlayer.screen.settings_page.title", "Overlayer");
        add("overlayer.screen.settings_page.button.add", "Add");
        add("overlayer.screen.settings_page.button.clear", "Clear List");
        add("overlayer.screen.delete_confirm.title", "Confirm Delete");
        add("overlayer.screen.delete_confirm.meg", "Are you sure you want to delete this image instance?");
        add("overlayer.screen.delete_all_confirm.title", "Confirm Clear All");
        add("overlayer.screen.delete_all_confirm.meg", "Are you sure you want to clear all image instances? This action is irreversible.?");

        add("overlayer.screen.image_edit.title", "Edit Image Instance");
        add("overlayer.screen.image_edit.button.mode", "Mode:");
        add("overlayer.screen.image_edit.button.mode.always", "Always");
        add("overlayer.screen.image_edit.button.mode.ingame", "In-Game");
        add("overlayer.screen.image_edit.button.mode.not_ingame", "Not In-Game");
        add("overlayer.screen.image_edit.button.mode.disable", "Disabled");
        add("overlayer.screen.image_edit.label.preview", "Preview");
        add("overlayer.screen.image_edit.label.layer", "Layer:");
        add("overlayer.screen.image_edit.slide.zoom", "Zoom: ");
        add("overlayer.screen.image_edit.slide.alpha", "Opacity: ");
    }
}
