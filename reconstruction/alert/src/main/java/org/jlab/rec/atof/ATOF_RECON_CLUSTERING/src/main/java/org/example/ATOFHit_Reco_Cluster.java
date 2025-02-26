/**
 * The {@code ATOFHit_Reco_Cluster} class handles the reconstruction and clustering
 * of hits in the ATOF detector. It reads raw hit data from HIPO files, calculates
 * hit properties (Z, Phi, Time), and forms clusters based on spatial and temporal
 * proximity thresholds.
 *
 * <p>Key features:
 * - Processes bar and wedge hits separately.
 * - Performs clustering based on Z, Phi, and Time differences.
 * - Outputs cluster statistics and hit details for analysis.
 *
 * <p>This implementation supports both ADC and TDC versions for reconstruction.</p>
 *
 * @author churaman
 * @version 1.0
 */

package org.jlab.rec.atof.ATOF_RECON_CLUSTERING;
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
    private static final double PHI_THRESHOLD = 0.3;
    private static final double TIME_THRESHOLD = 1.7;

    private static List<Double> deltaZList = new ArrayList<>();
    private static List<Double> deltaPhiList = new ArrayList<>();
    private static List<Double> deltaTimeList = new ArrayList<>();
    private static List<Integer> clusterSizes = new ArrayList<>();
    private static List<Integer> barClusterSizes = new ArrayList<>();
    private static Map<Integer, Integer> clusterSizeCounts = new HashMap<>();

    private static List<Double> zClusterList = new ArrayList<>();
    private static List<Double> phiClusterList = new ArrayList<>();
    private static List<Double> timeClusterList = new ArrayList<>();
    private static List<Double> energyClusterList = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Please provide the path to the HIPO file.");
            System.exit(1);
        }

        String hipoFilePath = args[0];
        HipoReader reader = new HipoReader();
        reader.open(hipoFilePath);

        if (!reader.getSchemaFactory().hasSchema("ATOF::adc")) {
            System.err.println("Required schema not found in the HIPO file.");
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
        Event event = new Event();

        int eventCount = 0;

        while (reader.hasNext()) {
            reader.nextEvent(event);
            event.read(atofAdcBank);

            int numHits = atofAdcBank.getRows();
            List<Hit> barHits = new ArrayList<>();
            List<Hit> wedgeHits = new ArrayList<>();

            System.out.printf("Processing Event #%d with %d hits.\n", eventCount, numHits);

            for (int hitIndex = 0; hitIndex < numHits; hitIndex++) {
                Hit hit = createHit(atofAdcBank, hitIndex);
                if (hit.layer == 0) barHits.add(hit);
                else if (hit.layer >= 10 && hit.layer <= 19) wedgeHits.add(hit);
            }

            if (barHits.size() == 2) {
                barClusterSizes.add(2);
            } else {
                barClusterSizes.add(null);
            }
            if (barHits.size() >= 2) {
                Hit barLeft = barHits.get(0);
                Hit barRight = barHits.get(1);

                double zBar = VEFF * (barLeft.time - barRight.time) / 2;
                double tBar = Math.min(barLeft.time - (zBar - BAR_LENGTH / 2) / VEFF, barRight.time - (zBar + BAR_LENGTH / 2) / VEFF);

                System.out.printf("Bar Hits (for Cluster Calculation):\n");
                printHitDetails(barLeft, "Bar Hit #1");
                printHitDetails(barRight, "Bar Hit #2");
                System.out.printf("  Calculated ZBar: %.2f mm, TBar: %.2f ns\n", zBar, tBar);

                List<Hit> clusterWedgeHits = new ArrayList<>();
                for (Hit wedgeHit : wedgeHits) {
                    double deltaZ = Math.abs(zBar - wedgeHit.zWedge());
                    double deltaPhi = Math.abs(barLeft.phi - wedgeHit.phi);
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

                    System.out.printf("Cluster Formed (Size: %d):\n", clusterSize);
                    System.out.printf("  Cluster Z: %.2f mm, Phi: %.2f rad, Time: %.2f ns, Total Energy: %.2f ADC\n", zCluster, phiCluster, timeCluster, energyCluster);
                }
            }
            eventCount++;
        }
    }

    private static Hit createHit(Bank bank, int index) {
        int sector = bank.getByte("sector", index);
        int layer = bank.getByte("layer", index);
        int component = bank.getShort("component", index);
        int order = bank.getByte("order", index);
        int adc = bank.getInt("ADC", index);
        float time = bank.getFloat("time", index);
        double phi = 2 * Math.PI * component / 60;
        return new Hit(sector, layer, component, order, adc, time, phi);
    }

    private static void printHitDetails(Hit hit, String label) {
        System.out.printf("%s -> Sector: %d, Layer: %d, Component: %d, Order: %d, ADC: %d, Time: %.2f ns, Phi: %.2f rad, Z: %.2f mm\n",
                label, hit.sector, hit.layer, hit.component, hit.order, hit.adc, hit.time, hit.phi, hit.zWedge());
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

    static class Hit {
        int sector, layer, component, order, adc;
        double time, phi;

        Hit(int sector, int layer, int component, int order, int adc, double time, double phi) {
            this.sector = sector;
            this.layer = layer;
            this.component = component;
            this.order = order;
            this.adc = adc;
            this.time = time;
            this.phi = phi;
        }

        double zWedge() {
            int wedgeIndex = component % N_WEDGE;
            return (wedgeIndex - (N_WEDGE - 1) / 2.0) * WEDGE_SPACING;
        }
    }
}


