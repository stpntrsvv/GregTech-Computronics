# AI Context: GregTech Computronics

This file is written for future AI/chat sessions. Read it before making changes so the repository does not have to be rediscovered from scratch.

## Project

`GregTech: Computronics` is a GregTech CEu addon for Minecraft 1.20.1 on Forge.

Important Gradle properties:

- `mod_id=gtcomputronics`
- `mod_name=GregTech: Computronics`
- `minecraft_version=1.20.1`
- `forge_version=47.4.0`
- `gtceu_version=7.4.0`
- Java target: 17

The project was started from the GregTech Addon Template, but git history was reset. The active remote is:

```text
https://github.com/stpntrsvv/GregTech-Computronics.git
```

## Current Working Content

Implemented and tested in-game:

- Creative tab: `GregTech: Computronics`
- Item: `gtcomputronics:punch_card`
- English lang entry: `Punch Card`
- Temporary model: uses vanilla `minecraft:item/paper`
- Temporary recipe: shapeless `minecraft:paper` -> `gtcomputronics:punch_card`

The punch card appears in the mod creative tab and was confirmed by the user in-game.

## Main Files

- `src/main/java/com/gregtechcomputronics/ComputronicsMod.java`
  - Forge mod entrypoint via `@Mod(ComputronicsMod.MOD_ID)`.
  - Registers Forge/GTCEu event listeners.
  - Calls `CustomCreativeModeTabs.init()`, then `CustomItems.init()`, then `REGISTRATE.registerRegistrate()`.

- `src/main/java/com/gregtechcomputronics/ComputronicsGTAddon.java`
  - GTCEu addon entrypoint via `@GTAddon`.
  - Implements `IGTAddon`.
  - Use this for GTCEu addon hooks such as generated GT recipes, tag prefixes, elements, ore/fluid veins, recipe keys, etc.

- `src/main/java/com/gregtechcomputronics/data/CustomCreativeModeTabs.java`
  - Registers the main creative tab.
  - Uses `ComputronicsMod.REGISTRATE.defaultCreativeTab("main", ...)`.
  - Sets the current `GTRegistrate` creative tab so later registered items are added to it.

- `src/main/java/com/gregtechcomputronics/data/CustomItems.java`
  - Register normal items here.
  - Current item:

```java
public static final ItemEntry<Item> PUNCH_CARD = ComputronicsMod.REGISTRATE
        .item("punch_card", Item::new)
        .register();
```

- `src/main/resources/assets/gtcomputronics/lang/en_us.json`
  - Client-facing English names.

- `src/main/resources/assets/gtcomputronics/models/item/punch_card.json`
  - Item model for the punch card.

- `src/main/resources/data/gtcomputronics/recipes/punch_card.json`
  - Temporary vanilla crafting recipe.

- `src/main/resources/gtcomputronics.mixins.json`
  - Mixin config. Only contains a dummy mixin for now.

## Registration Pattern

For ordinary mod content, prefer small `data` classes and call their `init()` methods from `ComputronicsMod` before `REGISTRATE.registerRegistrate()`.

Current order:

```java
CustomCreativeModeTabs.init();
CustomItems.init();
REGISTRATE.registerRegistrate();
```

Keep the creative tab init before item init so new items are associated with the mod tab by `GTRegistrate`.

Use `ComputronicsMod.REGISTRATE` for Registrate content. Do not create another registrate instance unless there is a specific reason.

## GTCEu vs Forge Entry Points

`ComputronicsMod` is the Forge mod entrypoint. Use it for lifecycle wiring and event listener registration.

`ComputronicsGTAddon` is the GTCEu addon entrypoint. GTCEu discovers it through `@GTAddon` and calls `IGTAddon` hooks.

Useful `IGTAddon` hooks include:

- `initializeAddon()`
- `registerTagPrefixes()`
- `registerElements()`
- `addRecipes(Consumer<FinishedRecipe> provider)`
- `removeRecipes(Consumer<ResourceLocation> consumer)`
- `registerOreVeins()`
- `registerFluidVeins()`
- `registerBedrockOreVeins()`
- `collectMaterialCasings(...)`
- `registerRecipeKeys(...)`

For materials, this template currently uses events in `ComputronicsMod`:

- `MaterialRegistryEvent`
- `MaterialEvent`
- `PostMaterialEvent`

Prefer those over deprecated `IGTAddon.registerMaterials()`.

## Development Commands

The agent environment previously needed `JAVA_HOME` set manually. User-level environment was configured to:

```text
C:\Users\stepa\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2
```

If the current terminal still cannot find Java, use this one-command prefix:

```powershell
$env:JAVA_HOME=[Environment]::GetEnvironmentVariable('JAVA_HOME','User'); $env:PATH=([Environment]::GetEnvironmentVariable('Path','User') + ';' + [Environment]::GetEnvironmentVariable('Path','Machine'))
```

Build:

```powershell
.\gradlew.bat build
```

Compile only:

```powershell
.\gradlew.bat compileJava
```

Format:

```powershell
.\gradlew.bat spotlessApply
```

Run full pre-commit check:

```powershell
.\gradlew.bat spotlessApply build
```

## Current Known Tradeoffs

- The punch card texture is temporary and points to vanilla paper.
- The punch card recipe is temporary and intentionally simple.
- The package/class names are already changed to `com.gregtechcomputronics`.
- The dummy mixin is still present from the template. Do not add real mixins unless an API/event solution is not enough.
- The normal README is short and user-facing. Keep this file as the detailed AI handoff.

## Git Workflow

The repository has a clean root commit:

```text
Initialize GregTech Computronics addon
```

Before pushing future changes:

```powershell
git status --short
.\gradlew.bat build
git add .
git commit -m "<message>"
git push
```
