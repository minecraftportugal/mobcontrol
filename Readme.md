
MobControl Plugin
=================

Plugin that profiles mob spawning and identifies potential chunks that may have mob farms.
After identifying a problematic chunk, this chunk can be configured to have a limited spawn rate amount, or disable any spawning at all.

This plugin does not prevent mobs from entering a certain chunk, only prevents it from spawning mobs.


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

* **debug**: activate verbosity.
* **reportSize**: Maximum amount of top chunks to show on report.
* **mostCommonMobsSize**: Maximum size of the top most spawned mobs list on each chunk to show on the report.

### Rules Configuration

Rules are applied on a world basis, from the bottom of the list to top, until one is matched. If none is matched, the *default* rule is applied.

The *default* rule should always have a rate of **1** because the minecraft server will always try to have a stable amount of mobs on a certain radius of the player, decreasing it will only make the server overstress the mob spawning algorithm. For the same reason, you should not disable mob spawning at all on a big blob of chunks.

All rules must have a different name.

In the **worlds** sections of the configuration, you need to list the name of the worlds

* **active**: a rule can be enable/disable by setting this to **true** or **false**, omitting is the same as setting to true.
* **rate**: A value between *0.0* and *1.0*, being the probability of a mob spawn event to succeed.
  
  A value of *0.0* will disable all mob spawns matched by the rule.

* **include**: A list of mob names to be matched against this rule.

  Leaving it empty, or using the **ALL** keyword, makes this rule match this rule against all mobs.

  Mob names are found here http://jd.bukkit.org/dev/apidocs/org/bukkit/entity/EntityType.html. Only *living* entities will be processed by this plugin.

  
* **exclude**: A list of mob names to exclude from this rule.
* **chunks**: A list of chunk positions to match this rule.

  Leaving it empty makes it accept all chunks.