/* 
//TDC version 
package org.jlab.rec.atof.ATOF_RECON_CLUSTERING;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.io.HipoReader;

import java.util.*;

public class ATOFHit_Reco_Cluster {

    private static final double VEFF = 200.0; // mm/ns
    private static final double BAR_LENGTH = 280.0; // mm
    private static final double WEDGE_SPACING = 30.0; // mm
    private static final int NUM_WEDGES = 10;
    private static final int NUM_BARS = 60;
    private static final double Z_THRESHOLD = 30.0;
    private static final double PHI_THRESHOLD = 0.3;
    private static final double TIME_THRESHOLD = 1.7;
    private static final double TDC_RESOLUTION = 0.015625; // ns per TDC unit

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Please provide the path to the HIPO file.");
            System.exit(1);
        }

        String hipoFilePath = args[0];
        HipoReader reader = new HipoReader();
        reader.open(hipoFilePath);

        if (!reader.getSchemaFactory().hasSchema("ATOF::tdc")) {
            System.err.println("Required schema not found in the HIPO file.");
            reader.close();
            System.exit(1);
        }

        processEvents(reader);
        reader.close();
    }

    private static void processEvents(HipoReader reader) {
        Bank atofTdcBank = new Bank(reader.getSchemaFactory().getSchema("ATOF::tdc"));
        Event event = new Event();

        int eventCount = 0;

        while (reader.hasNext()) {
            reader.nextEvent(event);
            event.read(atofTdcBank);

            int numHits = atofTdcBank.getRows();
            List<Hit> barHits = new ArrayList<>();
            List<Hit> wedgeHits = new ArrayList<>();

            System.out.printf("Processing Event #%d with %d hits.\n", eventCount, numHits);

            for (int hitIndex = 0; hitIndex < numHits; hitIndex++) {
                Hit hit = createHit(atofTdcBank, hitIndex);
                if (hit.isBarHit()) {
                    barHits.add(hit);
                } else if (hit.isWedgeHit()) {
                    wedgeHits.add(hit);
                }
            }

            processClusters(barHits, wedgeHits);

            eventCount++;
        }
    }

    private static Hit createHit(Bank bank, int index) {
        int sector = bank.getInt("sector", index);
        int layer = bank.getInt("layer", index);
        int component = bank.getInt("component", index);
        int order = bank.getInt("order", index);
        double time = bank.getInt("TDC", index) * TDC_RESOLUTION;

        double phi;
        if (component < NUM_WEDGES) {
            phi = calculatePhiForWedge(component);
        } else {
            phi = calculatePhiForBar(component);
        }

        return new Hit(sector, layer, component, order, time, phi);
    }

    private static void processClusters(List<Hit> barHits, List<Hit> wedgeHits) {
        if (barHits.size() < 2) {
            System.out.println("Insufficient bar hits for clustering.");
            return;
        }

        Hit barLeft = barHits.get(0);
        Hit barRight = barHits.get(1);

        double zBar = calculateZForBar(barLeft, barRight);
        double tBar = calculateTimeForBar(barLeft, barRight);

        System.out.printf("Bar Cluster: Z = %.2f mm, Time = %.2f ns\n", zBar, tBar);

        List<Hit> clusteredWedgeHits = new ArrayList<>();
        for (Hit wedgeHit : wedgeHits) {
            double deltaZ = Math.abs(zBar - wedgeHit.zWedge());
            double deltaPhi = Math.abs(barLeft.phi - wedgeHit.phi);
            double deltaTime = Math.abs(tBar - wedgeHit.time);

            if (deltaZ < Z_THRESHOLD && deltaPhi < PHI_THRESHOLD && deltaTime < TIME_THRESHOLD) {
                clusteredWedgeHits.add(wedgeHit);
            }
        }

        if (!clusteredWedgeHits.isEmpty()) {
            System.out.printf("Clustered Wedge Hits: %d\n", clusteredWedgeHits.size());
            for (Hit hit : clusteredWedgeHits) {
                printHitDetails(hit, "Clustered Wedge Hit");
            }
        }
    }

    private static double calculateZForBar(Hit barLeft, Hit barRight) {
        return VEFF * (barLeft.time - barRight.time) / 2.0;
    }

    private static double calculateTimeForBar(Hit barLeft, Hit barRight) {
        return (barLeft.time + barRight.time) / 2.0;
    }

    private static double calculatePhiForBar(int barIndex) {
        return -Math.PI + (2 * Math.PI) * barIndex / NUM_BARS;
    }

    private static double calculatePhiForWedge(int wedgeIndex) {
        return -Math.PI + (2 * Math.PI) * wedgeIndex / NUM_WEDGES;
    }

    private static void printHitDetails(Hit hit, String label) {
        System.out.printf("%s -> Sector: %d, Layer: %d, Component: %d, Order: %d, Time: %.2f ns, Phi: %.2f rad, Z: %.2f mm\n",
                label, hit.sector, hit.layer, hit.component, hit.order, hit.time, hit.phi, hit.zWedge());
    }

    static class Hit {
        int sector, layer, component, order;
        double time, phi;

        Hit(int sector, int layer, int component, int order, double time, double phi) {
            this.sector = sector;
            this.layer = layer;
            this.component = component;
            this.order = order;
            this.time = time;
            this.phi = phi;
        }

        boolean isBarHit() {
            return component == 10;
        }

        boolean isWedgeHit() {
            return component < NUM_WEDGES;
        }

        double zWedge() {
            if (isWedgeHit()) {
                return (component - (NUM_WEDGES - 1) / 2.0) * WEDGE_SPACING;
            }
            return 0.0;
        }
    }
}
*/
