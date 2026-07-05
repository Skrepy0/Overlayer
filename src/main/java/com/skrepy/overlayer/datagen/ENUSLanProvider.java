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

        add("overlayer.configuration.titleScreenBtnXOffset", "Title Screen Button X Offset");
        add("overlayer.configuration.titleScreenBtnYOffset", "Title Screen Button Y Offset");
        add("overlayer.configuration.optionsScreenBtnXOffset", "Options Screen Button X Offset");
        add("overlayer.configuration.optionsScreenBtnYOffset", "Options Screen Button Y Offset");

        add("overlayer.screen.common.save", "Save");
        add("overlayer.screen.common.cancel", "Cancel");
        add("overlayer.screen.common.delete", "Delete");
        add("overlayer.screen.common.select_pic", "Select Image File");
        add("overlayer.screen.common.delete_all", "§cClear All§r");

        add("overlayer.screen.button.select_file.tooltip", "Select Image File");
        add("overlayer.screen.button.config.tooltip", "Overlayer Configuration");

        add("overlayer.screen.settings_page.title", "Overlayer");
        add("overlayer.screen.settings_page.button.add", "Add");
        add("overlayer.screen.settings_page.button.clear", "§cClear List§r");
        add("overlayer.screen.delete_confirm.title", "Confirm Delete");
        add("overlayer.screen.delete_confirm.meg", "Are you sure you want to delete this image instance?");
        add("overlayer.screen.delete_all_confirm.title", "Confirm Clear All");
        add("overlayer.screen.delete_all_confirm.meg", "§6Are you sure you want to clear all image instances? This action is irreversible.§r");

        add("overlayer.screen.image_edit.title", "Edit Image Instance");
        add("overlayer.screen.image_edit.button.mode", "Mode: ");
        add("overlayer.screen.image_edit.button.mode.always", "Always");
        add("overlayer.screen.image_edit.button.mode.ingame", "In-Game");
        add("overlayer.screen.image_edit.button.mode.not_ingame", "Not In-Game");
        add("overlayer.screen.image_edit.button.mode.disable", "§cDisabled§r");
        add("overlayer.screen.image_edit.label.preview", "Preview");
        add("overlayer.screen.image_edit.label.layer", "Layer:");
        add("overlayer.screen.image_edit.slide.zoom", "Zoom: ");
        add("overlayer.screen.image_edit.slide.alpha", "Opacity: ");

        add("overlayer.toast.warning.unsupported_format.title", "Unsupported Format");
        add("overlayer.toast.warning.unsupported_format.meg", "Overlayer does not currently support the %s format.");
        add("overlayer.toast.warning.invalid_path.title", "Invalid Path");
        add("overlayer.toast.warning.invalid_path.meg.no_extension", "No file extension explicitly specified.");
        add("overlayer.toast.warning.invalid_path.meg.no_file", "The file under this path does not exist!");
        add("overlayer.toast.warning.invalid_path.meg.empty_path", "Image path cannot be empty!");

        add("overlayer.screen.unsaved.title", "Unsaved");
        add("overlayer.screen.unsaved.meg", "Changes you have made will not be saved. Do you want to exit?");
    }
}
