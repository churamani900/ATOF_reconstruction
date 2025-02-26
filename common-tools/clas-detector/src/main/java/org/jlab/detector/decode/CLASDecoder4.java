package org.jlab.detector.decode;

import org.jlab.detector.scalers.DaqScalers;
import java.util.ArrayList;
import java.util.List;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import org.jlab.detector.base.DetectorDescriptor;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.decode.DetectorDataDgtz.HelicityDecoderData;
import org.jlab.detector.helicity.HelicityBit;
import org.jlab.detector.helicity.HelicitySequence;
import org.jlab.detector.helicity.HelicityState;
import org.jlab.detector.pulse.ModeAHDC;

import org.jlab.logging.DefaultLogger;

import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.evio.EvioSource;
import org.jlab.io.hipo.HipoDataEvent;
import org.jlab.io.hipo.HipoDataSync;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.hipo4.io.HipoWriterSorted;

import org.jlab.utils.benchmark.ProgressPrintout;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.utils.options.OptionParser;
import org.jlab.utils.system.ClasUtilsFile;

/**
 *
 * @author gavalian
 */
public class CLASDecoder4 {

    private CodaEventDecoder          codaDecoder = null;
    private DetectorEventDecoder  detectorDecoder = null;
    private List<DetectorDataDgtz>       dataList = new ArrayList<>();
    private HipoDataSync                   writer = null;
    private HipoDataEvent               hipoEvent = null;
    private boolean              isRunNumberFixed = false;
    private int                  decoderDebugMode = 0;
    private SchemaFactory        schemaFactory    = new SchemaFactory();
    private ModeAHDC ahdcExtractor                = new ModeAHDC();

    public CLASDecoder4(boolean development){
        codaDecoder = new CodaEventDecoder();
        detectorDecoder = new DetectorEventDecoder(development);
        writer = new HipoDataSync();
        hipoEvent = (HipoDataEvent) writer.createEvent();
        String dir = ClasUtilsFile.getResourceDir("CLAS12DIR", "etc/bankdefs/hipo4");
        schemaFactory.initFromDirectory(dir);
        DefaultLogger.debug();
    }

    public CLASDecoder4(){
        codaDecoder = new CodaEventDecoder();
        detectorDecoder = new DetectorEventDecoder();
        writer = new HipoDataSync();
        hipoEvent = (HipoDataEvent) writer.createEvent();
        String dir = ClasUtilsFile.getResourceDir("CLAS12DIR", "etc/bankdefs/hipo4");
        schemaFactory.initFromDirectory(dir);
        DefaultLogger.debug();
    }

    public static CLASDecoder createDecoder(){
        CLASDecoder decoder = new CLASDecoder();
        return decoder;
    }

    public static CLASDecoder createDecoderDevel(){
        CLASDecoder decoder = new CLASDecoder(true);
        return decoder;
    }

    public void setVariation(String variation) {
        detectorDecoder.setVariation(variation);
    }

    public void setDebugMode(int mode){
        this.decoderDebugMode = mode;
    }

    public void setRunNumber(int run){
        if(this.isRunNumberFixed==false){
            this.detectorDecoder.setRunNumber(run);
        }
    }

    public void setRunNumber(int run, boolean fixed){
        this.isRunNumberFixed = fixed;
        this.detectorDecoder.setRunNumber(run);
        System.out.println(" SETTING RUN NUMBER TO " + run + " FIXED = " + this.isRunNumberFixed);
    }

    public CodaEventDecoder getCodaEventDecoder() {
	return codaDecoder;
    }

