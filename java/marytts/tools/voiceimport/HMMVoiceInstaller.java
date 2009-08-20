/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import marytts.util.MaryUtils;


/**
 * Install a voice by copying the voice data to marybase/lib/voices/voicename/
 * and creating a config file marybase/conf/locale-voicename.config
 * 
 * @author Anna Hunecke, Marcela Charfuelan (modifications for creating hmm voice config file)
 *
 */
public class HMMVoiceInstaller extends VoiceImportComponent{
    
    private DatabaseLayout db;
    private String name = "HMMVoiceInstaller";
    /** HMM Voice-specific parameters, these are parameters used during models training
    if using MGC: 
             gamma=0  alpha=0.42 linear gain (default)
    if using LSP: gamma>0 
        LSP: gamma=1  alpha=0.0  linear gain/log gain 
    Mel-LSP: gamma=1  alpha=0.42 log gain
    MGC-LSP: gamma=3  alpha=0.42 log gain  */
    public final String alpha   = name+".alpha";
    public final String gamma   = name+".gamma";    
    public final String logGain = name+".logGain";

    /** Parameter beta for postfiltering  */    
    public final String beta    = name+".beta";
       
    /** Tree files and TreeSet object */
    public final String treeDurFile = name+".Ftd";
    public final String treeLf0File = name+".Ftf";
    public final String treeMcpFile = name+".Ftm";
    public final String treeStrFile = name+".Fts";
    public final String treeMagFile = name+".Fta";
        
    /** HMM pdf model files and ModelSet object */
    public final String pdfDurFile = name+".Fmd";
    public final String pdfLf0File = name+".Fmf";
    public final String pdfMcpFile = name+".Fmm";
    public final String pdfStrFile = name+".Fms";
    public final String pdfMagFile = name+".Fma";
    
    /** GV pdf files*/
    /** Global variance file, it contains one global mean vector and one global diagonal covariance vector */
    public final String useGV        = name+".useGV";
    public final String pdfLf0GVFile = name+".Fgvf"; 
    public final String pdfMcpGVFile = name+".Fgvm";  
    public final String pdfStrGVFile = name+".Fgvs";  
    public final String pdfMagGVFile = name+".Fgva";
        
    /** Variables for allowing the use of external prosody */
    public final String useExtDur      = name+".useExtDur";
    public final String useExtLogF0      = name+".useExtLogF0";
    
    /** Variables for mixed excitation */
    public final String useMixExc      = name+".useMixExc";
    public final String useFourierMag  = name+".useFourierMag";
    public final String mixFiltersFile = name+".Fif";
    public final String numFilters     = name+".in";
    public final String orderFilters   = name+".io";
    
    /** Feature list file and Vector which will contain the loaded features from this file */
    //public final String featureListFile = name+".FeaList";
       
    /** Example context feature file (TARGETFEATURES in MARY) */
    public final String featuresFile = name+".FeaFile";
    
    public final String createZipFile = name+".createZipFile";
    public final String zipCommand = name+".zipCommand";

    public String getName(){
        return name;
    }

    
    /**
     * Get the map of properties2values
     * containing the default values
     * @return map of props2values
     */
    public SortedMap<String,String> getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap<String,String>();
           
