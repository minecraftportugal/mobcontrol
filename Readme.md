
MobControl Plugin
=================

Plugin that profiles mob spawning and identifies potential chunks that may have mob farms.
After identifying a problematic chunk, this chunk can be configured to have a limited spawn rate amount, or disable any spawning at all.

This plugin does not prevent mobs from entering a certain chunk, only prevents them from being spawn there.


Configuration
-------------

````
debug: false
reportSize: 10
mostCommonMobsSize: 3

worlds:

  default:
    rate: 1 
    include:
    exclude:
      
  world_name:
    base:
      active: true
      rate: 1.0
      include:
      - ALL
      exclude:
      - PIG
      - BAT
      chunks:
      - {x: 0, z: 30}
      - {x: 0, z: 0}
      
  amazing_world:
    base:
      rate: 1 
      include:
      exclude:
      
    rule2:
      rate: 0.5 
      include:
      - PIG
      exclude:
````



### Global Section


* **`debug`**: enable/disable verbosity.

* **`reportSize`**: Maximum amount of top chunks to show on report.

* **`mostCommonMobsSize`**: Maximum list size of the top most spawned mobs on each chunk, shown on the report.



### Rules Configuration

Rules are applied on a world basis, from the bottom to the top of the list, until one is matched. If none is matched, the `default` rule is applied.

The `default` rule should have a rate of `1` because the minecraft server will always try to have a stable amount of mobs on a certain radius of the player, decreasing it will only make the server overstress the mob spawning algorithm. For the same reason, you should not disable mob spawning at all on a big blob of chunks.

Rules on each world must have a different name.

In the `worlds` sections of the configuration, you need to list the name of the worlds.

* **`active`**: a rule can be enabled or disabled by setting this to `true` or `false`, omitting it is the same as setting to true.

* **`rate`**: A value between `0.0` and `1.0`, being the probability of a natural mob spawn event to succeed.
  
  A value of *0.0* will disable all mob spawns matched by the rule.

* **`include`**: A list of mob names to be matched against the rule.

  Leaving it empty, or using the `ALL` keyword, matches the rule against all mobs.

  Mob names are found here http://jd.bukkit.org/dev/apidocs/org/bukkit/entity/EntityType.html. Only *living* entities will be processed by this plugin.

  
* **`exclude`**: A list of mob names to be excluded.

* **`chunks`**: The list of chunks this rule applies to.

  Leaving this empty accept all chunks.

-----------

Permissions
-----------

Users must have the permission `mobcontrol.profile` to use the profiling commands.

-----------


Profiling
---------

Profiling allows you to identify chunks with abnormal amounts of mob spawn that might be problematic for the server.

* **`/mobprofiler start`**: starts the profiler. All posterior mob spawn events will be registered by the profiler.

* **`/mobprofiler stop`**: Stops the profiler from registering new mob spawn events.

* **`/mobprofiler clean`**: Cleans all profiler data. Should always be called when you're done with profiling to clean the unnecessary gathered data.

* **`/mobprofiler reset`**: Cleans and restarts the profiler.

* **`/mobprofiler chunk`**: Shows the chunk number where the user who uses this command is. Cannot be used from the console.

* **`/mobprofiler report [world]`**: Builds and shows a report with all the data the profiler has gathered. If used by a player inside the game and no world is passed as second argument, it will show a report for the user's current world. When used in the console, a world name must always be passed.
