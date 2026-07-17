package com.skrepy.overlayer.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

public class ModENUSLangProvider extends FabricLanguageProvider {

    public ModENUSLangProvider(FabricDataOutput dataOutput) {
        super(dataOutput, "en_us");
    }

    @Override
    public void generateTranslations(TranslationBuilder translationBuilder) {
        translationBuilder.add("category.overlayer.general", "Overlayer General Configuration");
        translationBuilder.add("key.overlayer.toggle_show_status", "Toggle Instance Display Status");
        translationBuilder.add("overlayer.configuration.title", "Overlayer");

        translationBuilder.add("overlayer.configuration", "Overlayer UI Configuration");
        translationBuilder.add("overlayer.configuration.titleScreenBtnXOffset", "Title Screen Button X Offset");
        translationBuilder.add("overlayer.configuration.titleScreenBtnYOffset", "Title Screen Button Y Offset");
        translationBuilder.add("overlayer.configuration.optionsScreenBtnXOffset", "Options Screen Button X Offset");
        translationBuilder.add("overlayer.configuration.optionsScreenBtnYOffset", "Options Screen Button Y Offset");

        translationBuilder.add("overlayer.screen.common.save", "Save");
        translationBuilder.add("overlayer.screen.common.cancel", "Cancel");
        translationBuilder.add("overlayer.screen.common.delete", "Delete");
        translationBuilder.add("overlayer.screen.common.select_pic", "Select Image File");
        translationBuilder.add("overlayer.screen.common.delete_all", "§cClear All§r");

        translationBuilder.add("overlayer.screen.button.select_file.tooltip", "Select Image File");
        translationBuilder.add("overlayer.screen.button.config.tooltip", "Overlayer Configuration\nHold §6[SHIFT]§r to enter UI configuration screen");

        translationBuilder.add("overlayer.screen.settings_page.title", "Overlayer");
        translationBuilder.add("overlayer.screen.settings_page.button.add", "Add");
        translationBuilder.add("overlayer.screen.settings_page.button.clear", "§cClear List§r");
        translationBuilder.add("overlayer.screen.delete_confirm.title", "Confirm Delete");
        translationBuilder.add("overlayer.screen.delete_confirm.meg", "Are you sure you want to delete this image instance?");
        translationBuilder.add("overlayer.screen.delete_all_confirm.title", "Confirm Clear All");
        translationBuilder.add("overlayer.screen.delete_all_confirm.meg", "§6Are you sure you want to clear all image instances? This action is irreversible.§r");

        translationBuilder.add("overlayer.screen.image_edit.title", "Edit Image Instance");
        translationBuilder.add("overlayer.screen.image_edit.button.mode", "Mode: ");
        translationBuilder.add("overlayer.screen.image_edit.button.mode.always", "Always");
        translationBuilder.add("overlayer.screen.image_edit.button.mode.ingame", "In-Game");
        translationBuilder.add("overlayer.screen.image_edit.button.mode.not_ingame", "Not In-Game");
        translationBuilder.add("overlayer.screen.image_edit.button.mode.disable", "§cDisabled§r");
        translationBuilder.add("overlayer.screen.image_edit.label.preview", "Preview");
        translationBuilder.add("overlayer.screen.image_edit.label.layer", "Layer:");
        translationBuilder.add("overlayer.screen.image_edit.slide.zoom", "Zoom: ");
        translationBuilder.add("overlayer.screen.image_edit.slide.alpha", "Opacity: ");
        translationBuilder.add("overlayer.screen.image_edit.slide.rotation", "Rotation: ");


        translationBuilder.add("overlayer.toast.warning.unsupported_format.title", "Unsupported Format");
        translationBuilder.add("overlayer.toast.warning.unsupported_format.meg", "Overlayer does not currently support the %s format.");
        translationBuilder.add("overlayer.toast.warning.invalid_path.title", "Invalid Path");
        translationBuilder.add("overlayer.toast.warning.invalid_path.meg.no_extension", "No file extension explicitly specified.");
        translationBuilder.add("overlayer.toast.warning.invalid_path.meg.no_file", "The file under this path does not exist!");
        translationBuilder.add("overlayer.toast.warning.invalid_path.meg.empty_path", "Image path cannot be empty!");

        translationBuilder.add("overlayer.screen.unsaved.title", "Unsaved");
        translationBuilder.add("overlayer.screen.unsaved.meg", "Changes you have made will not be saved. Do you want to exit?");
    }
}
