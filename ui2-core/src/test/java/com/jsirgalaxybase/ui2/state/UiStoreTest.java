package com.jsirgalaxybase.ui2.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class UiStoreTest {
    @Test public void reducerRunsEffectsThenPublishesOneStableState() {
        final StringBuilder order = new StringBuilder();
        UiReducer<Integer, Integer> reducer = new UiReducer<Integer, Integer>() {
            @Override public Integer reduce(Integer state, Integer action) { return state + action; }
        };
        UiEffect<Integer, Integer> effect = new UiEffect<Integer, Integer>() {
            @Override public void afterDispatch(Integer previous, Integer current, Integer action) {
                order.append("effect:").append(previous).append('>').append(current);
            }
        };
        UiStore<Integer, Integer> store = new UiStore<Integer, Integer>(1, reducer, Collections.singletonList(effect));
        store.addListener(new UiStore.Listener<Integer>() { @Override public void onStateChanged(Integer state) { order.append("listener:").append(state).append(';'); } });
        store.dispatch(2);
        assertEquals(Integer.valueOf(3), store.getState());
        assertEquals("effect:1>3listener:3;", order.toString());
    }

    @Test public void reentrantEffectActionsAreQueuedAndListenersSeeFinalStateOnce() {
        final UiStore<Integer, Integer>[] holder = new UiStore[1];
        final AtomicInteger notifications = new AtomicInteger();
        UiEffect<Integer, Integer> effect = new UiEffect<Integer, Integer>() {
            @Override public void afterDispatch(Integer previous, Integer current, Integer action) {
                if (current.intValue() < 3) holder[0].dispatch(Integer.valueOf(1));
            }
        };
        holder[0] = new UiStore<Integer, Integer>(Integer.valueOf(0),
            new UiReducer<Integer, Integer>() {
                @Override public Integer reduce(Integer state, Integer action) {
                    return Integer.valueOf(state.intValue() + action.intValue());
                }
            }, Collections.singletonList(effect));
        holder[0].addListener(new UiStore.Listener<Integer>() {
            @Override public void onStateChanged(Integer state) { notifications.incrementAndGet(); }
        });
        holder[0].dispatch(Integer.valueOf(1));
        assertEquals(Integer.valueOf(3), holder[0].getState());
        assertEquals(1, notifications.get());
    }

    @Test public void selectorCachesByStateIdentity() {
        final AtomicInteger calls = new AtomicInteger();
        UiSelector<Object, String> selector = new UiSelector<Object, String>() {
            @Override protected String compute(Object state) { calls.incrementAndGet(); return state.toString(); }
        };
        Object state = new Object();
        String first = selector.select(state);
        assertSame(first, selector.select(state));
        selector.select(new Object());
        assertEquals(2, calls.get());
    }

    @Test public void effectLoopFailsWithActionDiagnostic() {
        final UiStore<Integer, String>[] holder = new UiStore[1];
        UiEffect<Integer, String> loop = new UiEffect<Integer, String>() {
            @Override public void afterDispatch(Integer previous, Integer current, String action) {
                holder[0].dispatch("loop-" + current);
            }
        };
        holder[0] = new UiStore<Integer, String>(Integer.valueOf(0),
            new UiReducer<Integer, String>() {
                @Override public Integer reduce(Integer state, String action) {
                    return Integer.valueOf(state.intValue() + 1);
                }
            }, Collections.singletonList(loop));
        try {
            holder[0].dispatch("start");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("loop-"));
            return;
        }
        throw new AssertionError("effect loop should fail fast");
    }
}
