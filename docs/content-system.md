Basically stop stuffing everything into giant packet listeners and give content its own home.

Right now:

* A ton of content lives inside massive packet handlers
* Stuff is split by click1/click2/etc instead of by *what it actually is*
* It’s hard to find things
* It’s easy to break unrelated stuff when touching listeners



> One content file = one place that “owns” that thing.

Listeners should stay io/networking level:

* Decode packet bytes
* Setup walk-to / distance checks
* Do basic anti-spam / sanity checks
* Call into Kotlin dispatcher
* If nothing matches, fall back to legacy code for now until this is complete

No gameplay logic here long-term.


## Objects

Each object content file “owns” something:

* Either one object
* Or a group (by skill / area / function)

Each file:

* Lists the IDs it owns
* Handles all options (1–5)
* Later: item-on-object, magic-on-object, etc

Example:
objects/mining/CoalRocks.kt

Inside:
* click1 = mine
* click2 = prospect
* later: click3/4/5 if needed(probably not for coal)

Everything about coal rocks lives here.

## NPCs

Same idea as objects.

One file per NPC type (or group):

* Handles click1–5
* Attack
* Later: item-on-npc
Examples:

* Guards
* Farmers
* Shopkeepers

Each owns its own behavior.

## Interfaces / Buttons

Simple stuff:

* “1 button = 1 action” is fine (like teleports)

Sometimes button IDs mean different things depending on interface?


> The *interface* should own its buttons.

Long term goal:

* Interface knows when it’s open
* Can block button presses if player isn’t actually on it
* Fail-safe against weird packets


### Folders?

Use whatever makes sense:

**Skill-based**

objects/mining
npcs/thieving
objects/agility


**Function-based**

objects/doors
objects/teleports
npcs/shops


**Area-based (when helpful)**

objects/yanille
objects/edgeville

One file per unit

One file = one logical thing.

But a thing can be multiple IDs if they behave the same?


## Dispatcher Pattern 
This is the pattern we want everywhere:

1. Listener decodes packet + handles walking
2. When in range, calls dispatcher
3. Dispatcher finds content by ID
4. Runs matching handler
5. If it returns false → fall back to legacy

Eventually: no legacy left :)


## Interface State / “Busy” System

This is about preventing dupes and weird states.

### Idea

Give player a simple state, like:


activeInterfaceId

Then:

* Set when interface opens
* Clear when it closes
* Button handlers check it

If mismatch:

* Close interface
* Ignore action
* Maybe log it

Fail safe by default.

### Notes

Not everything needs this:

* Tabs (prayer, spellbook, etc)
* Some overlays (chatbox, dialogue)

This is mostly for modal interfaces.


### Dialogue System

Goal: Kotlin DSL for dialogues.

Features:

* NPC lines
* Player lines
* Options
* Auto pagination for long text

Example:

dialogue {
  npcChat("big long text...")
  options {
    "Choice 1" {
      playerChat("...")
      npcChat("...")
    }
    "Choice 2" {
      npcChat("...")
    }
    "Close" {
      close()
    }
  }
}



### Timers / Events

* Central helper for scheduled actions
* Easy cancel
* No custom loops everywhere
* No event leaks

Something like EventTimer that wraps scheduling properly.

End goal:

* Listeners = networking only
* Content = isolated, readable, testable
* No more “giant handler” files
* Easy to extend without breaking stuff
* Clear ownership of systems
