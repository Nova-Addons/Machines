package xyz.xenondevs.nova.machines.gui

import xyz.xenondevs.nova.machines.registry.GUIMaterials
import xyz.xenondevs.nova.ui.item.ProgressItem

class EnergyProgressItem : ProgressItem(GUIMaterials.ENERGY_PROGRESS, 16)

class ProgressArrowItem : ProgressItem(GUIMaterials.ARROW_PROGRESS, 16)

class PressProgressItem : ProgressItem(GUIMaterials.PRESS_PROGRESS, 8)

class PulverizerProgressItem : ProgressItem(GUIMaterials.PULVERIZER_PROGRESS, 14)

class LeftRightFluidProgressItem : ProgressItem(GUIMaterials.FLUID_PROGRESS_LEFT_RIGHT, 16)

class RightLeftFluidProgressItem : ProgressItem(GUIMaterials.FLUID_PROGRESS_RIGHT_LEFT, 16)

class BrewProgressItem : ProgressItem(GUIMaterials.TP_BREW_PROGRESS, 16)