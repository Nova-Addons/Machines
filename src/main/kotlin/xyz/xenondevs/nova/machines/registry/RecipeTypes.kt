package xyz.xenondevs.nova.machines.registry

import xyz.xenondevs.nova.data.recipe.RecipeTypeRegistry.register
import xyz.xenondevs.nova.machines.MACHINES
import xyz.xenondevs.nova.machines.recipe.*
import xyz.xenondevs.nova.machines.recipe.group.FluidInfuserRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.PressingRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.PulverizingRecipeGroup
import xyz.xenondevs.nova.machines.recipe.group.hardcoded.*

object RecipeTypes {
    
    val PULVERIZER = register(MACHINES, "pulverizer", PulverizerRecipe::class, PulverizingRecipeGroup, PulverizerRecipeDeserializer)
    val GEAR_PRESS = register(MACHINES, "press/gear", GearPressRecipe::class, PressingRecipeGroup, GearPressRecipeDeserializer)
    val PLATE_PRESS = register(MACHINES, "press/plate", PlatePressRecipe::class, PressingRecipeGroup, PlatePressRecipeDeserializer)
    val FLUID_INFUSER = register(MACHINES, "fluid_infuser", FluidInfuserRecipe::class, FluidInfuserRecipeGroup, FluidInfuserRecipeDeserializer)
    val ELECTRIC_BREWING_STAND = register(MACHINES, "electric_brewing_stand", ElectricBrewingStandRecipe::class, null, ElectricBrewingStandRecipeDeserializer)
    val STAR_COLLECTOR = register(MACHINES, null, StarCollectorRecipe::class, StarCollectorRecipeGroup, null)
    val COBBLESTONE_GENERATOR = register(MACHINES, null, CobblestoneGeneratorRecipe::class, CobblestoneGeneratorRecipeGroup, null)
    val FREEZER = register(MACHINES, null, FreezerRecipe::class, FreezerRecipeGroup, null)
    
    fun init() = Unit
    
}