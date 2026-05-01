package com.windwoif.balance.content.reactors.reactorCore;

import com.windwoif.balance.content.reactors.reactorEntity.ReactorEntity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReactorConnection {
    private final ReactorEntity lowerEntity;
    private final ReactorEntity upperEntity;
    private final Reactor lower;
    private final Reactor upper;
    private final ConnectionType type;
    private int offsetLower;
    private int offsetUpper;
    private int contactWidth;
    private int contactHeight;
    private boolean active = true;
    private boolean mechanical = false;
    private static final double STEP_LENGTH = 0.05;
    private static final double SPREAD_RATE = 0.05;
    private List<Container.PhaseInterface> interfaces;

    public void setActive(boolean active) {
        this.active = active;
    }

    public ReactorEntity getLowerEntity() {
        return lowerEntity;
    }

    public ReactorEntity getUpperEntity() {
        return upperEntity;
    }

    public boolean isMechanical() {
        return mechanical;
    }

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
        else if (type == ConnectionType.HORIZONTAL) {
            calculateHorizontalFlow();
        }
    }

    public ReactorConnection(ReactorEntity a, ReactorEntity b, ConnectionType type,
                             int contactWidth, int contactHeight,
                             double globalMinY, double globalMaxY) {
        // 按高度排序
        if (a.getBoundingBox().minY <= b.getBoundingBox().minY) {
            this.lowerEntity = a;
            this.upperEntity = b;
        } else {
            this.lowerEntity = b;
            this.upperEntity = a;
        }
        this.lower = this.lowerEntity.getReactor();
        this.upper = this.upperEntity.getReactor();
        this.type = type;
        this.contactWidth = contactWidth;
        this.contactHeight = contactHeight;

        double lowerBottom = lowerEntity.getBoundingBox().minY;
        double upperBottom = upperEntity.getBoundingBox().minY;
        this.offsetLower = (int) (globalMinY - lowerBottom);
        this.offsetUpper = (int) (globalMinY - upperBottom);
        this.active = true;
        this.mechanical = true;
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
        // 转换 lower 容器界面
        Stream<Container.PhaseInterface> lowerStream = lower
                .getPhaseInterfaces(offsetLower, offsetLower + contactHeight)
                .stream()
                .map(a -> {
                    double height = a.height() - offsetLower;
                    Container.PressureInfo pressureInfo = upper.getPressureInfo(height + offsetUpper);
                    double pressure = pressureInfo.pressure() - a.pressure();
                    if (pressure > 0.0) {
                        Phase phase = pressureInfo.phase();
                        return new Container.PhaseInterface(pressure, phase, phase, height);
                    }
                    return new Container.PhaseInterface(pressure, a.phaseBelow(), a.phaseAbove(), height);
                });

        Stream<Container.PhaseInterface> upperStream = upper
                .getPhaseInterfaces(offsetUpper, offsetUpper + contactHeight)
                .stream()
                .map(a -> {
                    double height = a.height() - offsetUpper;
                    Container.PressureInfo pressureInfo = lower.getPressureInfo(height + offsetLower);
                    double pressure = a.pressure() - pressureInfo.pressure();
                    if (pressure > 0.0) {
                        return new Container.PhaseInterface(pressure, a.phaseBelow(), a.phaseAbove(), height);
                    }
                    Phase phase = pressureInfo.phase();
                    return new Container.PhaseInterface(pressure, phase, phase, height);
                });

        Container.PhaseInterface bottom = createVirtualInterface(0.0);
        Container.PhaseInterface top = createVirtualInterface(contactHeight);

        interfaces = Stream.of(Stream.of(bottom, top), lowerStream, upperStream)
                .flatMap(s -> s)
                .sorted(Comparator.comparingDouble(Container.PhaseInterface::height))
                .collect(Collectors.toList());
    }

    private Container.PhaseInterface createVirtualInterface(double localHeight) {
        double lowerGlobal = offsetLower + localHeight;
        double upperGlobal = offsetUpper + localHeight;
        Container.PressureInfo lowerInfo = lower.getPressureInfo(lowerGlobal);
        Container.PressureInfo upperInfo = upper.getPressureInfo(upperGlobal);
        double pressure = upperInfo.pressure() - lowerInfo.pressure();
        Phase phase = pressure > 0? upperInfo.phase():lowerInfo.phase();
        return new Container.PhaseInterface(pressure, phase, phase, localHeight);
    }

    private void calculateFlow() {
        int n = interfaces.size();
        Container.PhaseInterface lower = interfaces.get(0);
        for (int i = 0; i < n - 1; i++) {
            Container.PhaseInterface upper = interfaces.get(i + 1);
            double h1 = lower.height();
            double h2 = upper.height();
            if (h1 == h2) continue;
            double p1 = lower.pressure();
            double p2 = upper.pressure();
            Phase phase1 = lower.phaseAbove();
            Phase phase2 = upper.phaseBelow();
            double density = (phase1.getDensity() + phase2.getDensity()) / 2;


            if (phase1 != phase2) {
                double zero = h1 - p1 * (h1 - h2) / (p1 - p2);
                double v1 = pressureIntegralA(Math.abs(h1 - zero), p1, density);
                double v2 = pressureIntegralA(Math.abs(h2 - zero), p2, density);
                phase1.demand(this, v1);
                phase2.demand(this, v2);
            } else {
                double v = Math.abs(pressureIntegralB(h2 - h1, p1, p2, density));
                phase1.demand(this, v);
            }
            lower = upper;
        }
    }

    private double pressureIntegralA(double height, double pressure, double density) {
        if (height <= 0 || density <= 0) return 0.0;
        double absPressure = Math.abs(pressure);
        return STEP_LENGTH * 2.0/3.0 * height * contactWidth * Math.sqrt(2 * absPressure / density);
    }

    private double pressureIntegralB(double height, double pressure1, double pressure2, double density) {
        if (height <= 0 || density <= 0) return 0.0;
        double p1Abs = Math.abs(pressure1);
        double p2Abs = Math.abs(pressure2);
        double sp1 = Math.sqrt(p1Abs);
        double sp2 = Math.sqrt(p2Abs);
        return STEP_LENGTH * 2.0/3.0 * Math.abs(height) * contactWidth * Math.sqrt(2 / density)
                * (p1Abs + p2Abs + sp1 * sp2) / (sp1 + sp2);
    }

    private void calculateHorizontalFlow() {
        Container.PressureInfo upperBottom = upper.getPressureInfo(0);
        double lowerHeight = lower.getHeight();
        Container.PressureInfo lowerTop = lower.getPressureInfo(lowerHeight);
        if (lowerTop.phase().isEmpty()) lowerTop = lower.getPressureInfo(lowerHeight - 0.01);

        double deltaP = lowerTop.pressure() - upperBottom.pressure();
        if (Math.abs(deltaP) < 1e-12) return;

        boolean downToUp = deltaP > 0;
        Phase sourcePhase = downToUp ? lowerTop.phase() : upperBottom.phase();
        Phase targetPhase = downToUp ? upperBottom.phase() : lowerTop.phase();

        double density = sourcePhase.getDensity();
        if (density <= 0) density = 1.0;

        double area = contactWidth * contactHeight;
        double absDeltaP = Math.abs(deltaP);
        double volume = STEP_LENGTH * area * (2.0/3.0) * Math.sqrt(2 * absDeltaP / density);
        if (volume <= 0) return;

        sourcePhase.demand(this, volume);
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

        if (boxA.overlapY(boxB) > 0) {
            if (determineVertical(boxA, boxB, boxA.touchX(boxB), boxA.overlapZ(boxB))) return ConnectionType.VERTICAL;
            if (determineVertical(boxA, boxB, boxA.touchZ(boxB), boxA.overlapX(boxB))) return ConnectionType.VERTICAL;
        }
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
                int contactMinY = Math.max(boxA.minY, boxB.minY);
                double lowerBottom = lowerEntity.getBoundingBox().minY;
                double upperBottom = upperEntity.getBoundingBox().minY;
                this.offsetLower = (int) (contactMinY - lowerBottom);
                this.offsetUpper = (int) (contactMinY - upperBottom);
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
}