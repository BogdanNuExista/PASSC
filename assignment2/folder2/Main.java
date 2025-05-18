package folder2;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

// ==== Eventimente Senzori ====
class TemperatureEvent implements Event {
    private final List<Double> values;
    public TemperatureEvent(List<Double> values) { this.values = values; }
    public List<Double> getValues() { return values; }
}

class WaterLevelEvent implements Event {
    private final double value;
    public WaterLevelEvent(double value) { this.value = value; }
    public double getValue() { return value; }
}

// ==== Publishers (Sensors) ====
class TemperatureSensor {
    private BasicEventBus eventBus;
    private Random random = new Random();

    public TemperatureSensor(BasicEventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void generateData() {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < 3; i++) { // Generam 3 valori random
            values.add(random.nextDouble() * 100);
        }
        eventBus.post(new TemperatureEvent(values));
    }
}

class WaterLevelSensor {
    private BasicEventBus eventBus;
    private Random random = new Random();

    public WaterLevelSensor(BasicEventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void generateData() {
        double level = random.nextDouble() * 50;
        eventBus.post(new WaterLevelEvent(level));
    }
}

// ==== Subscribers (Displays) ====
class NumericDisplay implements Subscriber {
    @Override
    public void handleEvent(Event event) {
        if (event instanceof TemperatureEvent) {
            TemperatureEvent tempEvent = (TemperatureEvent) event;
            System.out.println("Numeric Display - Temperatures: " + tempEvent.getValues());
        }
    }
}

class MaxValueDisplay implements Subscriber {
    private double maxTemp = Double.MIN_VALUE;
    @Override
    public void handleEvent(Event event) {
        if (event instanceof TemperatureEvent) {
            TemperatureEvent tempEvent = (TemperatureEvent) event;
            for (double value : tempEvent.getValues()) {
                maxTemp = Math.max(maxTemp, value);
            }
            System.out.println("Max Temp Display - Current Max: " + maxTemp);
        }
    }
}

class WaterLevelDisplay implements Subscriber {
    @Override
    public void handleEvent(Event event) {
        if (event instanceof WaterLevelEvent) {
            WaterLevelEvent waterEvent = (WaterLevelEvent) event;
            System.out.println("Water Level Display: " + waterEvent.getValue());
        }
    }
}

// ==== Main Class ====
public class Main {
    public static void main(String[] args) {
        BasicEventBus eventBus = new BasicEventBus();

        TemperatureSensor tempSensorTimisoara = new TemperatureSensor(eventBus);
        TemperatureSensor tempSensorArad = new TemperatureSensor(eventBus);
        WaterLevelSensor waterSensor = new WaterLevelSensor(eventBus);

        NumericDisplay numericDisplay = new NumericDisplay();
        MaxValueDisplay maxDisplay = new MaxValueDisplay();
        WaterLevelDisplay waterDisplay = new WaterLevelDisplay();

        eventBus.register(TemperatureEvent.class, numericDisplay);
        eventBus.register(TemperatureEvent.class, maxDisplay);
        eventBus.register(WaterLevelEvent.class, waterDisplay);

        System.out.println("=== Simulare Data Senzori ===");

        tempSensorTimisoara.generateData();
        tempSensorArad.generateData();      
        waterSensor.generateData();
    }
}