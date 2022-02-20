package xyz.xenondevs.nova.machines.registry

import xyz.xenondevs.nova.machines.MACHINES
import xyz.xenondevs.nova.material.NovaMaterialRegistry.registerItem

object GUIMaterials {
    
    val GEAR_BTN_OFF = registerItem(MACHINES, "gui_gear_btn_off", "menu.machines.mechanical_press.press_gears")
    val GEAR_BTN_ON = registerItem(MACHINES, "gui_gear_btn_on", "menu.machines.mechanical_press.press_gears")
    val PLATE_BTN_OFF = registerItem(MACHINES, "gui_plate_btn_off", "menu.machines.mechanical_press.press_plates")
    val PLATE_BTN_ON = registerItem(MACHINES, "gui_plate_btn_on", "menu.machines.mechanical_press.press_plates")
    val NBT_BTN_OFF = registerItem(MACHINES, "gui_nbt_btn_off", "menu.machines.mob_duplicator.nbt.off")
    val NBT_BTN_ON = registerItem(MACHINES, "gui_nbt_btn_on", "menu.machines.mob_duplicator.nbt.on")
    val COBBLESTONE_MODE_BTN = registerItem(MACHINES, "gui_cobblestone_btn", "menu.machines.cobblestone_generator.mode.cobblestone")
    val STONE_MODE_BTN = registerItem(MACHINES, "gui_stone_btn", "menu.machines.cobblestone_generator.mode.stone")
    val OBSIDIAN_MODE_BTN = registerItem(MACHINES, "gui_obsidian_btn", "menu.machines.cobblestone_generator.mode.obsidian")
    val PUMP_MODE_BTN = registerItem(MACHINES, "gui_pump_pump_btn", "menu.machines.pump.pump_mode")
    val PUMP_REPLACE_MODE_BTN = registerItem(MACHINES, "gui_pump_replace_btn", "menu.machines.pump.replace_mode")
    val ICE_MODE_BTN = registerItem(MACHINES, "gui_ice_btn", "menu.machines.freezer.mode.ice")
    val PACKED_ICE_MODE_BTN = registerItem(MACHINES, "gui_packed_ice_btn", "menu.machines.freezer.mode.packed_ice")
    val BLUE_ICE_MODE_BTN = registerItem(MACHINES, "gui_blue_ice_btn", "menu.machines.freezer.mode.blue_ice")
    val HOE_BTN_ON = registerItem(MACHINES, "gui_hoe_btn_on", "menu.machines.planter.autotill.on")
    val HOE_BTN_OFF = registerItem(MACHINES, "gui_hoe_btn_off", "menu.machines.planter.autotill.off")
    val FLUID_LEFT_RIGHT_BTN = registerItem(MACHINES, "gui_fluid_left_right_btn", "menu.machines.fluid_infuser.mode.insert")
    val FLUID_RIGHT_LEFT_BTN = registerItem(MACHINES, "gui_fluid_right_left_btn", "menu.machines.fluid_infuser.mode.extract")
    
    val TP_GREEN_PLUS = registerItem(MACHINES, "gui_green_plus")
    val TP_RED_MINUS = registerItem(MACHINES, "gui_red_minus")
    
    val AXE_PLACEHOLDER = registerItem(MACHINES, "gui_axe_placeholder")
    val HOE_PLACEHOLDER = registerItem(MACHINES, "gui_hoe_placeholder")
    val SHEARS_PLACEHOLDER = registerItem(MACHINES, "gui_shears_placeholder")
    val BOTTLE_PLACEHOLDER = registerItem(MACHINES, "gui_bottle_placeholder")
    val FISHING_ROD_PLACEHOLDER = registerItem(MACHINES, "gui_fishing_rod_placeholder")
    val MOB_CATCHER_PLACEHOLDER = registerItem(MACHINES, "gui_mob_catcher_placeholder")
    val SAPLING_PLACEHOLDER = registerItem(MACHINES, "gui_sapling_placeholder")
    
    val ARROW_PROGRESS = registerItem(MACHINES, "gui_arrow_progress")
    val ENERGY_PROGRESS = registerItem(MACHINES, "gui_energy_progress")
    val PULVERIZER_PROGRESS = registerItem(MACHINES, "gui_pulverizer_progress")
    val PRESS_PROGRESS = registerItem(MACHINES, "gui_press_progress")
    val TP_BREW_PROGRESS = registerItem(MACHINES, "gui_brew_progress")
    val FLUID_PROGRESS_LEFT_RIGHT = registerItem(MACHINES, "gui_fluid_progress_left_right")
    val FLUID_PROGRESS_RIGHT_LEFT = registerItem(MACHINES, "gui_fluid_progress_right_left")
    val TP_FLUID_PROGRESS_LEFT_RIGHT = registerItem(MACHINES, "gui_tp_fluid_progress_left_right")
    val TP_FLUID_PROGRESS_RIGHT_LEFT = registerItem(MACHINES, "gui_tp_fluid_progress_right_left")
    
}