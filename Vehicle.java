/**
 * Represents a vehicle in the rental system
 */
public class Vehicle {
    private final int id;
    private final String model;
    private final String type;
    private final double rentPerDay;
    private int quantity;
    private int availableQuantity;
    // Removed isAvailable field as it's redundant with availableQuantity > 0

    /**
     * Constructor for vehicles with quantity tracking
     *
     * @param id Vehicle identifier
     * @param model Vehicle model name
     * @param type Vehicle type/category
     * @param rentPerDay Daily rental rate
     * @param quantity Total quantity in fleet
     * @param availableQuantity Currently available quantity
     * @throws IllegalArgumentException if quantities are invalid
     */
    public Vehicle(int id, String model, String type, double rentPerDay, int quantity, int availableQuantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if (availableQuantity < 0 || availableQuantity > quantity) {
            throw new IllegalArgumentException("Available quantity must be between 0 and total quantity");
        }
        if (rentPerDay < 0) {
            throw new IllegalArgumentException("Rent per day cannot be negative");
        }

        this.id = id;
        this.model = model != null ? model : "";
        this.type = type != null ? type : "";
        this.rentPerDay = rentPerDay;
        this.quantity = quantity;
        this.availableQuantity = availableQuantity;
    }

    /**
     * Original constructor with isAvailable parameter (maintained for compatibility with RentalManager)
     *
     * @param id Vehicle identifier
     * @param model Vehicle model name
     * @param type Vehicle type/category
     * @param rentPerDay Daily rental rate
     * @param isAvailable Whether vehicle is available (for compatibility)
     * @param quantity Total quantity in fleet
     * @param availableQuantity Currently available quantity
     * @throws IllegalArgumentException if quantities are invalid
     */
    public Vehicle(int id, String model, String type, double rentPerDay, boolean isAvailable, int quantity, int availableQuantity) {
        this(id, model, type, rentPerDay, quantity, availableQuantity);
        // Note: isAvailable parameter is ignored as it's redundant with availableQuantity
    }

    /**
     * Constructor for vehicles with total quantity only (all available)
     *
     * @param id Vehicle identifier
     * @param model Vehicle model name
     * @param type Vehicle type/category
     * @param rentPerDay Daily rental rate
     * @param quantity Total quantity in fleet (all available)
     * @throws IllegalArgumentException if quantity is invalid
     */
    public Vehicle(int id, String model, String type, double rentPerDay, int quantity) {
        this(id, model, type, rentPerDay, quantity, quantity);
    }

    /**
     * Legacy constructor (backward compatibility)
     *
     * @param id Vehicle identifier
     * @param model Vehicle model name
     * @param type Vehicle type/category
     * @param rentPerDay Daily rental rate
     * @param isAvailable Whether vehicle is available
     */
    public Vehicle(int id, String model, String type, double rentPerDay, boolean isAvailable) {
        this(id, model, type, rentPerDay, 1, isAvailable ? 1 : 0);
    }

    /**
     * Update the total quantity of this vehicle
     *
     * @param quantity New total quantity
     * @throws IllegalArgumentException if quantity is invalid
     */
    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }

        this.quantity = quantity;

        // Ensure available quantity is not greater than new total
        if (this.availableQuantity > quantity) {
            this.availableQuantity = quantity;
        }
    }

    /**
     * Update the available quantity of this vehicle
     *
     * @param availableQuantity New available quantity
     * @throws IllegalArgumentException if available quantity is invalid
     */
    public void setAvailableQuantity(int availableQuantity) {
        if (availableQuantity < 0 || availableQuantity > quantity) {
            throw new IllegalArgumentException("Available quantity must be between 0 and total quantity");
        }

        this.availableQuantity = availableQuantity;
    }

    /**
     * Update both quantity values at once (for atomic updates)
     *
     * @param quantity New total quantity
     * @param availableQuantity New available quantity
     * @throws IllegalArgumentException if quantities are invalid
     */
    public void updateQuantities(int quantity, int availableQuantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if (availableQuantity < 0 || availableQuantity > quantity) {
            throw new IllegalArgumentException("Available quantity must be between 0 and total quantity");
        }

        this.quantity = quantity;
        this.availableQuantity = availableQuantity;
    }

    /**
     * Get the vehicle identifier
     * @return Vehicle ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get the vehicle model name
     * @return Model name
     */
    public String getModel() {
        return model;
    }

    /**
     * Get the vehicle type/category
     * @return Vehicle type
     */
    public String getType() {
        return type;
    }

    /**
     * Get the daily rental rate
     * @return Rent per day
     */
    public double getRentPerDay() {
        return rentPerDay;
    }

    /**
     * Check if vehicle is available for rent
     * @return true if at least one vehicle is available
     */
    public boolean isAvailable() {
        return availableQuantity > 0;
    }

    /**
     * Get total quantity in fleet
     * @return Total quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Get available quantity for rent
     * @return Available quantity
     */
    public int getAvailableQuantity() {
        return availableQuantity;
    }

    /**
     * Rent one vehicle from available inventory
     * @return true if rental was successful
     */
    public boolean rent() {
        if (availableQuantity > 0) {
            availableQuantity--;
            return true;
        }
        return false;
    }

    /**
     * Return one vehicle to available inventory
     * @return true if return was successful
     */
    public boolean returnVehicle() {
        if (availableQuantity < quantity) {
            availableQuantity++;
            return true;
        }
        return false;
    }

    /**
     * Get human-readable availability status
     * @return Availability status string
     */
    public String getAvailabilityStatus() {
        if (availableQuantity == 0) {
            return "Rented";
        } else if (availableQuantity == quantity) {
            return "Available (" + availableQuantity + ")";
        } else {
            return availableQuantity + "/" + quantity + " Available";
        }
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Model: %s, Type: %s, Rent: $%.2f per day, Status: %s",
                id, model, type, rentPerDay, getAvailabilityStatus());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Vehicle other = (Vehicle) obj;
        return id == other.id &&
                Double.compare(other.rentPerDay, rentPerDay) == 0 &&
                quantity == other.quantity &&
                availableQuantity == other.availableQuantity &&
                model.equals(other.model) &&
                type.equals(other.type);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + id;
        result = 31 * result + model.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (int) (Double.doubleToLongBits(rentPerDay) ^ (Double.doubleToLongBits(rentPerDay) >>> 32));
        result = 31 * result + quantity;
        result = 31 * result + availableQuantity;
        return result;
    }
}