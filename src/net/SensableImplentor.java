package net;

import java.util.*;

public abstract class SensableImplentor<O extends SensableImplentor<O>> implements Sensable<O> {

    private final Map<Integer, Set<SensorNode.Setter<O, ?>>> setters = new TreeMap<>();
    private final Map<Integer, Set<SensorNode.Setter<O, ?>>> settersViews = new TreeMap<>();
    private final Map<Integer, Set<SensorNode.Setter<O, ?>>> viewOfSettersViews = Collections.unmodifiableMap(settersViews);

    @Override
    public void updateSensors() {
        for (Map.Entry<Integer, Set<SensorNode.Setter<O, ?>>> entry : this.setters.entrySet()) {
            Set<SensorNode.Setter<O, ?>> idSetters = entry.getValue();
            short signal = this.getSignalFor(entry.getKey());
            for (SensorNode.Setter<O, ?> setter : idSetters) {
                setter.setSignal(signal);
            }
        }
    }

    public abstract short getSignalFor(int outputId);

    @Override
    public void addSensor(SensorNode.Setter<O, ?> sensorSetter)
            throws IllegalStateException, NullPointerException {

        int outputId = sensorSetter.getOutputId();
        Set<SensorNode.Setter<O, ?>> idSetters = getIdSetters(outputId);

        if (idSetters == null) {
            idSetters = new LinkedHashSet<>();
            setters.put(outputId, idSetters);
            settersViews.put(outputId, Collections.unmodifiableSet(idSetters));
        }

        idSetters.add(sensorSetter);
    }

    @Override
    public boolean removeSensor(SensorNode.Setter<O, ?> sensorSetter)
            throws IllegalStateException, NullPointerException {

        Set<SensorNode.Setter<O, ?>> idSetters = getIdSetters(sensorSetter.getOutputId());
        if (idSetters == null) return false;
        return idSetters.remove(sensorSetter);
    }

    @Override
    public boolean findAndRemoveSensor(SensorNode<O, ?, ?> sensor)
            throws IllegalStateException, NullPointerException {
        
        Set<SensorNode.Setter<O, ?>> idSetters = getIdSetters(sensor.getOutputId());
        if (idSetters == null) return false;
        
        for (SensorNode.Setter<O, ?> setter : idSetters) {
            if (setter.getSensor() == sensor) {
                idSetters.remove(setter);
                return true;
            }
        }
        return false;
    }

    private Set<SensorNode.Setter<O, ?>> getIdSetters(int outputId)
            throws IllegalStateException {

        if (outputId < 0 || outputId >= this.numOutputs()) {
            throw new IllegalStateException("Invalid outputId " + outputId + " for " + this.getClass());
        }

        return setters.get(outputId);
    }

    @Override
    public Set<SensorNode.Setter<O, ?>> getSensorSetters(int outputId)
            throws IllegalArgumentException {

        if (outputId < 0 || outputId >= this.numOutputs()) {
            throw new IllegalArgumentException("Invalid outputId " + outputId + " for " + this.getClass());
        }

        Set<SensorNode.Setter<O, ?>> idSetters = settersViews.get(outputId);

        if (idSetters == null) {
            idSetters = new LinkedHashSet<>();
            setters.put(outputId, idSetters);
            settersViews.put(outputId, idSetters = Collections.unmodifiableSet(idSetters));
        }
        return idSetters;
    }

    @Override
    public Map<Integer, Set<SensorNode.Setter<O, ?>>> getSensorSettersMap() {
        return this.viewOfSettersViews;
    }

    @Override
    public Set<SensorNode.Setter<O, ?>> getAllSensorSetters() {
        Set<SensorNode.Setter<O, ?>> setters = new HashSet<>();
        for (Set<SensorNode.Setter<O, ?>> idSetters : this.setters.values()) {
            setters.addAll(idSetters);
        }
        return setters;
    }
}