    public void initEvent(DataEvent event){

        if(event instanceof EvioDataEvent){
            EvioDataEvent evioEvent = (EvioDataEvent) event;
            if(evioEvent.getHandler().getStructure()!=null){
                try {

                    dataList = codaDecoder.getDataEntries( (EvioDataEvent) event);
                    
                    //-----------------------------------------------------------------------------
                    // This part reads the BITPACKED FADC data from tag=57638 Format (cmcms)
                    // Then unpacks into Detector Digigitized data, and appends to existing buffer
                    // Modified on 9/5/2018
                    //-----------------------------------------------------------------------------
                    
                    List<FADCData>  fadcPacked = codaDecoder.getADCEntries((EvioDataEvent) event);
                    
                    if(fadcPacked!=null){
                        List<DetectorDataDgtz> fadcUnpacked = FADCData.convert(fadcPacked);
                        dataList.addAll(fadcUnpacked);
                    }
                    //  END of Bitpacked section
                    //-----------------------------------------------------------------------------
                    
                    if(this.decoderDebugMode>0){
                        System.out.println("\n>>>>>>>>> RAW decoded data");
                        for(DetectorDataDgtz data : dataList){
                            System.out.println(data);
                        }
                    }
                    int runNumberCoda = codaDecoder.getRunNumber();
                    this.setRunNumber(runNumberCoda);
                    
                    detectorDecoder.translate(dataList);
                    detectorDecoder.fitPulses(dataList);
                    if(this.decoderDebugMode>0){
                        System.out.println("\n>>>>>>>>> TRANSLATED data");
                        for(DetectorDataDgtz data : dataList){
                            System.out.println(data);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }
    /**
     * return list of digitized ADC values from internal list
     * @param type detector type
     * @return
     */
    public List<DetectorDataDgtz>  getEntriesADC(DetectorType type){
        return this.getEntriesADC(type, dataList);
    }
    /**
     * returns ADC entries from decoded data for given detector TYPE
     * @param type detector type
     * @param entries digitized data list
     * @return list of ADC's for detector type
     */
    public List<DetectorDataDgtz>  getEntriesADC(DetectorType type,
            List<DetectorDataDgtz> entries){
        List<DetectorDataDgtz>  adc = new ArrayList<>();
        for(DetectorDataDgtz entry : entries){
            if(entry.getDescriptor().getType()==type){
                if(entry.getADCSize()>0&&entry.getTDCSize()==0){
                    adc.add(entry);
                }
            }
        }

        return adc;
    }

    public List<DetectorDataDgtz>  getEntriesTDC(DetectorType type){
        return getEntriesTDC(type,dataList);
    }

    /**
     * returns TDC entries from decoded data for given detector type
     * @param type detector type
     * @param entries digitized data list
     * @return list of ADC's for detector type
     */
    public List<DetectorDataDgtz>  getEntriesTDC(DetectorType type,
            List<DetectorDataDgtz> entries){
        List<DetectorDataDgtz>  tdc = new ArrayList<>();
        for(DetectorDataDgtz entry : entries){
            if(entry.getDescriptor().getType()==type){
                if(entry.getTDCSize()>0&&entry.getADCSize()==0){
                    tdc.add(entry);
                }
            }
        }
        return tdc;
    }

    public List<DetectorDataDgtz>  getEntriesVTP(DetectorType type){
        return getEntriesVTP(type,dataList);
    }
    /**
     * returns VTP entries from decoded data for given detector type
     * @param type detector type
     * @param entries digitized data list
     * @return list of VTP's for detector type
     */
    public List<DetectorDataDgtz>  getEntriesVTP(DetectorType type,
        List<DetectorDataDgtz> entries){
        List<DetectorDataDgtz>  vtp = new ArrayList<>();
        for(DetectorDataDgtz entry : entries){
            if(entry.getDescriptor().getType()==type){
                if(entry.getVTPSize()>0){
                    vtp.add(entry);
                }
            }
        }
        return vtp;
    }

    public List<DetectorDataDgtz>  getEntriesSCALER(DetectorType type){
        return getEntriesSCALER(type,dataList);
    }
    /**
     * returns VTP entries from decoded data for given detector type
     * @param type detector type
     * @param entries digitized data list
     * @return list of VTP's for detector type
     */
    public List<DetectorDataDgtz>  getEntriesSCALER(DetectorType type,
        List<DetectorDataDgtz> entries){
        List<DetectorDataDgtz>  scaler = new ArrayList<>();
        for(DetectorDataDgtz entry : entries){
            if(entry.getDescriptor().getType()==type){
                if(entry.getSCALERSize()>0){
                    scaler.add(entry);
                }
            }
        }
        return scaler;
    }

    public void extractPulses(Event event) {
        ahdcExtractor.update(6, null, event, schemaFactory, "AHDC::wf", "AHDC::adc");
    }

    public Bank getDataBankWF(String name, DetectorType type) {
        List<DetectorDataDgtz> a = this.getEntriesADC(type);
        Bank b = new Bank(schemaFactory.getSchema(name), a.size());
        for (int i=0; i<a.size(); ++i) {
            b.putByte("sector", i, (byte) a.get(i).getDescriptor().getSector());
            b.putByte("layer", i, (byte) a.get(i).getDescriptor().getLayer());
            b.putShort("component", i, (short) a.get(i).getDescriptor().getComponent());
            b.putByte("order", i, (byte) a.get(i).getDescriptor().getOrder());
            b.putLong("timestamp", i, a.get(i).getADCData(0).getTimeStamp());
            DetectorDataDgtz.ADCData xxx = a.get(i).getADCData(0);
            for (int j=0; j<xxx.getPulseSize(); ++j)
                b.putShort(String.format("s%d",j+1), i, xxx.getPulseValue(j));
        }
        return b;
    }

    public Bank getDataBankADC(String name, DetectorType type){

        List<DetectorDataDgtz> adcDGTZ = this.getEntriesADC(type);

        if(schemaFactory.hasSchema(name)==false) return null;

        Bank adcBANK = new Bank(schemaFactory.getSchema(name), adcDGTZ.size());

        for(int i = 0; i < adcDGTZ.size(); i++){
            adcBANK.putByte("sector", i, (byte) adcDGTZ.get(i).getDescriptor().getSector());
            adcBANK.putByte("layer", i, (byte) adcDGTZ.get(i).getDescriptor().getLayer());
            adcBANK.putShort("component", i, (short) adcDGTZ.get(i).getDescriptor().getComponent());
            adcBANK.putByte("order", i, (byte) adcDGTZ.get(i).getDescriptor().getOrder());
            adcBANK.putInt("ADC", i, adcDGTZ.get(i).getADCData(0).getADC());
            adcBANK.putFloat("time", i, (float) adcDGTZ.get(i).getADCData(0).getTime());
            adcBANK.putShort("ped", i, (short) adcDGTZ.get(i).getADCData(0).getPedestal());
            if(name == "BST::adc") adcBANK.putLong("timestamp", i, adcDGTZ.get(i).getADCData(0).getTimeStamp());
            if(name.equals("BMT::adc")||name.equals("FMT::adc")|| name.equals("FTTRK::adc")){
            	adcBANK.putInt("ADC", i, adcDGTZ.get(i).getADCData(0).getHeight());
            	adcBANK.putInt("integral", i, adcDGTZ.get(i).getADCData(0).getIntegral());
            	adcBANK.putLong("timestamp", i, adcDGTZ.get(i).getADCData(0).getTimeStamp());
            }
            if(name == "BAND::adc") adcBANK.putInt("amplitude", i, adcDGTZ.get(i).getADCData(0).getHeight());
         }
        return adcBANK;
    }


    public Bank getDataBankTDC(String name, DetectorType type){

        List<DetectorDataDgtz> tdcDGTZ = this.getEntriesTDC(type);
        if(schemaFactory.hasSchema(name)==false) return null;
        Bank tdcBANK = new Bank(schemaFactory.getSchema(name), tdcDGTZ.size());

        if(tdcBANK==null) return null;

        for(int i = 0; i < tdcDGTZ.size(); i++){
            tdcBANK.putByte("sector", i, (byte) tdcDGTZ.get(i).getDescriptor().getSector());
            tdcBANK.putByte("layer", i, (byte) tdcDGTZ.get(i).getDescriptor().getLayer());
            tdcBANK.putShort("component", i, (short) tdcDGTZ.get(i).getDescriptor().getComponent());
            tdcBANK.putByte("order", i, (byte) tdcDGTZ.get(i).getDescriptor().getOrder());
            tdcBANK.putInt("TDC", i, tdcDGTZ.get(i).getTDCData(0).getTime());
        }
        return tdcBANK;
    }

    public Bank getDataBankTDCPetiroc(String name, DetectorType type){

        List<DetectorDataDgtz> tdcDGTZ = this.getEntriesTDC(type);
        if(schemaFactory.hasSchema(name)==false){
          System.out.println("WARNING: No schema for TDC type : "  + type);
          return null;
        }
        Bank tdcBANK = new Bank(schemaFactory.getSchema(name), tdcDGTZ.size());

        if(tdcBANK==null) return null;

        // Not sure why  the schemea information isn't used here. 
        for(int i = 0; i < tdcDGTZ.size(); i++){
            tdcBANK.putByte("sector", i, (byte) tdcDGTZ.get(i).getDescriptor().getSector());
            tdcBANK.putByte("layer", i, (byte) tdcDGTZ.get(i).getDescriptor().getLayer());
            tdcBANK.putShort("component", i, (short) tdcDGTZ.get(i).getDescriptor().getComponent());
            tdcBANK.putByte("order", i, (byte) tdcDGTZ.get(i).getDescriptor().getOrder());
            tdcBANK.putInt("TDC", i, tdcDGTZ.get(i).getTDCData(0).getTime());
            tdcBANK.putInt("ToT", i, tdcDGTZ.get(i).getTDCData(0).getToT());
            //System.err.println("event: " + tdcDGTZ.get(i).toString());
        }
        return tdcBANK;
    }


    public Bank getDataBankTimeStamp(String name, DetectorType type) {

        List<DetectorDataDgtz> tdcDGTZ = this.getEntriesTDC(type);
        if(schemaFactory.hasSchema(name)==false) return null;
        Map<Integer, DetectorDataDgtz> tsMap = new LinkedHashMap<>();
        for(DetectorDataDgtz tdc : tdcDGTZ) {
            DetectorDescriptor desc = tdc.getDescriptor();
            int hash = ((desc.getCrate()<<8)&0xFF00) | (desc.getSlot()&0x00FF);
            if(tsMap.containsKey(hash)) {
                if(tsMap.get(hash).getTimeStamp() != tdc.getTimeStamp()) 
                    System.out.println("WARNING: inconsistent timestamp for DCRB crate/slot " 
                                       + desc.getCrate() + "/" + desc.getSlot());
            }
            else {
                tsMap.put(hash, tdc);
            }
        }
        
        Bank tsBANK = new Bank(schemaFactory.getSchema(name), tsMap.size());

        if(tsBANK==null) return null;
        
        int i=0;
        for(DetectorDataDgtz tdc : tsMap.values()) {
            tsBANK.putByte("crate", i, (byte) tdc.getDescriptor().getCrate());
            tsBANK.putByte("slot",  i, (byte) tdc.getDescriptor().getSlot());
            tsBANK.putLong("timestamp", i, tdc.getTimeStamp());
            i++;
        }
        return tsBANK;
    }
    
    public Bank getDataBankUndecodedADC(String name, DetectorType type){
        List<DetectorDataDgtz> adcDGTZ = this.getEntriesADC(type);
        Bank adcBANK = new Bank(schemaFactory.getSchema(name), adcDGTZ.size());

        for(int i = 0; i < adcDGTZ.size(); i++){
            adcBANK.putByte("crate", i, (byte) adcDGTZ.get(i).getDescriptor().getCrate());
            adcBANK.putByte("slot", i, (byte) adcDGTZ.get(i).getDescriptor().getSlot());
            adcBANK.putShort("channel", i, (short) adcDGTZ.get(i).getDescriptor().getChannel());
            adcBANK.putInt("ADC", i, adcDGTZ.get(i).getADCData(0).getADC());
            adcBANK.putFloat("time", i, (float) adcDGTZ.get(i).getADCData(0).getTime());
            adcBANK.putShort("ped", i, (short) adcDGTZ.get(i).getADCData(0).getPedestal());
        }
        return adcBANK;
    }

    public Bank getDataBankUndecodedTDC(String name, DetectorType type){

        List<DetectorDataDgtz> tdcDGTZ = this.getEntriesTDC(type);

        Bank tdcBANK = new Bank(schemaFactory.getSchema(name), tdcDGTZ.size());
        if(tdcBANK==null) return null;

        for(int i = 0; i < tdcDGTZ.size(); i++){
            tdcBANK.putByte("crate", i, (byte) tdcDGTZ.get(i).getDescriptor().getCrate());
            tdcBANK.putByte("slot", i, (byte) tdcDGTZ.get(i).getDescriptor().getSlot());
            tdcBANK.putShort("channel", i, (short) tdcDGTZ.get(i).getDescriptor().getChannel());
            tdcBANK.putInt("TDC", i, tdcDGTZ.get(i).getTDCData(0).getTime());
        }
        return tdcBANK;
    }

    public Bank getDataBankUndecodedVTP(String name, DetectorType type){

        List<DetectorDataDgtz> vtpDGTZ = this.getEntriesVTP(type);

        Bank vtpBANK = new Bank(schemaFactory.getSchema(name), vtpDGTZ.size());
        if(vtpBANK==null) return null;

        for(int i = 0; i < vtpDGTZ.size(); i++){
            vtpBANK.putByte("crate", i, (byte) vtpDGTZ.get(i).getDescriptor().getCrate());
            vtpBANK.putInt("word", i, vtpDGTZ.get(i).getVTPData(0).getWord());
        }
        return vtpBANK;
    }

    public Bank getDataBankUndecodedSCALER(String name, DetectorType type){

        List<DetectorDataDgtz> scalerDGTZ = this.getEntriesSCALER(type);

        Bank scalerBANK = new Bank(schemaFactory.getSchema(name), scalerDGTZ.size());
        if(scalerBANK==null) return null;

        for(int i = 0; i < scalerDGTZ.size(); i++){
            scalerBANK.putByte("crate", i, (byte) scalerDGTZ.get(i).getDescriptor().getCrate());
            scalerBANK.putByte("slot", i, (byte) scalerDGTZ.get(i).getDescriptor().getSlot());
            scalerBANK.putShort("channel", i, (short) scalerDGTZ.get(i).getDescriptor().getChannel());
            scalerBANK.putByte("helicity", i, (byte) scalerDGTZ.get(i).getSCALERData(0).getHelicity());
            scalerBANK.putByte("quartet", i, (byte) scalerDGTZ.get(i).getSCALERData(0).getQuartet());
            scalerBANK.putLong("value", i, scalerDGTZ.get(i).getSCALERData(0).getValue());
        }
        return scalerBANK;
    }

    public Event getDataEvent(DataEvent rawEvent){
        this.initEvent(rawEvent);
        return getDataEvent();
    }

    public Event getDataEvent(){

        Event event = new Event();

        String[]         wfBankNames = new String[]{"AHDC::wf"};
        DetectorType[]   wfBankTypes = new DetectorType[]{DetectorType.AHDC};
        String[]        adcBankNames = new String[]{"FTOF::adc","ECAL::adc","FTCAL::adc",
                                                    "FTHODO::adc", "FTTRK::adc",
                                                    "HTCC::adc","BST::adc","CTOF::adc",
                                                    "CND::adc","LTCC::adc","BMT::adc",
                                                    "FMT::adc","HEL::adc","RF::adc",
                                                    "BAND::adc","RASTER::adc"};
        DetectorType[]  adcBankTypes = new DetectorType[]{DetectorType.FTOF,DetectorType.ECAL,DetectorType.FTCAL,
                                                          DetectorType.FTHODO,DetectorType.FTTRK,
                                                          DetectorType.HTCC,DetectorType.BST,DetectorType.CTOF,
                                                          DetectorType.CND,DetectorType.LTCC,DetectorType.BMT,
                                                          DetectorType.FMT,DetectorType.HEL,DetectorType.RF,
                                                          DetectorType.BAND, DetectorType.RASTER};

        String[] tdcBankNames = new String[]{"FTOF::tdc","ECAL::tdc","DC::tdc",
                                             "HTCC::tdc","LTCC::tdc","CTOF::tdc",
                                             "CND::tdc","RF::tdc","RICH::tdc",
                                             "BAND::tdc"};
        DetectorType[] tdcBankTypes = new DetectorType[]{DetectorType.FTOF,DetectorType.ECAL,
                                                         DetectorType.DC,DetectorType.HTCC,DetectorType.LTCC,
                                                         DetectorType.CTOF,DetectorType.CND,DetectorType.RF,
                                                         DetectorType.RICH,DetectorType.BAND};

        for(int i = 0; i < adcBankTypes.length; i++){
            Bank adcBank = getDataBankADC(adcBankNames[i],adcBankTypes[i]);
            if(adcBank!=null){
                if(adcBank.getRows()>0){
                    event.write(adcBank);
                }
            }
        }

        for(int i = 0; i < wfBankTypes.length; i++){
            Bank wfBank = getDataBankWF(wfBankNames[i],wfBankTypes[i]);
            if(wfBank!=null && wfBank.getRows()>0){
                event.write(wfBank);
            }
        }

        for(int i = 0; i < tdcBankTypes.length; i++){
            Bank tdcBank = getDataBankTDC(tdcBankNames[i],tdcBankTypes[i]);
            if(tdcBank!=null){
                if(tdcBank.getRows()>0){
                    event.write(tdcBank);
                }
            }
        }
        try {
            // Do ATOF 
            Bank tdcBank = getDataBankTDCPetiroc("ATOF::tdc",DetectorType.ATOF);
            if(tdcBank!=null){
                if(tdcBank.getRows()>0){
                    event.write(tdcBank);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }


        try {
            Bank tsBank = getDataBankTimeStamp("DC::jitter", DetectorType.DC);
            if(tsBank != null) {
                if(tsBank.getRows()>0) {
                    event.write(tsBank);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        /**
         * Adding un-decoded banks to the event
         */
        try {
            Bank adcBankUD = this.getDataBankUndecodedADC("RAW::adc", DetectorType.UNDEFINED);
            if(adcBankUD!=null){
                if(adcBankUD.getRows()>0){
                    event.write(adcBankUD);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        try {
            Bank tdcBankUD = this.getDataBankUndecodedTDC("RAW::tdc", DetectorType.UNDEFINED);
            if(tdcBankUD!=null){
                if(tdcBankUD.getRows()>0){
                    event.write(tdcBankUD);
                }
            } else {

            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        try {
            Bank vtpBankUD = this.getDataBankUndecodedVTP("RAW::vtp", DetectorType.UNDEFINED);
            if(vtpBankUD!=null){
                if(vtpBankUD.getRows()>0){
                    event.write(vtpBankUD);
                }
            } else {

            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        try {
            Bank scalerBankUD = this.getDataBankUndecodedSCALER("RAW::scaler", DetectorType.UNDEFINED);
            if(scalerBankUD!=null){
                if(scalerBankUD.getRows()>0){
                    event.write(scalerBankUD);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        //-----------------------------------------------------
        // CREATING BONUS BANK --------------------------------
        //-----------------------------------------------------
        try {
            //System.out.println("creating bonus bank....");
            Bank bonusBank = this.createBonusBank();
            if(bonusBank!=null){
                if(bonusBank.getRows()>0){
                    event.write(bonusBank);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return event;
    }

    public long getTriggerPhase() {
        long timestamp    = this.codaDecoder.getTimeStamp();
        int  phase_offset = 1;
        return ((timestamp%6)+phase_offset)%6; // TI derived phase correction due to TDC and FADC clock differences
    }

    public Bank createHeaderBank( int nrun, int nevent, float torus, float solenoid){

        if(schemaFactory.hasSchema("RUN::config")==false) return null;

        Bank bank = new Bank(schemaFactory.getSchema("RUN::config"), 1);

        int    localRun = this.codaDecoder.getRunNumber();
        int  localEvent = this.codaDecoder.getEventNumber();
        int   localTime = this.codaDecoder.getUnixTime();
        long  timeStamp = this.codaDecoder.getTimeStamp();
        long triggerBits = this.codaDecoder.getTriggerBits();

        if(nrun>0){
            localRun = nrun;
            localEvent = nevent;
        }

        /*
        // example of getting torus/solenoid from RCDB:
        if (Math.abs(solenoid)>10) {
            solenoid = this.detectorDecoder.getRcdbSolenoidScale();
        }
        if (Math.abs(torus)>10) {
            torus = this.detectorDecoder.getRcdbTorusScale();
        }
        */

        bank.putInt("run",        0, localRun);
        bank.putInt("event",      0, localEvent);
        bank.putInt("unixtime",   0, localTime);
        bank.putLong("trigger",   0, triggerBits);
        bank.putFloat("torus",    0, torus);
        bank.putFloat("solenoid", 0, solenoid);
        bank.putLong("timestamp", 0, timeStamp);

        return bank;
    }

    public Bank createOnlineHelicityBank() {
        if (schemaFactory.hasSchema("HEL::online")==false ||
            this.codaDecoder.getHelicityLevel3()==HelicityBit.DNE.value()) return null;
        Bank bank = new Bank(schemaFactory.getSchema("HEL::online"), 1);
        byte  helicityL3 = this.codaDecoder.getHelicityLevel3();
        IndexedTable hwpTable = this.detectorDecoder.scalerManager.
                getConstants(this.detectorDecoder.getRunNumber(),"/runcontrol/hwp");
        bank.putByte("helicityRaw",0, helicityL3);
        bank.putByte("helicity",0,(byte)(helicityL3*hwpTable.getIntValue("hwp",0,0,0)));
        return bank;
    }

    public Bank createTriggerBank(){

        if(schemaFactory.hasSchema("RUN::trigger")==false) return null;

        Bank bank = new Bank(schemaFactory.getSchema("RUN::trigger"), this.codaDecoder.getTriggerWords().size());

        for(int i=0; i<this.codaDecoder.getTriggerWords().size(); i++) {
            bank.putInt("id",      i, i+1);
            bank.putInt("trigger", i, this.codaDecoder.getTriggerWords().get(i));
        }
        return bank;
    }

    public Bank createEpicsBank(){
        if(schemaFactory.hasSchema("RAW::epics")==false) return null;
        if (this.codaDecoder.getEpicsData().isEmpty()==true) return null;
        String json = this.codaDecoder.getEpicsData().toString();
        Bank bank = new Bank(schemaFactory.getSchema("RAW::epics"), json.length());
        for (int ii=0; ii<json.length(); ii++) {
            bank.putByte("json",ii,(byte)json.charAt(ii));
        }
        return bank;
    }

    /**
     * Create the RUN::scaler and HEL::scaler banks
     * Requires:
     *   - RAW::scaler
     *   - fcup/slm/hel/dsc calibrations from CCDB
     *   - event unix time from RUN::config and run start time from RCDB,
     *     or a good clock frequency from CCDB
     * @param event
     * @return 
     */
    public List<Bank> createReconScalerBanks(Event event){
        return DaqScalers.createBanks(detectorDecoder.getRunNumber(),
                schemaFactory, event, detectorDecoder.scalerManager);
    }

    public Bank createBonusBank(){
        if(schemaFactory.hasSchema("RTPC::adc")==false) return null;
        List<DetectorDataDgtz> bonusData = this.getEntriesADC(DetectorType.RTPC);
        int totalSize = 0;
        for(int i = 0; i < bonusData.size(); i++){
            short[]  pulse = bonusData.get(i).getADCData(0).getPulseArray();
            totalSize += pulse.length;
        }
        
        Bank bonusBank = new Bank(schemaFactory.getSchema("RTPC::adc"), totalSize);
        int currentRow = 0;
        for(int i = 0; i < bonusData.size(); i++){
            
            DetectorDataDgtz bonus = bonusData.get(i);
            
            short[] pulses = bonus.getADCData(0).getPulseArray();
            long timestamp = bonus.getADCData(0).getTimeStamp();
            double    time = bonus.getADCData(0).getTime();
            double   coeff = time*120.0;
            
            double   offset1 = 0.0;
            double   offset2 =  (double) (8*(timestamp%8));
            
            for(int k = 0; k < pulses.length; k++){
                
                double pulseTime = coeff + offset1 + offset2 + k*120.0;
                
                bonusBank.putByte("sector", currentRow, (byte) bonus.getDescriptor().getSector());
                bonusBank.putByte("layer" , currentRow, (byte) bonus.getDescriptor().getLayer());
                bonusBank.putShort("component", currentRow, (short) bonus.getDescriptor().getComponent());
                bonusBank.putByte("order",      currentRow, (byte) bonus.getDescriptor().getOrder());
                bonusBank.putInt("ADC",    currentRow, pulses[k]);
                bonusBank.putFloat("time", currentRow, (float) pulseTime);
                bonusBank.putShort("ped",  currentRow, (short) 0);
                currentRow++;
            }
        }
        
        return bonusBank;
    }

    public Bank createHelicityDecoderBank(EvioDataEvent event) {
        HelicityDecoderData data = this.codaDecoder.getDataEntries_HelicityDecoder(event);
        if(data!=null) {
            Bank bank = new Bank(schemaFactory.getSchema("HEL::decoder"), 1);
            bank.putByte("helicity",        0, data.getHelicityState().getHelicity().value());
            bank.putByte("pair",            0, data.getHelicityState().getPairSync().value());
            bank.putByte("pattern",         0, data.getHelicityState().getPatternSync().value());
            bank.putByte("tSettle",         0, data.getTSettle().value());
            bank.putByte("helicityPattern", 0, data.getHelicityPattern().value());
            bank.putByte("polarity",        0, data.getPolarity());
            bank.putByte("phase",           0, data.getPatternPhaseCount());
            bank.putLong("timestamp",       0, data.getTimestamp());
            bank.putInt("helicitySeed",     0, data.getHelicitySeed());
            bank.putInt("nTStableRE",       0, data.getNTStableRisingEdge());
            bank.putInt("nTStableFE",       0, data.getNTStableFallingEdge());
            bank.putInt("nPattern",         0, data.getNPattern());
            bank.putInt("nPair",            0, data.getNPair());
            bank.putInt("tStableStart",     0, data.getTStableStart());
            bank.putInt("tStableEnd",       0, data.getTStableEnd());
            bank.putInt("tStableTime",      0, data.getTStableTime());
            bank.putInt("tSettleTime",      0, data.getTSettleTime());
            bank.putInt("patternArray",     0, data.getPatternWindows());
            bank.putInt("pairArray",        0, data.getPairWindows());
            bank.putInt("helicityArray",    0, data.getHelicityWindows());
            bank.putInt("helicityPArray",   0, data.getHelicityPatternWindows());
            return bank;
        }
        else 
            return null;
    }
    
    
    public static void main(String[] args){

        OptionParser parser = new OptionParser("decoder");

        parser.setDescription("CLAS12 Data Decoder");
        parser.addOption("-n", "-1", "maximum number of events to process");
        parser.addOption("-c", "2", "compression type (0-NONE, 1-LZ4 Fast, 2-LZ4 Best, 3-GZIP)");
        parser.addOption("-d", "0","debug mode, set >0 for more verbose output");
        parser.addOption("-m", "run","translation tables source (use -m devel for development tables)");
        parser.addOption("-b", "16","record buffer size in MB");
        parser.addRequired("-o","output.hipo");


        parser.addOption("-r", "-1","run number in the header bank (-1 means use CODA run)");
        parser.addOption("-t", "-0.5","torus current in the header bank");
        parser.addOption("-s", "0.5","solenoid current in the header bank");
        parser.addOption("-x", null,"CCDB timestamp (MM/DD/YYYY-HH:MM:SS)");
        parser.addOption("-v","default","CCDB variation");

        parser.parse(args);

        List<String> inputList = parser.getInputList();

        if(inputList.isEmpty()==true){
            parser.printUsage();
            System.out.println("\n >>>> error : no input file is specified....\n");
            System.exit(0);
        }

        String modeDevel = parser.getOption("-m").stringValue();
        boolean developmentMode = false;

        if(modeDevel.compareTo("run")!=0&&modeDevel.compareTo("devel")!=0){
            parser.printUsage();
            System.out.println("\n >>>> error : mode has to be set to \"run\" or \"devel\" ");
            System.exit(0);
        }

        if(modeDevel.compareTo("devel")==0){
            developmentMode = true;
        }

        String outputFile = parser.getOption("-o").stringValue();
        int compression = parser.getOption("-c").intValue();
        int  recordsize = parser.getOption("-b").intValue();
        int debug = parser.getOption("-d").intValue();

        CLASDecoder4 decoder = new CLASDecoder4(developmentMode);

        decoder.setDebugMode(debug);

        HipoWriterSorted writer = new HipoWriterSorted();
        writer.setCompressionType(compression);
        writer.getSchemaFactory().initFromDirectory(ClasUtilsFile.getResourceDir("CLAS12DIR", "etc/bankdefs/hipo4"));

        Bank  rawScaler   = new Bank(writer.getSchemaFactory().getSchema("RAW::scaler"));
        Bank  rawRunConf  = new Bank(writer.getSchemaFactory().getSchema("RUN::config"));
        Bank  helicityAdc = new Bank(writer.getSchemaFactory().getSchema("HEL::adc"));
        Event scalerEvent = new Event();

        int nrun = parser.getOption("-r").intValue();
        double torus = parser.getOption("-t").doubleValue();
        double solenoid = parser.getOption("-s").doubleValue();

        writer.open(outputFile);
        ProgressPrintout progress = new ProgressPrintout();
        System.out.println("INPUT LIST SIZE = " + inputList.size());
        int nevents = parser.getOption("-n").intValue();
        int counter = 0;

        if(nrun>0){
            decoder.setRunNumber(nrun,true);
        }

        if (parser.getOption("-x").getValue() != null)
            decoder.detectorDecoder.setTimestamp(parser.getOption("-x").stringValue());
        if (parser.getOption("-v").getValue() != null)
            decoder.detectorDecoder.setVariation(parser.getOption("-v").stringValue());

        // Store all helicity readings, ordered by timestamp:
        TreeSet<HelicityState> helicityReadings = new TreeSet<>();

        for(String inputFile : inputList){
            EvioSource reader = new EvioSource();
            reader.open(inputFile);
           
            while(reader.hasEvent()==true){
                EvioDataEvent event = (EvioDataEvent) reader.getNextEvent();
                
                Event  decodedEvent = decoder.getDataEvent(event);
                
                Bank   header = decoder.createHeaderBank( nrun, counter, (float) torus, (float) solenoid);
                if(header!=null) decodedEvent.write(header);
                Bank   trigger = decoder.createTriggerBank();
                if(trigger!=null) decodedEvent.write(trigger);
                Bank onlineHelicity = decoder.createOnlineHelicityBank();
                if(onlineHelicity!=null) decodedEvent.write(onlineHelicity);
                Bank decodedHelicity = decoder.createHelicityDecoderBank(event);
                if (decodedHelicity!=null) decodedEvent.write(decodedHelicity);
                
                Bank epics = decoder.createEpicsBank();
                
                decodedEvent.read(rawScaler);
                decodedEvent.read(rawRunConf);
                decodedEvent.read(helicityAdc);

                decoder.extractPulses(decodedEvent);

                helicityReadings.add(HelicityState.createFromFadcBank(helicityAdc, rawRunConf,
                    decoder.detectorDecoder.scalerManager));

                if(rawScaler.getRows()>0 || epics!=null) {
                    scalerEvent.reset();
                    
                    if(rawScaler.getRows()>0) scalerEvent.write(rawScaler);
                    if(rawRunConf.getRows()>0) scalerEvent.write(rawRunConf);

                    for (Bank b : decoder.createReconScalerBanks(decodedEvent)) {
                        decodedEvent.write(b);
                        scalerEvent.write(b);
                    }

                    if (epics!=null) {
                        decodedEvent.write(epics);
                        scalerEvent.write(epics);
                    }

                    writer.addEvent(scalerEvent, 1);
                }
                
                writer.addEvent(decodedEvent,0);
                
                counter++;
                progress.updateStatus();
                if(counter%25000==0){
                    System.gc();
                }
                if(nevents>0){
                    if(counter>=nevents) break;
                }
            }

        }

        // add the helicity flips into new tag-1 events:
        HelicitySequence.writeFlips(writer, helicityReadings);

        writer.close();
    }

}
