/**
 * The {@code Hit} class represents a single detector hit in the ATOF reconstruction process.
 *
 * <p>Each hit encapsulates essential information about its location and signal, including:
 * - {@code sector}: The sector of the detector where the hit was recorded.
 * - {@code layer}: The detector layer (e.g., bar or wedge hit).
 * - {@code component}: The specific detector component identifier.
 * - {@code order}: Indicates whether the hit is from the inner or outer side of the bar.
 * - {@code adc}: The amplitude of the signal in ADC units.
 * - {@code time}: The timestamp of the hit.
 * - {@code phi}: The azimuthal angle (in radians) of the hit.
 * - {@code pedestal}: The pedestal value used for calibration.
 *
 * <p>This class provides methods to retrieve these attributes for further reconstruction
 * and clustering analysis in the ATOF detector.</p>
 *
 * @author churaman 
 * @version 1.0
 */

package org.jlab.rec.atof.ATOF_RECON_CLUSTERING;

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
package org.jlab.rec.atof.ATOF_RECON_CLUSTERING.org.example;

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
