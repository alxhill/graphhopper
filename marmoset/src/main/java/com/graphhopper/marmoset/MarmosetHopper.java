package com.graphhopper.marmoset;

import com.graphhopper.GraphHopper;
import com.graphhopper.util.CmdArgs;

import java.io.IOException;

/**
 * Created by alexander on 15/02/2016.
 */
public class MarmosetHopper {
    GraphHopper hopper;

    public MarmosetHopper() {
        hopper = new GraphHopper();
    }

    public void init() {
        CmdArgs args;
        try
        {
            args = CmdArgs.readFromConfig("config.properties", "graphhopper.config");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return;
        }

        args.put("osmreader.osm", "british-isles-latest.osm.pbf");
        hopper.init(args);
    }
}
