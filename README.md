# dyeutils

a client-side fabric mod for hypixel skyblock, built to take some of the busywork out of the dye grind.

## features

**party inviter**: keep a list of igns and invite all of them with one keypress.

hypixel refuses any invite command issued while five or more invites are already outstanding, so a
long list cannot go out at once. the mod sends four, then four more, then waits and watches party
chat, sending the next four as soon as enough invites resolve, either people joining or invites
lapsing after a minute. you get told who never joined at the end. pressing the hotkey again while it
is still working is ignored.

**favourites**: a second, permanent list of the people you group with often. the party list gets
rewritten every time you put a different group together; favourites does not. star a name on the
party list to add them, and put any of them back on the party list with one button.

both screens have a search box, and both show each player's head next to their name.

**party warp**: a second hotkey that sends `!warp` in party chat.

**bacte as a dye**: bacte, the rift colosseum boss, is rendered as the celadon dye he drops instead
of a slime. he is recognised from his nametag through all five phases, from `B` up to `Bacte`, so it
holds for the whole fight. globowls and every other slime are left alone.

his nametag floats free of him, so the mod has to work out which slime it belongs to. it takes the
slime standing directly under the tag, comparing only x and z: bacte pulses between size 1 and 17
during the fight, so his feet sit anywhere from half a block to eight below his own nametag, and any
measurement that includes height picks one of the growths sprouting beside him instead. checked
against a recorded fight, this is right 627 times out of 630, wrong none, and picks nothing at all
three times.

**disband after bacte**: runs `/p disband` when hypixel announces the kill, so the next group can be
invited straight away. only hypixel's own announcement counts, not someone typing the words in chat.
it can be switched off in the config.

both hotkeys are unbound by default. set them in the mod's config screen or under
options, controls, key binds.

## usage

open the config with `/dyeutils`, or through mod menu.

the party list can also be edited from chat:

```
/dyeutils playerlist               show the current list
/dyeutils playerlist add <ign>
/dyeutils playerlist remove <ign>
/dyeutils playerlist clear         empty the list

/dyeutils favourites               show your favourites
/dyeutils favourites add <ign>
/dyeutils favourites remove <ign>
/dyeutils favourites clear
```

under advanced you can change the command the names are appended to. it defaults to `/p invite `,
which is the form hypixel documents for inviting several people at once.

## the network

**player heads are the only thing in this mod that touches the network,** and they are always on.

the usernames on your party list and favourites are sent to
[mowojang.matdoes.dev](https://mowojang.matdoes.dev), a public read-only mirror of mojang's profile
api, to find out whose skin is whose. it is used rather than mojang directly because mojang rate
limits profile lookups to about one per player per minute, which a screen full of names hits
immediately. firmament does the same thing for the same reason.

each name is looked up once per session, only while one of those screens is open, and the whole list
goes in a single request. the skin image itself is downloaded by minecraft from mojang's own servers,
through the same cache it uses for anyone you stand next to. this mod does not handle it. a name
that cannot be looked up keeps the default skin and is not asked about again. if the lookups fail
repeatedly they stop for the session.

nothing about you is sent. no account, no session, no login, no id of yours, only the usernames you
typed in yourself.

**this is not the auto updater coming back.** the mod does not download code, and does not replace its
own jar. new versions are still installed by hand.

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
git tag v0.1.2
git push origin v0.1.2
```

`.github/workflows/release.yml` builds the jar and opens a **draft** release with it attached. write
the notes on the draft and publish when you are ready.

the workflow fails the build rather than publishing a bad release if the tag does not match
`mod_version`, if the jar is not named `dyeutils-<mod version>+<minecraft version>.jar`, or if the
version inside the jar disagrees with its file name, so a release can never claim to be a version
it is not.

the mod does not update itself. new versions are downloaded and installed by hand.

## notes

minecraft is unobfuscated from 1.21.11 onwards, so the build has no `mappings` line and mod
dependencies use the plain gradle configurations instead of `modImplementation`.

the config screen is built on dandelion with its moulconfig backend, which is what skyblocker uses.
the party list editor is a separate screen because dandelion's list options render every entry as a
full option card and offer no add or remove controls.

## licence

mit.
