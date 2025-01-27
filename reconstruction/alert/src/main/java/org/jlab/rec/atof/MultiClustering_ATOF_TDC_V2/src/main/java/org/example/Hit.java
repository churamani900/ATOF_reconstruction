/**
 * Hit Class
 *
 * @author churaman
 *
 * Description:
 * This class represents a hit in the ATOF detector. Each hit is characterized by its sector, 
 * component, z-position, time, and azimuthal angle (phi).
 *
 * Attributes:
 * - Sector: The sector of the hit in the detector (0-14, clockwise looking downstream).
 * - Component: The specific component or paddle where the hit occurred.
 * - Z-Position: The calculated Z-coordinate (in mm) of the hit.
 * - Time: The time of the hit in nanoseconds.
 * - Phi: The azimuthal angle of the hit in radians.
 *
 * Usage:
 *   This class is used in clustering algorithms and detector simulations to store 
 *   and process hit information.
 */


package org.jlab.rec.atof.MultiClustering_ATOF_TDC_V2;

public class Hit {
    private int sector;
    private int component;
    private double zPosition;
    private double time;
    private double phi;

    
    public Hit(int sector, int component, double zPosition, double time, double phi) {
        this.sector = sector;
        this.component = component;
        this.zPosition = zPosition;
        this.time = time;
        this.phi = phi;
    }

    
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
