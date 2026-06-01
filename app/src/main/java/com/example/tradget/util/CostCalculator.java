package com.example.tradget.util;

/**
 * Utility class for fuel cost calculations.
 */
public class CostCalculator {

    /** Default fuel efficiency: 40 km/litre */
    private static final double DEFAULT_FUEL_EFFICIENCY_KPL = 40.0;

    /**
     * Calculates the total fuel cost for a trip.
     * @param distanceKm Distance in kilometers
     * @param fuelPricePerLitre Fuel price per litre in ₹
     * @return Total fuel cost in ₹
     */
    public static double calculateTotalFuelCost(double distanceKm, double fuelPricePerLitre) {
        double litresNeeded = distanceKm / DEFAULT_FUEL_EFFICIENCY_KPL;
        return litresNeeded * fuelPricePerLitre;
    }

    /**
     * Calculates the cost per person after fuel cost splitting.
     * @param distanceKm Distance in kilometers
     * @param fuelPricePerLitre Fuel price per litre in ₹
     * @param numberOfPassengers Number of passengers to split cost among
     * @return Cost per person in ₹
     */
    public static double calculateCostPerPerson(double distanceKm, 
                                                 double fuelPricePerLitre,
                                                 int numberOfPassengers) {
        if (numberOfPassengers <= 0) return 0;
        double totalCost = calculateTotalFuelCost(distanceKm, fuelPricePerLitre);
        return totalCost / numberOfPassengers;
    }

    /**
     * Formats cost as a string with ₹ symbol.
     * @param cost Cost value in ₹
     * @return Formatted string (e.g., "₹125")
     */
    public static String formatCost(double cost) {
        return String.format("₹ %.0f", cost);
    }
}
