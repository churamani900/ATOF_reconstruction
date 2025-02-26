/**
 * ATOFHit_Reco_Cluster.java
 * 
 * Description:
 * This program is part of the residuals calculation and clustering efforts for the ATOF detector system. 
 * It processes hit data from the ATOF::adc and MC::True banks in HIPO files, calculates residuals, 
 * and forms clusters by combining bar and wedge hits based on spatial, temporal, and angular proximity.
 * 
 * Features:
 * - Reads HIPO files containing ATOF::adc and MC::True data.
 * - Processes bar and wedge hits to form clusters based on configurable thresholds for:
 *   - Delta Z (position difference along Z-axis)
 *   - Delta Phi (azimuthal angle difference)
 *   - Delta Time (time difference between hits)
 * - Calculates weighted averages for Z, Phi, and Time for clusters.
 * - Computes residuals between reconstructed and true hit positions, angles, and times.
 * - Generates cluster size statistics and outputs detailed per-cluster information.
 * 
 * @author churaman
 * Date: January 24, 2025
 * 
 * Parameters:
 * - `VEFF`: Effective velocity for timing-based Z calculation (200 mm/ns).
 * - `BAR_LENGTH`: Length of the scintillator bar (280 mm).
 * - `WEDGE_SPACING`: Spacing between wedges (3 mm).
 * - `N_WEDGE`: Number of wedges per bar (10).
 * - `Z_THRESHOLD`: Maximum allowed Z difference for clustering (280 mm).
 * - `PHI_THRESHOLD`: Maximum allowed azimuthal angle difference for clustering (0.1 radians).
 * - `TIME_THRESHOLD`: Maximum allowed time difference for clustering (1.7 ns).
 * 
 * Usage:
 * - Run the program with a HIPO file as input:
 *   `java ATOFHit_Reco_Cluster <path_to_hipo_file>`
 * - Ensure the HIPO file contains the required schemas: ATOF::adc and MC::True.
 * 
 * Contact:
 * - Email: cpaudel@nmsu.edu
 * Notes:
 * - The program supports both ATOF::adc and ATOF::tdc versions for flexibility in input formats.
 * - Output includes detailed residuals and clustering information for debugging and analysis purposes.
 */


