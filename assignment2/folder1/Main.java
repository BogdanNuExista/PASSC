package folder1;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;


// ==== Sensor Events ====
class TemperatureEvent {
    private final List<Double> values;
    public TemperatureEvent(List<Double> values) { this.values = values; }
    public List<Double> getValues() { return values; }
}

class WaterLevelEvent {
    private final double value;
    public WaterLevelEvent(double value) { this.value = value; }
    public double getValues() { return value; }
}

// ==== Publishers (Sensors) ====
class TemperatureSensor {
    private ImprovedEventBus eventBus;
    private Random random = new Random();

    public TemperatureSensor(ImprovedEventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void generateData() {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < 3; i++) { // Generate 3 random values
            values.add(random.nextDouble() * 100);
        }
        eventBus.post(new TemperatureEvent(values));
    }
}

class WaterLevelSensor {
    private ImprovedEventBus eventBus;
    private Random random = new Random();

    public WaterLevelSensor(ImprovedEventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void generateData() {
        double level = random.nextDouble() * 50;
        eventBus.post(new WaterLevelEvent(level));
    }
}

// ==== Subscribers (Displays) ====
class NumericDisplay {
    @Subscribe
    public void handleTemperature(TemperatureEvent event) {
        System.out.println("Numeric Display - Temperature: " + event.getValues());
    }

    @Subscribe
    public void handleWaterLevel(WaterLevelEvent event) {
        System.out.println("Numeric Display - Water Level: " + event.getValues());
    }
}

class MaxValueDisplay {
    private double maxTemp = Double.MIN_VALUE;
    @Subscribe
    public void updateMaxTemperature(TemperatureEvent event) {
        for (double value : event.getValues()) {
            if (value > maxTemp) {
                maxTemp = value;
            }
        }
        System.out.println("Max Temp Display - Current Max: " + maxTemp);
    }
}

// ==== Main Class ====
public class Main {
    public static void main(String[] args) {
        ImprovedEventBus eventBus = new ImprovedEventBus();

        TemperatureSensor sensor = new TemperatureSensor(eventBus);
        WaterLevelSensor waterSensor = new WaterLevelSensor(eventBus);

        NumericDisplay numericDisplay = new NumericDisplay();
        MaxValueDisplay maxDisplay = new MaxValueDisplay();

        eventBus.register(numericDisplay, TemperatureEvent.class);
        eventBus.register(maxDisplay, TemperatureEvent.class);
        eventBus.register(numericDisplay, WaterLevelEvent.class);

        System.out.println("=== Sensor Data Simulation ===");
        sensor.generateData();
        waterSensor.generateData();
        
        System.out.println("=== After Unsubscribing ===");
        eventBus.unregister(maxDisplay, TemperatureEvent.class);
        sensor.generateData();
    }
}