package xyz.xenondevs.nova.machines.registry

import xyz.xenondevs.nova.machines.Machines
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerItem
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerUnnamedItem

object GUIMaterials {
    
    val GEAR_BTN_OFF = registerItem(Machines, "gui_gear_btn_off", localizedName = "menu.machines.mechanical_press.press_gears")
    val GEAR_BTN_ON = registerItem(Machines, "gui_gear_btn_on", localizedName = "menu.machines.mechanical_press.press_gears")
    val PLATE_BTN_OFF = registerItem(Machines, "gui_plate_btn_off", localizedName = "menu.machines.mechanical_press.press_plates")
    val PLATE_BTN_ON = registerItem(Machines, "gui_plate_btn_on", localizedName = "menu.machines.mechanical_press.press_plates")
    val NBT_BTN_OFF = registerItem(Machines, "gui_nbt_btn_off", localizedName = "menu.machines.mob_duplicator.nbt.off")
    val NBT_BTN_ON = registerItem(Machines, "gui_nbt_btn_on", localizedName = "menu.machines.mob_duplicator.nbt.on")
    val COBBLESTONE_MODE_BTN = registerItem(Machines, "gui_cobblestone_btn", localizedName = "menu.machines.cobblestone_generator.mode.cobblestone")
    val STONE_MODE_BTN = registerItem(Machines, "gui_stone_btn", localizedName = "menu.machines.cobblestone_generator.mode.stone")
    val OBSIDIAN_MODE_BTN = registerItem(Machines, "gui_obsidian_btn", localizedName = "menu.machines.cobblestone_generator.mode.obsidian")
    val PUMP_MODE_BTN = registerItem(Machines, "gui_pump_pump_btn", localizedName = "menu.machines.pump.pump_mode")
    val PUMP_REPLACE_MODE_BTN = registerItem(Machines, "gui_pump_replace_btn",localizedName =  "menu.machines.pump.replace_mode")
    val ICE_MODE_BTN = registerItem(Machines, "gui_ice_btn", localizedName = "menu.machines.freezer.mode.ice")
    val PACKED_ICE_MODE_BTN = registerItem(Machines, "gui_packed_ice_btn", localizedName = "menu.machines.freezer.mode.packed_ice")
    val BLUE_ICE_MODE_BTN = registerItem(Machines, "gui_blue_ice_btn", localizedName = "menu.machines.freezer.mode.blue_ice")
    val HOE_BTN_ON = registerItem(Machines, "gui_hoe_btn_on", localizedName = "menu.machines.planter.autotill.on")
    val HOE_BTN_OFF = registerItem(Machines, "gui_hoe_btn_off", localizedName ="menu.machines.planter.autotill.off")
    val FLUID_LEFT_RIGHT_BTN = registerItem(Machines, "gui_fluid_left_right_btn", localizedName = "menu.machines.fluid_infuser.mode.insert")
    val FLUID_RIGHT_LEFT_BTN = registerItem(Machines, "gui_fluid_right_left_btn", localizedName = "menu.machines.fluid_infuser.mode.extract")
    
    val TP_GREEN_PLUS = registerUnnamedItem(Machines, "gui_green_plus")
    val TP_RED_MINUS = registerUnnamedItem(Machines, "gui_red_minus")
    
    val AXE_PLACEHOLDER = registerUnnamedItem(Machines, "gui_axe_placeholder")
    val HOE_PLACEHOLDER = registerUnnamedItem(Machines, "gui_hoe_placeholder")
    val SHEARS_PLACEHOLDER = registerUnnamedItem(Machines, "gui_shears_placeholder")
    val BOTTLE_PLACEHOLDER = registerUnnamedItem(Machines, "gui_bottle_placeholder")
    val FISHING_ROD_PLACEHOLDER = registerUnnamedItem(Machines, "gui_fishing_rod_placeholder")
    val MOB_CATCHER_PLACEHOLDER = registerUnnamedItem(Machines, "gui_mob_catcher_placeholder")
    val SAPLING_PLACEHOLDER = registerUnnamedItem(Machines, "gui_sapling_placeholder")
    
    val ARROW_PROGRESS = registerUnnamedItem(Machines, "gui_arrow_progress")
    val ENERGY_PROGRESS = registerUnnamedItem(Machines, "gui_energy_progress")
    val PULVERIZER_PROGRESS = registerUnnamedItem(Machines, "gui_pulverizer_progress")
    val PRESS_PROGRESS = registerUnnamedItem(Machines, "gui_press_progress")
    val TP_BREW_PROGRESS = registerUnnamedItem(Machines, "gui_brew_progress")
    val FLUID_PROGRESS_LEFT_RIGHT = registerUnnamedItem(Machines, "gui_fluid_progress_left_right")
    val FLUID_PROGRESS_RIGHT_LEFT = registerUnnamedItem(Machines, "gui_fluid_progress_right_left")
    val TP_FLUID_PROGRESS_LEFT_RIGHT = registerUnnamedItem(Machines, "gui_tp_fluid_progress_left_right")
    val TP_FLUID_PROGRESS_RIGHT_LEFT = registerUnnamedItem(Machines, "gui_tp_fluid_progress_right_left")
    
}