package com.windwoif.balance.content.reactors.reactorCore;

import com.windwoif.balance.content.reactors.reactorEntity.ReactorEntity;
import net.minecraft.world.level.Level;

import java.util.*;

public class ReactorConnectionManager {
    private static final Map<Level, ReactorConnectionManager> instances = new HashMap<>();
    private final Set<ReactorConnection> connections = new HashSet<>();
    private final Map<ReactorEntity, Set<ReactorConnection>> entityMap = new HashMap<>();

    public static ReactorConnectionManager get(Level level) {
        if (level.isClientSide) return null;
        return instances.computeIfAbsent(level, k -> new ReactorConnectionManager());
    }

    public void addConnection(ReactorEntity a, ReactorEntity b) {
        if (a == b || hasConnection(a, b)) return;
        ReactorConnection conn = new ReactorConnection(a, b);
        connections.add(conn);
        entityMap.computeIfAbsent(a, k -> new HashSet<>()).add(conn);
        entityMap.computeIfAbsent(b, k -> new HashSet<>()).add(conn);
    }

    public void removeConnection(ReactorEntity a, ReactorEntity b) {
        ReactorConnection conn = findConnection(a, b);
        if (conn != null) {
            conn.invalidate();
            connections.remove(conn);
            entityMap.getOrDefault(a, Collections.emptySet()).remove(conn);
            entityMap.getOrDefault(b, Collections.emptySet()).remove(conn);
        }
    }

    public void removeAllConnections(ReactorEntity entity) {
        Set<ReactorConnection> set = entityMap.getOrDefault(entity, Collections.emptySet());
        for (ReactorConnection conn : new ArrayList<>(set)) {
            ReactorEntity other = conn.getOther(entity);
            removeConnection(entity, other);
        }
        entityMap.remove(entity);
    }

    public boolean hasConnection(ReactorEntity a, ReactorEntity b) {
        return findConnection(a, b) != null;
    }

    private ReactorConnection findConnection(ReactorEntity a, ReactorEntity b) {
        Set<ReactorConnection> set = entityMap.get(a);
        if (set == null) return null;
        return set.stream().filter(c -> c.contains(b)).findFirst().orElse(null);
    }

    public Set<ReactorConnection> getConnections(ReactorEntity entity) {
        return Collections.unmodifiableSet(entityMap.getOrDefault(entity, Collections.emptySet()));
    }

    public Set<ReactorConnection> getAllConnections() {
        return Collections.unmodifiableSet(connections);
    }

    public void tick() {
        for (ReactorConnection conn : connections) {
            if (conn.isActive()) {
                conn.update();
            }
        }
        for (ReactorConnection conn : connections) {
            if (conn.isActive()) {
                conn.apply();
            }
        }
    }

    // Debug: print all connections to console (visible in IDE)
    public void debugPrintConnections() {
        System.out.println("=== Reactor Connections ===");
        for (ReactorConnection conn : connections) {
            System.out.println(conn);
        }
        System.out.println("============================");
    }
}