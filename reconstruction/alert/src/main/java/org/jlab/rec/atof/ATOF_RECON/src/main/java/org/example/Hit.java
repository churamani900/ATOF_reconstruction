/**
 * The {@code Hit} class represents a single detector hit in the ATOF reconstruction process.
 *
 * <p>Each hit contains information about its sector, component, Z position, time, and phi.
 * This data is used for spatial and temporal calculations during event reconstruction
 * and clustering.</p>
 * <p>Key attributes of a hit:
 * - Sector: The sector in which the hit occurred.
 * - Component: The specific detector component that recorded the hit.
 * - Z Position: The hit's Z-coordinate in the detector.
 * - Time: The timestamp of the hit.
 * - Phi: The azimuthal angle of the hit.</p>
 *
 * @author Churaman
 * @version 1.0
 */

package org.jlab.rec.atof.ATOF_RECON;

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
