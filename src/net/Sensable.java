package net;

import java.util.*;
import java.util.stream.*;

public interface Sensable<O extends Sensable<O>> {

    public int numOutputs();
    public void updateSensors();

    public void addSensor(SensorNode.Setter<O, ?> sensorSetter) throws IllegalStateException, NullPointerException;
    
    default public void addSensors(Set<SensorNode.Setter<O, ?>> sensorsSetters)
            throws IllegalStateException, NullPointerException {
        
        for (SensorNode.Setter<O, ?> setter : sensorsSetters) {
            this.addSensor(setter);
        }
    }
    
    
    public boolean removeSensor(SensorNode.Setter<O, ?> sensorSetter) throws IllegalStateException, NullPointerException;
    
    default public void removeSensors(Set<SensorNode.Setter<O, ?>> sensors)
            throws IllegalStateException, NullPointerException {
        
        for (SensorNode.Setter<O, ?> setter : sensors) {
            this.removeSensor(setter);
        }
    }


    public boolean findAndRemoveSensor(SensorNode<O, ?, ?> sensor) throws IllegalStateException, NullPointerException;
    
    default public void findAndRemoveSensors(Set<SensorNode<O, ?, ?>> sensors)
            throws IllegalStateException, NullPointerException {
        
        for (SensorNode<O, ?, ?> sensor : sensors) {
            this.findAndRemoveSensor(sensor);
        }
    }

    public Set<SensorNode.Setter<O, ?>> getSensorSetters(int outputId);
    public Map<Integer, Set<SensorNode.Setter<O, ?>>> getSensorSettersMap();
    public Set<SensorNode.Setter<O, ?>> getAllSensorSetters();

    default public Set<SensorNode<O, ?, ?>> getSensors(int outputId) {
        return this.getSensorSetters(outputId).stream()
                .map(SensorNode.Setter::getSensor)
                .collect(Collectors.toSet());
    }

    default public Map<Integer, Set<SensorNode<O, ?, ?>>> getSensorsMap() {
        return this.getSensorSettersMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().stream()
                                        .map(SensorNode.Setter::getSensor)
                                        .collect(Collectors.toSet())
                            ));
    }

    default public Set<SensorNode<O, ?, ?>> getAllSensors() {
        return this.getAllSensorSetters().stream()
                .map(SensorNode.Setter::getSensor)
                .collect(Collectors.toSet());
    }
}
