package folder11;

import java.util.Random;

// ==== Sensor Events ====
class TemperatureEvent {
    private final double value;
    public TemperatureEvent(double value) { this.value = value; }
    public double getValue() { return value; }
}

class WaterLevelEvent {
    private final double level;
    public WaterLevelEvent(double level) { this.level = level; }
    public double getLevel() { return level; }
}

// ==== Publishers (Sensors) ====
class TemperatureSensor {
    private MethodNameEventBus eventBus;
    private Random random = new Random();

    public TemperatureSensor(MethodNameEventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void generateData() {
        double temp = random.nextDouble() * 100;
        eventBus.post(new TemperatureEvent(temp));
    }
}

class WaterLevelSensor {
    private MethodNameEventBus eventBus;
    private Random random = new Random();

    public WaterLevelSensor(MethodNameEventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void generateData() {
        double level = random.nextDouble() * 10;
        eventBus.post(new WaterLevelEvent(level));
    }
}

// ==== Subscribers (Displays) ====
class NumericDisplay {
    public void displayTemperature(TemperatureEvent event) {
        System.out.println("Numeric Display - Temperature: " + event.getValue());
    }

    public void displayWaterLevel(WaterLevelEvent event) {
        System.out.println("Numeric Display - Water Level: " + event.getLevel());
    }
}

class MaxValueDisplay {
    private double maxTemp = Double.MIN_VALUE;
    public void updateMax(TemperatureEvent event) {
        maxTemp = Math.max(maxTemp, event.getValue());
        System.out.println("Max Temp Display - Current Max: " + maxTemp);
    }
}

// ==== Main Class ====
public class Main11 {
    public static void main(String[] args) {
        MethodNameEventBus eventBus = new MethodNameEventBus();
        TemperatureSensor sensor = new TemperatureSensor(eventBus);
        WaterLevelSensor waterSensor = new WaterLevelSensor(eventBus);
        
        NumericDisplay numericDisplay = new NumericDisplay();
        MaxValueDisplay maxDisplay = new MaxValueDisplay();

        // Explicit registration with method names
        eventBus.register(TemperatureEvent.class, numericDisplay, "displayTemperature");
        eventBus.register(TemperatureEvent.class, maxDisplay, "updateMax");
        eventBus.register(WaterLevelEvent.class, numericDisplay, "displayWaterLevel");

        System.out.println("=== Sensor Data Simulation (Method Name Approach) ===");
        sensor.generateData();
        waterSensor.generateData();
    }
}