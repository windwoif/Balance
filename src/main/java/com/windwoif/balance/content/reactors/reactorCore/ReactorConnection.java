package com.windwoif.balance.content.reactors.reactorCore;

import com.windwoif.balance.content.reactors.reactorEntity.ReactorEntity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ReactorConnection {
    private final ReactorEntity lowerEntity;
    private final ReactorEntity upperEntity;
    private final Reactor lower;
    private final Reactor upper;
    private final ConnectionType type;
    private int connectionOffset;
    private int contactWidth;
    private int contactHeight;
    private boolean active = true;
    private static final double TIME_STEP = 10; // 秒，与实际游戏 tick 匹配
    private List<Container.PhaseInterface> interfaces;

    // 平滑相关字段
    private final Map<Phase, Double> rawDemand = new HashMap<>();
    private final Map<Phase, Double> lastSmoothedDemand = new HashMap<>();
    private static final double FLOW_SMOOTHING = 0.2; // 新值权重，越小惯性越大

    public enum ConnectionType {
        VERTICAL,
        HORIZONTAL,
        NEIGHBOR
    }

    public ReactorConnection(ReactorEntity a, ReactorEntity b) {
        if (a.getBoundingBox().minY <= b.getBoundingBox().minY) {
            this.lowerEntity = a;
            this.upperEntity = b;
        } else {
            this.lowerEntity = b;
            this.upperEntity = a;
        }
        lower = this.lowerEntity.getReactor();
        upper = this.upperEntity.getReactor();
        this.type = determineConnectionType(a, b);
    }

    public void update() {
        if (!active) return;
        if (type == ConnectionType.VERTICAL) {
            updateInterfaces();
            calculateFlow();
        }
    }

    public void apply() {
        if (!active) return;
        exchangeChemical(upper, lower);
        exchangeChemical(lower, upper);
    }

    private void exchangeChemical(Reactor a, Reactor b) {
        a.getSortedPhases().stream().flatMap(phase -> phase.getFlowAmount(this).entrySet().stream()).forEach(entry -> {
            float temperature = a.getTemperature();
            b.changeChemical(entry.getKey(), a.tryChangeChemical(entry.getKey(), -entry.getValue(), temperature) , temperature);
        });
    }
    private void updateInterfaces() {
        var interfacesLower = lower
                .getPhaseInterfaces(connectionOffset, contactHeight + connectionOffset)
                .stream().map(a -> {
                    double height = a.height() - connectionOffset;
                    Container.PressureInfo pressureInfo = upper.getPressureInfo(height);
                    double pressure = pressureInfo.pressure() - a.pressure();
                    if (pressure > 0.0) {
                        Phase phase = pressureInfo.phase();
                        return new Container.PhaseInterface(pressure, phase, phase, height);
                    }
                    return new Container.PhaseInterface(pressure, a.phaseBelow(), a.phaseAbove(), height);
                });
        var interfacesUpper = upper.getPhaseInterfaces(0, contactHeight)
                .stream().map(a -> {
                    double height = a.height();
                    Container.PressureInfo pressureInfo = lower.getPressureInfo(height + connectionOffset);
                    double pressure = a.pressure() - pressureInfo.pressure();
                    if (pressure > 0.0) {
                        return new Container.PhaseInterface(pressure, a.phaseBelow(), a.phaseAbove(), height);
                    }
                    Phase phase = pressureInfo.phase();
                    return new Container.PhaseInterface(pressure, phase, phase, height);
                });
        interfaces = Stream.concat(interfacesLower, interfacesUpper)
                .sorted(Comparator.comparingDouble(Container.PhaseInterface::height))
                .toList();
    }

    private void calculateFlow() {
        rawDemand.clear();
        int n = interfaces.size();
        if (n < 2) return;

        Container.PhaseInterface lowerIface = interfaces.get(0);
        for (int i = 0; i < n - 1; i++) {
            Container.PhaseInterface upperIface = interfaces.get(i + 1);
            double h1 = lowerIface.height();
            double h2 = upperIface.height();
            if (Math.abs(h1 - h2) < 1e-12) continue;

            double p1 = lowerIface.pressure();
            double p2 = upperIface.pressure();
            Phase phase1 = lowerIface.phaseAbove();
            Phase phase2 = upperIface.phaseBelow();
            double density = (phase1.getDensity() + phase2.getDensity()) / 2.0;
            if (density <= 0) density = 1.0;

            if (phase1 != phase2) {
                double dp = p1 - p2;
                if (Math.abs(dp) < 1e-12) continue;
                double zero = h1 - p1 * (h1 - h2) / dp;
                boolean hasZero = (zero > Math.min(h1, h2) && zero < Math.max(h1, h2));
                if (hasZero) {
                    double v1 = pressureIntegralA(Math.abs(h1 - zero), p1, density);
                    double v2 = pressureIntegralA(Math.abs(h2 - zero), p2, density);
                    double flow1 = (p1 > 0) ? v1 : -v1;
                    double flow2 = (p2 > 0) ? v2 : -v2;
                    rawDemand.merge(phase1, flow1, Double::sum);
                    rawDemand.merge(phase2, flow2, Double::sum);
                } else {
                    double v = pressureIntegralB(Math.abs(h2 - h1), p1, p2, density);
                    double flow = (p1 > p2) ? v : -v;
                    rawDemand.merge(phase1, flow, Double::sum);
                    rawDemand.merge(phase2, -flow, Double::sum);
                }
            } else {
                double v = pressureIntegralB(Math.abs(h2 - h1), p1, p2, density);
                double flow = (p1 > p2) ? v : -v;
                rawDemand.merge(phase1, flow, Double::sum);
            }
            lowerIface = upperIface;
        }

        // 应用平滑并提交需求
        for (Map.Entry<Phase, Double> entry : rawDemand.entrySet()) {
            Phase phase = entry.getKey();
            double raw = entry.getValue();
            double last = lastSmoothedDemand.getOrDefault(phase, 0.0);
            if (last * raw < 0) {
                last = 0;
            }
            double smoothed = FLOW_SMOOTHING * raw + (1 - FLOW_SMOOTHING) * last;
            lastSmoothedDemand.put(phase, smoothed);
            if (Math.abs(smoothed) > 1e-12) {
                phase.demand(this, Math.abs(smoothed));
            }
        }
    }

    private double pressureIntegralA(double height, double pressure, double density) {
        if (height <= 0 || density <= 0) return 0.0;
        double absPressure = Math.abs(pressure);
        return TIME_STEP * 2.0/3.0 * height * contactWidth * Math.sqrt(2 * absPressure / density);
    }

    private double pressureIntegralB(double height, double pressure1, double pressure2, double density) {
        if (height <= 0 || density <= 0) return 0.0;
        double p1Abs = Math.abs(pressure1);
        double p2Abs = Math.abs(pressure2);
        double sp1 = Math.sqrt(p1Abs);
        double sp2 = Math.sqrt(p2Abs);
        return TIME_STEP * 2.0/3.0 * Math.abs(height) * contactWidth * Math.sqrt(2 / density)
                * (p1Abs + p2Abs + sp1 * sp2) / (sp1 + sp2);
    }

    private static class IntAABB {
        public final int minX, maxX, minY, maxY, minZ, maxZ;
        public IntAABB(AABB aabb) {
            this.minX = (int) Math.round(aabb.minX);
            this.maxX = (int) Math.round(aabb.maxX);
            this.minY = (int) Math.round(aabb.minY);
            this.maxY = (int) Math.round(aabb.maxY);
            this.minZ = (int) Math.round(aabb.minZ);
            this.maxZ = (int) Math.round(aabb.maxZ);
        }
        public int overlapX(IntAABB other) { return Math.min(maxX, other.maxX) - Math.max(minX, other.minX); }
        public int overlapY(IntAABB other) { return Math.min(maxY, other.maxY) - Math.max(minY, other.minY); }
        public int overlapZ(IntAABB other) { return Math.min(maxZ, other.maxZ) - Math.max(minZ, other.minZ); }
        public boolean touchX(IntAABB other) { return maxX == other.minX || other.maxX == minX; }
        public boolean touchY(IntAABB other) { return maxY == other.minY || other.maxY == minY; }
        public boolean touchZ(IntAABB other) { return maxZ == other.minZ || other.maxZ == minZ; }
    }

    private ConnectionType determineConnectionType(ReactorEntity a, ReactorEntity b) {
        IntAABB boxA = new IntAABB(a.getBoundingBox());
        IntAABB boxB = new IntAABB(b.getBoundingBox());

        // 垂直面相邻 (VERTICAL)
        if (boxA.overlapY(boxB) > 0) {
            if (determineVertical(boxA, boxB, boxA.touchX(boxB), boxA.overlapZ(boxB))) return ConnectionType.VERTICAL;
            if (determineVertical(boxA, boxB, boxA.touchZ(boxB), boxA.overlapX(boxB))) return ConnectionType.VERTICAL;
        }
        // 水平面相邻 (HORIZONTAL)
        if (boxA.overlapX(boxB) > 0 && boxA.overlapZ(boxB) > 0) {
            if (boxA.touchY(boxB)) {
                int width = boxA.overlapX(boxB);
                int height = boxA.overlapZ(boxB);
                if (width > 0 && height > 0) {
                    this.contactWidth = width;
                    this.contactHeight = height;
                    return ConnectionType.HORIZONTAL;
                }
            }
        }
        return ConnectionType.NEIGHBOR;
    }

    private boolean determineVertical(IntAABB boxA, IntAABB boxB, boolean adjacent, int width) {
        if (adjacent) {
            int height = boxA.overlapY(boxB);
            if (width > 0 && height > 0) {
                this.contactWidth = width;
                this.contactHeight = height;
                int lowerBottom = (int) Math.round(lowerEntity.getBoundingBox().minY);
                int contactMinY = Math.max(boxA.minY, boxB.minY);
                this.connectionOffset = contactMinY - lowerBottom;
                return true;
            }
        }
        return false;
    }

    public ReactorEntity getOther(ReactorEntity self) {
        if (self == lowerEntity) return upperEntity;
        if (self == upperEntity) return lowerEntity;
        return null;
    }

    public boolean contains(ReactorEntity entity) {
        return entity == lowerEntity || entity == upperEntity;
    }

    public void invalidate() { active = false; }
    public boolean isActive() { return active; }

    public Reactor getLowerReactor() { return lower; }
    public Reactor getUpperReactor() { return upper; }
    public double getContactWidth() { return contactWidth; }
    public double getContactHeight() { return contactHeight; }
    public ConnectionType getType() { return type; }
}