# dyeutils

a client-side fabric mod for hypixel skyblock, built to take some of the busywork out of the dye grind.

## features

**party inviter**: keep a list of igns and invite all of them with one keypress.

hypixel refuses any invite command issued while five or more invites are already outstanding, so a
long list cannot go out at once. the mod sends four, then four more, then waits and watches party
chat, sending the next four as soon as enough invites resolve, either people joining or invites
lapsing after a minute. you get told who never joined at the end. pressing the hotkey again while it
is still working is ignored.

if the party ends while it is still working, the rest of the queue is forgotten rather than sent into
a party that is not there any more, or into the next one you put together. leaving, disbanding and
being kicked all count, including the disband the mod itself runs after bacte. this is always on.

**favourites**: a second, permanent list of the people you group with often. the party list gets
rewritten every time you put a different group together; favourites does not. star a name on the
party list to add them, and put any of them back on the party list with one button.

both screens have a search box, and both show each player's head next to their name.

**party warp**: a second hotkey that sends `!warp` in party chat.

**`!partydt`**: anyone in the party can type `!partydt` in party chat to take themselves off your
party list, for when they are logging off or done with bacte. they come off the list, and out of an
invite run that is going on at the time. you get a line telling you who asked. only party chat counts
and the message has to be nothing but the command, so it cannot be set off from public chat or in
passing. this is always on.

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

**vincent's boosted dyes**: every skyblock year vincent picks three dyes and boosts their drop rates,
two at 2x and one at 3x. his menu shows you which three straight away, so this hides them behind
question marks until you click one, and then plays a csgo-style case opening that scrolls dyes past and
lands on the one actually boosted in that slot. open his menu with `/dyes`.

the year is read off the dyes themselves, since their own lore says which year the boost applies to, so
nothing here needs a clock and the dyes hide themselves again when the rotation turns over. reveals are
remembered by dye name rather than by slot, so the two times hypixel has swapped a dye mid-year would
have re-hidden only the dye that changed.

the dyes on the reel are the real ones. every dye in skyblock is a player head, so the mod learns their
icons from any menu that has dyes in it, vincent's dye compendium being the quickest way, and remembers
them between sessions. until you have opened that once the reel is short.

nothing about this is sent to the server. the click on a question mark is swallowed by the mod, and the
real items are left untouched in the menu, so hypixel and any other mod still see exactly what vincent
sent. it can be switched off under vincent in the config.

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

**player heads are the only thing in this mod that touches the network,** and they are always on. two
things rest on them now: the names on your lists, and the dye icons on the reveal reel.

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

the dye icons are the same mechanism one step along. every skyblock dye is a player head, so the mod
keeps the texture blob hypixel already sent with the item and hands that to minecraft, which fetches the
image from mojang's own texture servers through the same cache it uses for any head in any inventory. no
name is looked up for this, no request of ours is made, and nothing about you is sent. a dye whose
texture will not load renders as the default head and the reveal still works.

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

## credits

the dye reveal animation is a port of the dungeon chest case opening from
[skyocean](https://github.com/meowdding/SkyOcean) by meowdding, used under its mit licence. the easing,
the timing and the card geometry are kept as they are there so it feels the same. the blur shader and the
post effect chain that drives it are written from scratch, because skyocean's chain is not under the mit
half of its licence.

## licence

mit.
