if not module then
    function module(modname,...)
        local function findtable(tbl,fname)
            for key in string.gmatch(fname,"([%w_]+)") do
                if key and key~="" then
                    local val = rawget(tbl,key)
                    if not val then
                        local field = {}
                        tbl[key]=field
                        tbl = field
                    elseif type(val)~="table" then
                        return nil
                    else
                        tbl = val
                    end
                end
            end
            return tbl
        end

        assert(type(modname)=="string")
        local value,modul = package.loaded[modname]
        if type(value)~="table" then
            modul = findtable(_G,modname)
            assert(modul,"name conflict for module '"..modname.."'" )
            package.loaded[modname] = modul
        else
            modul = value
        end

        local name = modul._NAME
        if not name then
            modul._M = modul
            modul._NAME = modname
            modul._PACKAGE = string.match(modname,"([%w%._]*)%.[%w_]*$")
        end
        local func = debug.getinfo(2,"f").func
        debug.setupvalue(func,1,modul)
        for _,f in ipairs{...} do f(modul) end
    end

    function package.seeall(modul)
        setmetatable(modul,{__index=_G})
    end
end