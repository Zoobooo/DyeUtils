# dyeutils

a client-side fabric mod for hypixel skyblock, built to take some of the busywork out of the dye grind.

## features

**party inviter**: keep a list of igns and invite all of them with one keypress. hypixel caps invites
at five per command, so longer lists are split into batches sent half a second apart.

**party warp**: a second hotkey that runs `/p warp`.

both hotkeys are unbound by default. set them in the mod's config screen or under
options, controls, key binds.

## usage

open the config with `/dyeutils`, or through mod menu.

the party list can also be edited from chat:

```
/dyeutils playerlist               show the current list
/dyeutils playerlist add <ign>
/dyeutils playerlist remove <ign>
```

under advanced you can change the command the names are appended to. it defaults to `/p invite `,
which is the form hypixel documents for inviting several people at once.

## requirements

- minecraft 26.1.2
- fabric loader 0.19.3 or newer
- fabric api
- fabric language kotlin

yacl and dandelion are bundled, so you do not need to install them separately. mod menu is optional.

## building

needs jdk 25.

```
./gradlew build
```

the jar lands in `build/libs`.

`libs/dandelion-1.0.0-alpha.21+26.1.jar` is vendored because that version was never published to a
maven repository. it is the first one with a key mapping option, which the hotkey rows use.

## notes

minecraft is unobfuscated from 1.21.11 onwards, so the build has no `mappings` line and mod
dependencies use the plain gradle configurations instead of `modImplementation`.

the config screen is built on dandelion with its moulconfig backend, which is what skyblocker uses.
the party list editor is a separate screen because dandelion's list options render every entry as a
full option card and offer no add or remove controls.

## licence

mit.
