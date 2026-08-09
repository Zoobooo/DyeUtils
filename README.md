# dyeutils

a client-side fabric mod for hypixel skyblock, built to take some of the busywork out of the dye grind.

## features

**party inviter**: keep a list of igns and invite all of them with one keypress. hypixel caps invites
at five per command, so longer lists are split into batches sent half a second apart.

**party warp**: a second hotkey that runs `/p warp`.

both hotkeys are unbound by default. set them in the mod's config screen or under
options, controls, key binds.

**auto update**, the mod keeps itself up to date from github releases. it checks in the background
while you play, downloads anything newer that was built for your minecraft version, and swaps the jar
in when you close the game, so the new version is running the next time you start. there is nothing
to click and no message in chat.

on windows the jar cannot be renamed while the game holds it open, so the update is written into the
existing file and the file name keeps the version it was first downloaded as. the version mod menu
reports is the real one.

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

## releasing

bump `mod_version` in `gradle.properties`, commit, then tag and push:

```
git tag v0.2.0
git push origin v0.2.0
```

`.github/workflows/release.yml` builds the jar and opens a **draft** release with it attached. write
the notes on the draft and publish when you are ready. the updater ignores drafts, so nothing reaches
players until you press publish.

the workflow fails the build rather than publishing a bad release if the tag does not match
`mod_version`, if the jar is not named `dyeutils-<mod version>+<minecraft version>.jar`, or if the
version inside the jar disagrees with its file name. that name is what the updater matches on, so a
renamed asset silently stops every installed copy from updating.

## notes

minecraft is unobfuscated from 1.21.11 onwards, so the build has no `mappings` line and mod
dependencies use the plain gradle configurations instead of `modImplementation`.

the config screen is built on dandelion with its moulconfig backend, which is what skyblocker uses.
the party list editor is a separate screen because dandelion's list options render every entry as a
full option card and offer no add or remove controls.

## licence

mit.
