require "fixmodule"
require "demod.fbsr.FBSRBridge"

local inspect = require "inspect"
local BlueprintString = require "blueprintstring"

require "dataloader"
require "core.data"
require "base.data"

local blueprint_table = BlueprintString.fromString(fbsr.decoded())
--print(inspect(blueprint_table))
fbsr.result(blueprint_table)