           String rootdir = db.getProp(db.ROOTDIR);
           props.put(alpha, "0.42");
           props.put(beta, "0.0");
           props.put(gamma, "0");
           props.put(logGain, "false");
           props.put(treeDurFile, "voices/qst001/ver1/tree-dur.inf"); 
           props.put(treeLf0File, "voices/qst001/ver1/tree-lf0.inf");
           props.put(treeMcpFile, "voices/qst001/ver1/tree-mgc.inf");
           props.put(treeStrFile, "voices/qst001/ver1/tree-str.inf");
           props.put(treeMagFile, "voices/qst001/ver1/tree-mag.inf");
           props.put(pdfDurFile, "voices/qst001/ver1/dur.pdf"); 
           props.put(pdfLf0File, "voices/qst001/ver1/lf0.pdf"); 
           props.put(pdfMcpFile, "voices/qst001/ver1/mgc.pdf"); 
           props.put(pdfStrFile, "voices/qst001/ver1/str.pdf");
           props.put(pdfMagFile, "voices/qst001/ver1/mag.pdf");
           props.put(useGV, "true");
           props.put(pdfLf0GVFile, "data/gv/gv-lf0-littend.pdf"); 
           props.put(pdfMcpGVFile, "data/gv/gv-mgc-littend.pdf"); 
           props.put(pdfStrGVFile, "data/gv/gv-str-littend.pdf");
           props.put(pdfMagGVFile, "data/gv/gv-mag-littend.pdf");
           props.put(useExtDur, "false");
           props.put(useExtLogF0, "false");
           props.put(useMixExc, "true");
           props.put(useFourierMag, "true");
           props.put(mixFiltersFile, "data/filters/mix_excitation_filters.txt"); 
           props.put(numFilters, "5");
           props.put(orderFilters, "48");           
           props.put(featuresFile, "phonefeatures/cmu_us_arctic_slt_a0001.pfeats");
           props.put(createZipFile, "false");
           props.put(zipCommand, "/usr/bin/zip");
           
       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap();
        props2Help.put(alpha, "Training parameter: Frequency wrapping coefficient. 0.42 for mel frequency.");
        props2Help.put(beta, "Postfiltering coefficient, -0.8 - 0.8");
        props2Help.put(gamma, "Training parameter: gamma=0 for MGC, gamma>0 for LSP");
        props2Help.put(logGain, "Training parameter: use log gain / linear gain, default for MGC logGain=false");
        props2Help.put(treeDurFile, "durations tree file"); 
        props2Help.put(treeLf0File, "log F0 tree file");
        props2Help.put(treeMcpFile, "Mel-cepstral (mcp or Mel-generalized cepstral mgc, HTS Version 2.0.1 used mgc) tree file");
        props2Help.put(treeStrFile, "Bandpass voicing strengths tree file (optional: used for mixed excitation)");
        props2Help.put(treeMagFile, "Fourier Magnitudes tree (optional: used for mixed excitation)");
        props2Help.put(pdfDurFile, "Duration means and variances PDF file"); 
        props2Help.put(pdfLf0File, "Log F0 means and variances PDF file"); 
        props2Help.put(pdfMcpFile, "Mel-cepstral (or Mel-generalized cepstral mgc) means and variances PDF file"); 
        props2Help.put(pdfStrFile, "Bandpass voicing strengths means and variances PDF file (optional: used for mixed excitation)");
        props2Help.put(pdfMagFile, "Fourier Magnitudes means and variances PDF file (optional: used for mixed excitation)");
        props2Help.put(useGV, "Use global variance in parameter generation (true/false)");
        props2Help.put(pdfLf0GVFile, "Global variance for Log F0, mean and (diagonal) variance PDF file"); 
        props2Help.put(pdfMcpGVFile, "Global variance for Mel-cepstral (or Mel-generalized cepstral mgc) mean and (diagonal) variance PDF file"); 
        props2Help.put(pdfStrGVFile, "Global variance for Bandpass voicing strengths mean and (diagonal) variance PDF file (optional: used for mixed excitation)");
        props2Help.put(pdfMagGVFile, "Global variance for Fourier Magnitudes mean and (diagonal) variance PDF file (optional: used for mixed excitation)");
        props2Help.put(useExtDur, "Use external prosody: use external duration (true/false), it will use the unit_duration targetfeature generated by Mary.");
        props2Help.put(useExtLogF0, "Use external prosody: use external logF0 (true/false), it will use the unit_logf0 and unit_logf0delta targetfeatures generated by Mary.");
        props2Help.put(useMixExc, "Use mixed excitation in speech generation (true/false)");
        props2Help.put(useFourierMag, "Use Fourier magnitudes for pulse generation (true/false)");
        props2Help.put(mixFiltersFile, "Filter taps of bandpass filters for mixed excitation (optional: used for mixed excitation)"); 
        props2Help.put(numFilters, "Number of filters in bandpass bank, default 5 filters (optional: used for mixed excitation)");
        props2Help.put(orderFilters, "Number of taps in bandpass filters, default 48 taps (optional: used for mixed excitation)");
        props2Help.put(featuresFile, "File for testing the HMMSynthesiser, example of a file in HTSCONTEXT format. If the file is not provided or does not exist a file from data/labels/gen/ will be used.");
        props2Help.put(createZipFile, "Create zip file for Mary voices installation (used by Mary voices administrator only).");
        props2Help.put(zipCommand, "zip command to create a voice.zip file for voice installation.");
        
    }

    
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
        System.out.println("Installing hmm voice: ");
        /* make a new directory for the voice */
        System.out.println("Making voice directory ... ");
        String fileSeparator = System.getProperty("file.separator");
        String filedir = db.getProp(db.FILEDIR);
        String configdir = db.getProp(db.CONFIGDIR);
        String maryBase = db.getProp(db.MARYBASE);
        String rootDir = db.getProp(db.ROOTDIR);
        if (!maryBase.endsWith(fileSeparator)) maryBase = maryBase + fileSeparator;
        String newVoiceDir = maryBase
        					+"lib"+fileSeparator
        					+"voices"+fileSeparator
        					+db.getProp(db.VOICENAME).toLowerCase()
        					+fileSeparator;
        
        System.out.println(" newVoiceDir = " +  newVoiceDir);
        File newVoiceDirFile = new File(newVoiceDir);
        if (!newVoiceDirFile.exists()) newVoiceDirFile.mkdir();
        
        /* copy the files */
        System.out.println("Copying files ... ");
        try{
            File in, out;
            in = new File(rootDir + getProp(treeDurFile));
            out = new File(newVoiceDir + getFileName(getProp(treeDurFile)));
            copy(in,out);   
            in = new File(rootDir + getProp(treeLf0File));
            out = new File(newVoiceDir + getFileName(getProp(treeLf0File)));
            copy(in,out);   
            in = new File(rootDir + getProp(treeMcpFile));
            out = new File(newVoiceDir + getFileName(getProp(treeMcpFile)));
            copy(in,out);
            
            /* optional file for mixed excitation */
            in = new File(rootDir + getProp(treeStrFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(treeStrFile)));
              copy(in,out);    
            }
            /* optional file for using Fourier magnitudes in pulse generation */
            in = new File(rootDir + getProp(treeMagFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(treeMagFile)));
              copy(in,out);   
            }
            
            in = new File(rootDir + getProp(pdfDurFile));
            out = new File(newVoiceDir + getFileName(getProp(pdfDurFile)));
            copy(in,out);   
            in = new File(rootDir + getProp(pdfLf0File));
            out = new File(newVoiceDir + getFileName(getProp(pdfLf0File)));
            copy(in,out);   
            in = new File(rootDir + getProp(pdfMcpFile));
            out = new File(newVoiceDir + getFileName(getProp(pdfMcpFile)));
            copy(in,out);   
            
            /* optional file for using Fourier magnitudes in pulse generation */
            in = new File(rootDir + getProp(pdfStrFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(pdfStrFile)));
              copy(in,out);
            }
            /* optional file for mixed excitation */
            in = new File(rootDir + getProp(pdfMagFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(pdfMagFile)));
              copy(in,out);   
            }
            
            /* global variance files */
            in = new File(rootDir + getProp(pdfMcpGVFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(pdfMcpGVFile)));
              copy(in,out);   
            }
            in = new File(rootDir + getProp(pdfLf0GVFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(pdfLf0GVFile)));
              copy(in,out);   
            }
            in = new File(rootDir + getProp(pdfStrGVFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(pdfStrGVFile)));
              copy(in,out);   
            }
            in = new File(rootDir + getProp(pdfMagGVFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(pdfMagGVFile)));
              copy(in,out);   
            }
            
            in = new File(rootDir + getProp(mixFiltersFile));
            out = new File(newVoiceDir + getFileName(getProp(mixFiltersFile)));
            copy(in,out);
            
            
            in = new File(rootDir + getProp(featuresFile));
            if(in.exists()){
              out = new File(newVoiceDir + getFileName(getProp(featuresFile)));
              copy(in,out); 
            } else {
              /* copy one example of MARY context features file, it can be one of the 
               * files used for testing in phonefeatures/gen/*.pfeats*/
              File dirPhonefeatures  = new File(rootDir + "phonefeatures/gen");
              if( dirPhonefeatures.exists() && dirPhonefeatures.list().length > 0 ){ 
                String[] feaFiles = dirPhonefeatures.list();
                in = new File(rootDir + "phonefeatures/gen/"+feaFiles[0]);
                out = new File(newVoiceDir + getFileName(feaFiles[0]));
                copy(in,out);
                props.put(featuresFile, rootDir+"phonefeatures/gen/"+feaFiles[0]);
              } else{
                System.out.println("Problem copying one example of context features, the directory phonefeatures/gen/ is empty or directory does not exist.");
                throw new IOException();
              }
            }   
               
        }catch (IOException ioe){
            return false;
        }
        

        // Normalise locale: (e.g., if user set en-US, change it to en_US)
        String locale = MaryUtils.string2locale(db.getProp(db.LOCALE)).toString();
        
        String configFileName = maryBase
        					+"conf"+fileSeparator
        					+locale
        					+"-"+db.getProp(db.VOICENAME).toLowerCase()
        					+".config";
        System.out.println("\nCreating config file: " + configFileName);
        createConfigFile(configFileName, locale);
        System.out.println("... done! ");        
        System.out.println("To run the voice, restart your Mary server");
        
        /* create a zip file for installation */
        if( getProp(createZipFile).contentEquals("true") ) {
          System.out.println("\nCreating voice installation file: " + db.getProp(db.MARYBASE)+locale 
                               + "-"+db.getProp(db.VOICENAME).toLowerCase() + ".zip\n");  
          String maryBaseForShell = maryBase.replaceAll(" ", Pattern.quote("\\ "));
          String installZipFile = locale
                               + "-"+db.getProp(db.VOICENAME).toLowerCase()
                               + ".zip";
          configFileName = "conf"+fileSeparator
                         + locale
                         + "-"+db.getProp(db.VOICENAME).toLowerCase()
                         + ".config";
          String cmdLine = "cd "+ maryBaseForShell + "\n" + getProp(zipCommand) + " " 
                         + installZipFile + " " 
                         + configFileName + " "
                         + "lib/voices/" + db.getProp(db.VOICENAME).toLowerCase() + fileSeparator + "*";  
          System.out.println("CommandLine:" + cmdLine);
          launchBatchProc(cmdLine, "zip", filedir);
          System.out.println();
        }
        
        return true;
        }
 
    private void copy(File source, File dest)throws IOException{
        try { 
            System.out.println("copying: " + source + "\n    --> " + dest);
            FileChannel in = new FileInputStream(source).getChannel();
            FileChannel out = new FileOutputStream(dest).getChannel();   
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());            
            out.write(buf);
            in.close();
            out.close();
        } catch (Exception e){
            System.out.println("Error copying file "
                    +source.getAbsolutePath()+" to "+dest.getAbsolutePath()
                    +" : "+e.getMessage());
            throw new IOException();
        }
    }
    
    
    private void createExampleText(File exampleTextFile) throws IOException{
        try{
            //just take the first three transcript files as example text
            PrintWriter exampleTextOut =
                new PrintWriter(
                        new FileWriter(exampleTextFile),true);
            for (int i=0;i<3;i++){
                String basename = bnl.getName(i);
                BufferedReader transIn = 
                    new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(
                                            new File(db.getProp(db.TEXTDIR)
                                                    +basename+db.getProp(db.TEXTEXT)))));
                String text = transIn.readLine();
                transIn.close();            
                exampleTextOut.println(text);
            }
            exampleTextOut.close();
            
        } catch (Exception e){
            System.out.println("Error creating example text file "
                    +exampleTextFile.getName());
            throw new IOException();
        }
        
    }
    
    
    private void createConfigFile(String filename, 
            					String locale){
        try{
            PrintWriter configOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
                                        new File(filename)),"UTF-8"),true);
            File in;
            String rootDir = db.getProp(db.ROOTDIR);
            String voicename = db.getProp(db.VOICENAME).toLowerCase();
            //print the header
            configOut.println("#Auto-generated config file for voice "+voicename+"\n");
            //print name and version info
             configOut.println("name = " + voicename);
             
             configOut.println(locale+"-voice.version = "+db.getProp(db.MARYBASEVERSION)+"\n");
             configOut.println("voice.version = "+db.getProp(db.MARYBASEVERSION)+"\n");
             
             //print providing info
             configOut.println("# Declare \"group names\" as component that other components can require.\n"+
                     	"# These correspond to abstract \"groups\" of which this component is an instance.\n"+
                     	"provides = \\\n         "+locale+"-voice \\\n" + "         hmm-voice\n");             
             configOut.println("# List the dependencies, as a whitespace-separated list.\n"+
                     "# For each required component, an optional minimum version and an optional\n"+
                     "# download url can be given.\n"+
                     "# We can require a component by name or by an abstract \"group name\"\n"+ 
                     "# as listed under the \"provides\" element.\n"+
             		 "requires = \\\n   "+locale+" \\\n   marybase \\");
             configOut.println("   hmm \n\n");
             configOut.println("requires.marybase.version = 4.0.0\n"+
             		 "requires."+locale+".version = 4.0.0\n"+
             		 "requires."+locale+".download = http://mary.dfki.de/download/mary-install-4.x.x.jar\n"+
                     "requires.hmm.version = 4.0.0\n");
                
             //now follow the module settings
              configOut.println("####################################################################\n"+
                      "####################### Module settings  ###########################\n"+
                      "####################################################################\n"+
                      "# For keys ending in \".list\", values will be appended across config files,\n"+
                      "# so that .list keys can occur in several config files.\n"+
                      "# For all other keys, values will be copied to the global config, so\n"+
              		  "# keys should be unique across config files.\n");              
              configOut.println("hmm.voices.list = \\\n   " + voicename + "\n");
              
              
              String voiceHeader = "voice."+voicename;
              
              //wants-to-be-default value
              configOut.println("# If this setting is not present, a default value of 0 is assumed.\n"+
                      voiceHeader+".wants.to.be.default = 0\n");
      
              //properties of the voice
              configOut.println("# Set your voice specifications\n"+
                      voiceHeader+".gender = "+db.getProp(db.GENDER).toLowerCase()+"\n"+
                      voiceHeader+".locale = "+ locale +"\n"+
                      voiceHeader+".domain = "+db.getProp(db.DOMAIN).toLowerCase()+"\n"+
                      voiceHeader+".samplingRate = "+db.getProp(db.SAMPLINGRATE)+"\n");
              
                     
              //voice data
              configOut.println("# HMM Voice-specific parameters \n" +
                    "# parameters used during models training \n" +
                    "# MGC: stage=gamma=0 alpha=0.42 linear gain (default) \n" +
                    "# LSP: gamma>0  \n" +
                    "#          LSP: gamma=1 alpha=0.0  linear gain/log gain \n" +
                    "#      Mel-LSP: gamma=1 alpha=0.42 log gain \n" +
                    "#      MGC-LSP: gamma=3 alpha=0.42 log gain \n" +
                    voiceHeader+".alpha = " + getProp(alpha) + "\n" +
                    voiceHeader+".gamma = " + getProp(gamma) + "\n" +
                    voiceHeader+".logGain = " + getProp(logGain) + "\n");
              
              configOut.println("# Parameter beta for postfiltering \n" +
                      voiceHeader+".beta = " + getProp(beta) + "\n"); 
              
              configOut.println("# HMM Voice-specific files\n# Information about trees\n"+
                      voiceHeader+".Ftd = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(treeDurFile))+"\n"+
                      voiceHeader+".Ftf = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(treeLf0File))+"\n"+
                      voiceHeader+".Ftm = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(treeMcpFile)));
              if( new File(rootDir + getProp(treeStrFile)).exists())
                configOut.println(voiceHeader+".Fts = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(treeStrFile)));
              if( new File(rootDir + getProp(treeMagFile)).exists())
                configOut.println(voiceHeader+".Fta = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(treeMagFile)));
              configOut.println("\n# Information about means and variances PDFs \n"+
                      voiceHeader+".Fmd = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfDurFile))+"\n"+
                      voiceHeader+".Fmf = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfLf0File))+"\n"+
                      voiceHeader+".Fmm = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfMcpFile)));
              if( new File(rootDir + getProp(pdfStrFile)).exists())
               configOut.println(voiceHeader+".Fms = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfStrFile)));
              if( new File(rootDir + getProp(pdfMagFile)).exists())
               configOut.println(voiceHeader+".Fma = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfMagFile)));
              
              configOut.println("\n# Information about Global Mean and Variance PDFs \n" +
                    "# By default GV is not used for generating strengths and Fourier magnitudes,\n" +
                    "# although the gv pdf for these are generated during training. \n" +
                    "# Uncomment the lines corresponding to gv-str and gv-mag for using them.");
              configOut.println(voiceHeader+".useGV = "+ getProp(useGV));
              if( new File(rootDir + getProp(pdfLf0GVFile)).exists())
                  configOut.println(voiceHeader+".Fgvf = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfLf0GVFile)));
              if( new File(rootDir + getProp(pdfMcpGVFile)).exists())
                  configOut.println(voiceHeader+".Fgvm = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfMcpGVFile)));
              if( new File(rootDir + getProp(pdfStrGVFile)).exists())
                  configOut.println("#" + voiceHeader+".Fgvs = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfStrGVFile)));
              if( new File(rootDir + getProp(pdfMagGVFile)).exists())
                  configOut.println("#" + voiceHeader+".Fgva = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfMagGVFile)));
              
              configOut.println("\n# File for testing the HMMSynthesiser, a context features file example.\n" +
                      voiceHeader+".FeaFile = MARY_BASE/lib/voices/"+voicename+"/"+getFileName(getProp(featuresFile)));
              
              configOut.println("\n# Information about Mixed Excitation");
              configOut.println(voiceHeader+".useMixExc = "+ getProp(useMixExc));
              configOut.println(voiceHeader+".useFourierMag = "+ getProp(useFourierMag));
              configOut.println();
              
              configOut.println("# Information for using external prosody, " +
                    "if set to true it will use the MARY targetfeatures: \n" +
                    "# ContinuousFeatureProcessors\n" +
                    "#  unit_duration float \n" +
                    "#  unit_logf0 float \n" +
                    "#  unit_logf0delta float");
              configOut.println(voiceHeader+".useExtDur = "+ getProp(useExtDur));
              configOut.println(voiceHeader+".useExtLogF0 = "+ getProp(useExtLogF0));
              configOut.println();
              
              if( new File(rootDir + getProp(treeStrFile)).exists()) {
                configOut.println("# Filter taps of bandpass filters for mixed excitation \n" +
                                "# File format: for example if we have 5 filters each with 48 taps \n" +
                                "# then the taps are in a vector \n" +
                                "# tap[1][1] \n" +
                                "# ... \n" +
                                "# tap[1][48] \n" +
                                "# tap[2][1] \n" +
                                "# ... \n" +
                                "# tap[2][48] \n" +
                                "# ... \n" +
                                "# tap[5][1] \n" +
                                "# ... \n" +
                                "# tap[5][48] \n" +
                                voiceHeader+".Fif = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(mixFiltersFile))+"\n"+
                                "# Number of filters in bandpass bank, default 5 filters \n" +
                                voiceHeader+".in = " + getProp(numFilters)+"\n" +
                                "# Number of taps in bandpass filters, default 48 taps \n" +
                                voiceHeader+".io = " + getProp(orderFilters) );
              }
                     
              
        } catch (Exception e){
            throw new Error("Error writing config file : "
                    +e.getMessage());
        }
    }
    
    
    /**
     * Given a file name with path it return the file name
     * @param fileNameWithPath
     * @return
     */
    private String getFileName(String fileNameWithPath) {
       String str;
       int i;
       
       i = fileNameWithPath.lastIndexOf("/");
       str = fileNameWithPath.substring(i+1); 
       
       return str;
        
    }
    
    /**
     * A general process launcher for the various tasks but using an intermediate batch file
     * (copied from ESTCaller.java)
     * @param cmdLine the command line to be launched.
     * @param task a task tag for error messages, such as "Pitchmarks" or "LPC".
     * @param the basename of the file currently processed, for error messages.
     */
    private void launchBatchProc( String cmdLine, String task, String baseName ) {
        
        Process proc = null;
        Process proctmp = null;
        BufferedReader procStdout = null;
        String line = null;
        String filedir = db.getProp(db.ROOTDIR);
        String tmpFile = filedir+"tmp.bat";

        // String[] cmd = null; // Java 5.0 compliant code
        
        try {
            FileWriter tmp = new FileWriter(tmpFile);
            tmp.write(cmdLine);
            tmp.close();
            
            /* make it executable... */
            proctmp = Runtime.getRuntime().exec( "chmod +x "+tmpFile );
            
            /* Java 5.0 compliant code below. */
            /* Hook the command line to the process builder: */
            /* cmd = cmdLine.split( " " );
            pb.command( cmd ); /*
            /* Launch the process: */
            /*proc = pb.start(); */
            
            /* Java 1.0 equivalent: */
            proc = Runtime.getRuntime().exec( tmpFile );
            
            /* Collect stdout and send it to System.out: */
            procStdout = new BufferedReader( new InputStreamReader( proc.getInputStream() ) );
            while( true ) {
                line = procStdout.readLine();
                if ( line == null ) break;
                System.out.println( line );
            }
            /* Wait and check the exit value */
            proc.waitFor();
            if ( proc.exitValue() != 0 ) {
                throw new RuntimeException( task + " computation failed on file [" + baseName + "]!\n"
                        + "Command line was: [" + cmdLine + "]." );
            }
            
            
        }
        catch ( IOException e ) {
            throw new RuntimeException( task + " computation provoked an IOException on file [" + baseName + "].", e );
        }
        catch ( InterruptedException e ) {
            throw new RuntimeException( task + " computation interrupted on file [" + baseName + "].", e );
        }
        
    }    
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress(){
        return -1;
    }
    
}
