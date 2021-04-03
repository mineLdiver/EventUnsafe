package net.mine_diver.unsafeevents.listener;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface EventListener {

    int DEFAULT_PRIORITY = 0;

    ListenerPriority priority() default ListenerPriority.CUSTOM;

    int numPriority() default DEFAULT_PRIORITY;
}
