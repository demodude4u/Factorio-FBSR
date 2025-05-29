package com.demod.fbsr.app;

import org.json.JSONObject;

import com.demod.factorio.Config;
import com.google.common.util.concurrent.AbstractIdleService;

public class RPCService extends AbstractIdleService {

    @Override
    protected void startUp() throws Exception {
        JSONObject config = Config.get().optJSONObject("rpc", new JSONObject());

        int port = config.optInt("port", 50832);

        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startUp'");
    }

    @Override
    protected void shutDown() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'shutDown'");
    }

}