package org.jlab.rec.atof.RESIDUALS_CALCULATIONS;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.io.HipoReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ATOFHit_Reco_Cluster {

    private static final double VEFF = 200.0; // mm/ns
    private static final double BAR_LENGTH = 280.0; // mm
    private static final double WEDGE_SPACING = 3.0; // mm
    private static final int N_WEDGE = 10;
    private static final double Z_THRESHOLD = 280.0;
    private static final double PHI_THRESHOLD = 0.1; // radians
    private static final double TIME_THRESHOLD = 1.7;

    private static List<Double> deltaZList = new ArrayList<>();
    private static List<Double> deltaPhiList = new ArrayList<>();
    private static List<Double> deltaTimeList = new ArrayList<>();
    private static List<Integer> clusterSizes = new ArrayList<>();
    private static Map<Integer, Integer> clusterSizeCounts = new HashMap<>();

    private static List<Double> zClusterList = new ArrayList<>();
    private static List<Double> phiClusterList = new ArrayList<>();
    private static List<Double> timeClusterList = new ArrayList<>();
    private static List<Double> energyClusterList = new ArrayList<>();

    private static List<Double> deltaZResiduals = new ArrayList<>();
    private static List<Double> deltaPhiResiduals = new ArrayList<>();
    private static List<Double> deltaTimeResiduals = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Please provide the path to the HIPO file.");
            System.exit(1);
        }

        String hipoFilePath = args[0];
        HipoReader reader = new HipoReader();
        reader.open(hipoFilePath);

        if (!reader.getSchemaFactory().hasSchema("ATOF::adc") || !reader.getSchemaFactory().hasSchema("MC::True")) {
            System.err.println("Required schemas not found in the HIPO file.");
            reader.close();
            System.exit(1);
        }

        processEvents(reader);
        reader.close();

        System.out.println("\nCluster Size Summary:");
        for (Map.Entry<Integer, Integer> entry : clusterSizeCounts.entrySet()) {
            System.out.printf("Clusters of size %d: %d\n", entry.getKey(), entry.getValue());
        }
    }

    private static void processEvents(HipoReader reader) {
        Bank atofAdcBank = new Bank(reader.getSchemaFactory().getSchema("ATOF::adc"));
        Bank mcTrueBank = new Bank(reader.getSchemaFactory().getSchema("MC::True"));
        Event event = new Event();

        int eventCount = 0;

        while (reader.hasNext()) {
            reader.nextEvent(event);
            event.read(atofAdcBank);
            event.read(mcTrueBank);

            int numHits = atofAdcBank.getRows();
            List<Hit> barHits = new ArrayList<>();
            List<Hit> wedgeHits = new ArrayList<>();

            System.out.printf("\nProcessing Event #%d with %d hits.\n", eventCount, numHits);

            for (int hitIndex = 0; hitIndex < numHits; hitIndex++) {
                Hit hit = createHit(atofAdcBank, hitIndex);
                if (hit.layer == 0) barHits.add(hit);
                else if (hit.layer >= 10 && hit.layer <= 19) wedgeHits.add(hit);
            }

            if (barHits.size() >= 2) {
                Hit barLeft = barHits.get(0);
                Hit barRight = barHits.get(1);

                double zBar = VEFF * (barLeft.time - barRight.time) / 2;
                double tBar = Math.min(barLeft.time - (zBar - BAR_LENGTH / 2) / VEFF,
                        barRight.time - (zBar + BAR_LENGTH / 2) / VEFF);

                processCluster(barLeft, barRight, wedgeHits, zBar, tBar);
            }

            if (mcTrueBank.getRows() > 0) {
                extractAndCompareTruth(mcTrueBank, barHits);
            }
            eventCount++;
        }
    }

    private static void processCluster(Hit barLeft, Hit barRight, List<Hit> wedgeHits, double zBar, double tBar) {
        List<Hit> clusterWedgeHits = new ArrayList<>();
        System.out.println("Clustering:");

        for (Hit wedgeHit : wedgeHits) {
            double deltaZ = Math.abs(zBar - wedgeHit.zWedge());
            double deltaPhi = normalizePhi(barLeft.phi - wedgeHit.phi);
            double deltaTime = Math.abs(tBar - wedgeHit.time);

            deltaZList.add(deltaZ);
            deltaPhiList.add(deltaPhi);
            deltaTimeList.add(deltaTime);

            if (deltaZ < Z_THRESHOLD && deltaPhi < PHI_THRESHOLD && deltaTime < TIME_THRESHOLD) {
                clusterWedgeHits.add(wedgeHit);
            }
        }

        if (!clusterWedgeHits.isEmpty()) {
            int clusterSize = 2 + clusterWedgeHits.size();
            clusterSizes.add(clusterSize);
            clusterSizeCounts.put(clusterSize, clusterSizeCounts.getOrDefault(clusterSize, 0) + 1);

            double zCluster = calculateWeightedAverageZ(barLeft, barRight, clusterWedgeHits);
            double phiCluster = calculateWeightedAveragePhi(barLeft, barRight, clusterWedgeHits);
            double timeCluster = calculateWeightedAverageTime(barLeft, barRight, clusterWedgeHits);
            double energyCluster = calculateClusterEnergy(barLeft, barRight, clusterWedgeHits);

            zClusterList.add(zCluster);
            phiClusterList.add(phiCluster);
            timeClusterList.add(timeCluster);
            energyClusterList.add(energyCluster);

            System.out.printf("Cluster: ZCluster=%.2f, PhiCluster=%.2f, TimeCluster=%.2f, EnergyCluster=%.2f, Size=%d\n",
                    zCluster, phiCluster, timeCluster, energyCluster, clusterSize);
        }
    }

    private static double calculateWeightedAverageZ(Hit barLeft, Hit barRight, List<Hit> clusterWedgeHits) {
        double weightedSumZ = barLeft.adc * barLeft.zWedge() + barRight.adc * barRight.zWedge();
        double totalWeight = barLeft.adc + barRight.adc;

        for (Hit hit : clusterWedgeHits) {
            weightedSumZ += hit.adc * hit.zWedge();
            totalWeight += hit.adc;
        }

        return totalWeight > 0 ? weightedSumZ / totalWeight : 0.0;
    }

    private static double calculateWeightedAveragePhi(Hit barLeft, Hit barRight, List<Hit> clusterWedgeHits) {
        double weightedSumPhi = barLeft.adc * barLeft.phi + barRight.adc * barRight.phi;
        double totalWeight = barLeft.adc + barRight.adc;

        for (Hit hit : clusterWedgeHits) {
            weightedSumPhi += hit.adc * hit.phi;
            totalWeight += hit.adc;
        }

        return totalWeight > 0 ? weightedSumPhi / totalWeight : 0.0;
    }

    private static double calculateWeightedAverageTime(Hit barLeft, Hit barRight, List<Hit> clusterWedgeHits) {
        double weightedSumTime = barLeft.adc * barLeft.time + barRight.adc * barRight.time;
        double totalWeight = barLeft.adc + barRight.adc;

        for (Hit hit : clusterWedgeHits) {
            weightedSumTime += hit.adc * hit.time;
            totalWeight += hit.adc;
        }

        return totalWeight > 0 ? weightedSumTime / totalWeight : 0.0;
    }

    private static double calculateClusterEnergy(Hit barLeft, Hit barRight, List<Hit> clusterWedgeHits) {
        double totalEnergy = barLeft.adc + barRight.adc;

        for (Hit hit : clusterWedgeHits) {
            totalEnergy += hit.adc;
        }

        return totalEnergy;
    }

    private static double normalizePhi(double phi) {
        while (phi > Math.PI) phi -= 2 * Math.PI;
        while (phi < -Math.PI) phi += 2 * Math.PI;
        return phi;
    }

    private static Hit createHit(Bank bank, int index) {
        int sector = bank.getByte("sector", index);
        int layer = bank.getByte("layer", index);
        int component = bank.getShort("component", index);
        int order = bank.getByte("order", index);
        int adc = bank.getInt("ADC", index);
        int ped = bank.getInt("ped", index);
        float time = bank.getFloat("time", index);
        double phi = 2 * Math.PI * component / 60;
        return new Hit(sector, layer, component, order, adc, time, phi, ped);
    }

    private static void extractAndCompareTruth(Bank mcTrueBank, List<Hit> barHits) {
        for (int i = 0; i < mcTrueBank.getRows(); i++) {
            double truthZ = mcTrueBank.getFloat("avgZ", i);
            double truthPhi = Math.atan2(mcTrueBank.getFloat("avgY", i), mcTrueBank.getFloat("avgX", i));
            double truthTime = mcTrueBank.getFloat("avgT", i);

            for (Hit barHit : barHits) {
                double residualZ = barHit.zWedge() - truthZ;
                double residualPhi = normalizePhi(barHit.phi - truthPhi);
                double residualTime = barHit.time - truthTime;

                deltaZResiduals.add(residualZ);
                deltaPhiResiduals.add(residualPhi);
                deltaTimeResiduals.add(residualTime);

                System.out.printf("Truth Z: %.2f, Reconstructed Z: %.2f, Residual: %.2f\n", truthZ, barHit.zWedge(), residualZ);
                System.out.printf("Truth Phi: %.2f, Reconstructed Phi: %.2f, Residual Phi: %.2f\n", truthPhi, barHit.phi, residualPhi);
                System.out.printf("Truth Time: %.2f, Reconstructed Time: %.2f, Residual Time: %.2f\n", truthTime, barHit.time, residualTime);
            }
        }
    }

    static class Hit {
        int sector, layer, component, order, adc, ped;
        double time, phi;

        Hit(int sector, int layer, int component, int order, int adc, double time, double phi, int ped) {
            this.sector = sector;
            this.layer = layer;
            this.component = component;
            this.order = order;
            this.adc = adc;
            this.time = time;
            this.phi = phi;
            this.ped = ped;
        }

        double zWedge() {
            int wedgeIndex = component % N_WEDGE;
            return (wedgeIndex - (N_WEDGE - 1) / 2.0) * WEDGE_SPACING;
        }
    }
}


