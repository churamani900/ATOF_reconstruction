/**
 * Hit.java
 * 
 * Description:
 * This class represents a hit in the ATOF detector system, providing information about the hit's 
 * position, time, energy, and associated parameters. It is used for residual calculations and 
 * analysis within the ATOF system.
 * 
 * Features:
 * - Stores details of a hit, such as:
 *   - Sector: The detector sector where the hit occurred.
 *   - Layer: The layer in the detector (e.g., bar or wedge).
 *   - Component: The specific component (e.g., PMT) that registered the hit.
 *   - Order: The order of the hit (e.g., left/right PMT for bars).
 *   - ADC: Amplitude of the signal (proportional to energy deposited).
 *   - Time: Time of the hit in nanoseconds.
 *   - Phi: Azimuthal angle of the hit in radians.
 *   - Pedestal: Baseline value subtracted from the ADC signal.
 * 
 * @author churaman
 * Date: January 24, 2025
 * 
 * Usage:
 * - This class can be instantiated to represent a single hit in the ATOF detector.
 * - It provides getter methods for retrieving individual hit attributes.
 * 
 * Parameters:
 * - `sector`: Integer representing the sector number of the detector.
 * - `layer`: Integer representing the layer (bar or wedge).
 * - `component`: Integer representing the specific PMT/component number.
 * - `order`: Integer representing the PMT order (e.g., left/right for bars).
 * - `adc`: Integer representing the ADC value (signal amplitude).
 * - `time`: Double representing the time of the hit in nanoseconds.
 * - `phi`: Double representing the azimuthal angle of the hit in radians.
 * - `pedestal`: Integer representing the pedestal value for ADC correction.
 * Contact:
 * - Email: cpaudel@nmsu.edu
 */


package org.jlab.rec.atof.RESIDUALS_CALCULATIONS;

public class Hit {
    private int sector;
    private int layer;
    private int component;
    private int order;
    private int adc;
    private double time;
    private double phi;
    private int pedestal;

    public Hit(int sector, int layer, int component, int order, int adc, double time, double phi, int pedestal) {
        this.sector = sector;
        this.layer = layer;
        this.component = component;
        this.order = order;
        this.adc = adc;
        this.time = time;
        this.phi = phi;
        this.pedestal = pedestal;
    }

    public int getSector() {
        return sector;
    }

    public int getLayer() {
        return layer;
    }

    public int getComponent() {
        return component;
    }

    public int getOrder() {
        return order;
    }

    public int getAdc() {
        return adc;
    }

    public double getTime() {
        return time;
    }

    public double getPhi() {
        return phi;
    }

    public int getPedestal() {
        return pedestal;
    }
}


/*
package org.jlab.rec.atof.RESIDUALS_CALCULATIONS.org.example;

public class Hit {
    private int sector;
    private int component;
    private double zPosition;
    private double time;
    private double phi;

    // Constructor
    public Hit(int sector, int component, double zPosition, double time, double phi) {
        this.sector = sector;
        this.component = component;
        this.zPosition = zPosition;
        this.time = time;
        this.phi = phi;
    }

    // Getter methods
    public int getSector() {
        return sector;
    }

    public int getComponent() {
        return component;
    }

    public double getzPosition() {
        return zPosition;
    }

    public double getTime() {
        return time;
    }

    public double getPhi() {
        return phi;
    }

    @Override
    public String toString() {
        return "Hit{Sector=" + sector + ", Component=" + component + ", Z=" + zPosition + 
               ", Time=" + time + ", Phi=" + phi + "}";
    }
}
*/
