/**
 * Represents a vehicle in the rental system
 */
public class Vehicle {
    private int id;
    private String model;
    private String type;
    private double rentPerDay;
    private boolean available;

    /**
     * Constructs a vehicle with the given attributes
     */
    public Vehicle(int id, String model, String type, double rentPerDay, boolean available) {
        this.id = id;
        this.model = model;
        this.type = type;
        this.rentPerDay = rentPerDay;
        this.available = available;
    }

    /**
     * Gets the vehicle ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the vehicle model
     */
    public String getModel() {
        return model;
    }

    /**
     * Gets the vehicle type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the rent per day amount
     */
    public double getRentPerDay() {
        return rentPerDay;
    }

    /**
     * Checks if the vehicle is available for rent
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Sets the availability status of the vehicle
     */
    public void setAvailable(boolean available) {
        this.available = available;
    }
}