//TDC versions

/*
package org.jlab.rec.atof.RESIDUALS_CALCULATIONS_TDC;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.io.HipoReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ATOFHitTDCResiduals {

    private static final double VEFF = 200.0; // mm/ns
    private static final double BAR_LENGTH = 280.0; // mm
    private static final double WEDGE_SPACING = 3.0; // mm
    private static final int N_WEDGE = 10;
    private static final double Z_THRESHOLD = 280.0;
    private static final double PHI_THRESHOLD = 0.1; // radians
    private static final double TIME_THRESHOLD = 1.7;

    private static List<Double> deltaZList = new ArrayList<>();
    private static List<Double> deltaPhiList = new ArrayList<>();
    private static List<Double> deltaTimeList = new ArrayList<>();
    private static List<Double> deltaZResiduals = new ArrayList<>();
    private static List<Double> deltaPhiResiduals = new ArrayList<>();
    private static List<Double> deltaTimeResiduals = new ArrayList<>();
    private static Map<Integer, Integer> clusterSizeCounts = new HashMap<>();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Please provide the path to the HIPO file.");
            System.exit(1);
        }

        String hipoFilePath = args[0];
        HipoReader reader = new HipoReader();
        reader.open(hipoFilePath);

        if (!reader.getSchemaFactory().hasSchema("ATOF::tdc") || !reader.getSchemaFactory().hasSchema("MC::True")) {
            System.err.println("Required schemas not found in the HIPO file.");
            reader.close();
            System.exit(1);
        }

        processEvents(reader);
        reader.close();

        System.out.println("\nCluster Size Summary:");
        for (Map.Entry<Integer, Integer> entry : clusterSizeCounts.entrySet()) {
            System.out.printf("Clusters of size %d: %d\n", entry.getKey(), entry.getValue());
        }
    }

    private static void processEvents(HipoReader reader) {
        Bank atofTdcBank = new Bank(reader.getSchemaFactory().getSchema("ATOF::tdc"));
        Bank mcTrueBank = new Bank(reader.getSchemaFactory().getSchema("MC::True"));
        Event event = new Event();

        int eventCount = 0;

        while (reader.hasNext()) {
            reader.nextEvent(event);
            event.read(atofTdcBank);
            event.read(mcTrueBank);

            List<Hit> barHits = new ArrayList<>();
            List<Hit> wedgeHits = new ArrayList<>();

            for (int hitIndex = 0; hitIndex < atofTdcBank.getRows(); hitIndex++) {
                Hit hit = createHit(atofTdcBank, hitIndex);
                if (hit.isBarHit()) barHits.add(hit);
                else if (hit.isWedgeHit()) wedgeHits.add(hit);
            }

            if (barHits.size() >= 2) {
                Hit barLeft = barHits.stream().filter(Hit::isLeftPMT).findFirst().orElse(null);
                Hit barRight = barHits.stream().filter(Hit::isRightPMT).findFirst().orElse(null);

                if (barLeft != null && barRight != null) {
                    double zBar = VEFF * (barLeft.time - barRight.time) / 2;
                    double tBar = Math.min(
                            barLeft.time - (zBar - BAR_LENGTH / 2) / VEFF,
                            barRight.time - (zBar + BAR_LENGTH / 2) / VEFF
                    );

                    processCluster(barLeft, barRight, wedgeHits, zBar, tBar);
                }
            }

            if (mcTrueBank.getRows() > 0) {
                extractAndCompareTruth(mcTrueBank, barHits);
            }
            eventCount++;
        }
    }

    private static void processCluster(Hit barLeft, Hit barRight, List<Hit> wedgeHits, double zBar, double tBar) {
        List<Hit> clusterWedgeHits = new ArrayList<>();
        System.out.println("Clustering:");

        for (Hit wedgeHit : wedgeHits) {
            double deltaZ = Math.abs(zBar - wedgeHit.zWedge());
            double deltaPhi = normalizePhi(barLeft.phi - wedgeHit.phi);
            double deltaTime = Math.abs(tBar - wedgeHit.time);

            deltaZList.add(deltaZ);
            deltaPhiList.add(deltaPhi);
            deltaTimeList.add(deltaTime);

            if (deltaZ < Z_THRESHOLD && deltaPhi < PHI_THRESHOLD && deltaTime < TIME_THRESHOLD) {
                clusterWedgeHits.add(wedgeHit);
            }
        }

        if (!clusterWedgeHits.isEmpty()) {
            int clusterSize = 2 + clusterWedgeHits.size();
            clusterSizeCounts.put(clusterSize, clusterSizeCounts.getOrDefault(clusterSize, 0) + 1);
        }
    }

    private static void extractAndCompareTruth(Bank mcTrueBank, List<Hit> barHits) {
        for (int i = 0; i < mcTrueBank.getRows(); i++) {
            double truthZ = mcTrueBank.getFloat("avgZ", i);
            double truthPhi = Math.atan2(mcTrueBank.getFloat("avgY", i), mcTrueBank.getFloat("avgX", i));
            double truthTime = mcTrueBank.getFloat("avgT", i);

            for (Hit barHit : barHits) {
                double residualZ = barHit.zWedge() - truthZ;
                double residualPhi = normalizePhi(barHit.phi - truthPhi);
                double residualTime = barHit.time - truthTime;

                deltaZResiduals.add(residualZ);
                deltaPhiResiduals.add(residualPhi);
                deltaTimeResiduals.add(residualTime);

                System.out.printf("Truth Z: %.2f, Reconstructed Z: %.2f, Residual Z: %.2f\n", truthZ, barHit.zWedge(), residualZ);
                System.out.printf("Truth Phi: %.2f, Reconstructed Phi: %.2f, Residual Phi: %.2f\n", truthPhi, barHit.phi, residualPhi);
                System.out.printf("Truth Time: %.2f, Reconstructed Time: %.2f, Residual Time: %.2f\n", truthTime, barHit.time, residualTime);
            }
        }
    }

    private static Hit createHit(Bank bank, int index) {
        int sector = bank.getByte("sector", index);
        int layer = bank.getByte("layer", index);
        int component = bank.getShort("component", index);
        int order = bank.getByte("order", index);
        int tdc = bank.getInt("TDC", index);
        int tot = bank.getInt("ToT", index);
        double time = tdc * 0.015625; // Convert TDC to ns
        double phi = 2 * Math.PI * component / 60;
        double z = (component < 10) ? (component - (N_WEDGE - 1) / 2.0) * WEDGE_SPACING : 0.0;
        return new Hit(sector, layer, component, order, tdc, tot, z, phi, time);
    }

    private static double normalizePhi(double phi) {
        while (phi > Math.PI) phi -= 2 * Math.PI;
        while (phi < -Math.PI) phi += 2 * Math.PI;
        return phi;
    }

    static class Hit {
        int sector, layer, component, order, tdc, tot;
        double z, phi, time;

        Hit(int sector, int layer, int component, int order, int tdc, int tot, double z, double phi, double time) {
            this.sector = sector;
            this.layer = layer;
            this.component = component;
            this.order = order;
            this.tdc = tdc;
            this.tot = tot;
            this.z = z;
            this.phi = phi;
            this.time = time;
        }

        boolean isBarHit() {
            return component == 10;
        }

        boolean isWedgeHit() {
            return component < 10;
        }

        boolean isLeftPMT() {
            return order == 0 && isBarHit();
        }

        boolean isRightPMT() {
            return order == 1 && isBarHit();
        }

        double zWedge() {
            return (component < 10) ? (component - (N_WEDGE - 1) / 2.0) * WEDGE_SPACING : 0.0;
        }
    }
}
*/
