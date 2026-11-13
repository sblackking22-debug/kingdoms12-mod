# Kingdoms (Fabric, Minecraft 26.3) - Stage 1

Villager and pillager kingdoms with kings, queens, armies, overthrow and a royal court.
Singleplayer. Requires Java 25.

## Build

```
gradle wrapper --gradle-version 9.6.0     # once, creates ./gradlew
./gradlew build                            # jar in build/libs/
./gradlew runClient                        # test in a dev client
```

Versions are in `gradle.properties` (Loom 1.17, Loader 0.19.5, Fabric API 0.159.0+26.3).
Check https://fabricmc.net/develop/ if any of them fail to resolve.

## How to play (stage 1)

1. `/kingdom found villager` and `/kingdom found pillager` in two different places.
   Each makes one king, one queen and a small army wearing enchanted diamond armor.
2. `/kingdom pledge villager|pillager` to pick your faction (once).
3. Beat that faction's king down to one heart. He surrenders and becomes invulnerable.
   Right-click him: you take the throne, get +10 hearts (20 total), and his queens become yours.
   The deposed king can now be killed by hand.
4. Killing a queen by hand (not yours) gives you a free throne (max 5 queens).
   Right-click a female soldier holding a gold ingot to crown her.
5. Right-click one of your queens for a clickable menu: Toast, Dine, Kiss, Sleep, Execute.
   Favoring one queen makes the others jealous; very jealous queens lose loyalty.
6. `/kingdom rank <recruit|swordsman|axeman|archer>` promotes or demotes the nearest soldier.
7. `/kingdom status` shows your state.

While you do not rule a faction, both sides slowly reinforce and march on each other's king.

## Textures

`src/main/resources/assets/kingdoms/textures/entity/*.png` are 64x64 solid-color placeholders.
Replace them with real player-layout skins made in Blockbench (same file names).
Queens: cream long hair and gown. Pillagers: gray skin.

## Not in stage 1 yet

Generated castles/outposts/mansions, land capture and banners, horsemen, extra golems,
the ravager-like beast, the evoker-like caster, the throne block and helmet-over-throne,
natural female villager spawns, and the villager-side caster. Those are stages 2 and 3.

## Honest note

Written without being able to compile (no network in the build environment) against the
Mojang-named API of 26.x from memory. Expect a few small compile errors from renamed classes;
send me the error output and I will fix them.
