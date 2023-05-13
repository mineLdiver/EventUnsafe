package net.mine_diver.unsafeevents.test;

import net.mine_diver.unsafeevents.listener.EventListener;

public class TestStaticMethods {

    @EventListener
    private static void listenForTest(TestEvent event) {
        event.stream.println("Static methods successful");
    }
}
