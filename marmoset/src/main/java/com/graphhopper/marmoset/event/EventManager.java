package com.graphhopper.marmoset.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by alexander on 22/04/2016.
 */
public class EventManager {

    @FunctionalInterface
    public interface EventHandler {
        void handle(String name, Object ...args);

    }

    private static HashMap<String, List<EventHandler>> handlers = new HashMap<>();

    public static void listenTo(String name, EventHandler handler)
    {
        synchronized (handlers)
        {
            if (!handlers.containsKey(name))
            {
                handlers.put(name, new ArrayList<>());
            }

            handlers.get(name).add(handler);
        }
    }

    public static synchronized void trigger(String name, Object ...args)
    {
        if (!handlers.containsKey(name))
            return;

        handlers.get(name).forEach(h -> h.handle(name, args));
    }
}
