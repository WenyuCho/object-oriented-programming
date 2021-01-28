package Main;

import java.util.* ;
import java.io.* ;

public class BasicSim {
    static HashMap<String, Integer> gateOut  = new HashMap<String, Integer>() ;
    static HashMap<String, String[]> gateDef = new HashMap<String, String[]>() ;

    static ArrayList<String> inputList = new ArrayList<String>() ;
    static ArrayList<String> outputList = new ArrayList<String>() ;
    //static ArrayList<String[]> gateList = new ArrayList<String[]>() ;    
    static int[] rr;
    
    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis() ;
        String benchFile = "c:/data/c7552_UX.bench3.txt" ;
        parseBenchFile(benchFile) ; // build data structure 
        String ipFile = "c:/data/c7552_1m_ip.txt" ;
        String opFile = "c:/data/c7552_1m_op.txt" ;
        simulation(ipFile,opFile) ; // generate the result
        System.out.printf("Total time=%.3f sec(s)\n",
						(System.currentTimeMillis()-start)/1000.0) ;
    }
    // 模擬器函數
    public static void simulation(String ipFile, String opFile) throws Exception {
        
        BufferedReader br = new BufferedReader(new FileReader(ipFile)) ;
        BufferedWriter bw = new BufferedWriter(new FileWriter(opFile)) ;        
        
        String ipvs = "", opvs = "" ;
        
	   // 讀取輸入訊號檔案(xxx_ip.txt)，並進行邏輯閘運算，產生輸出。
        while ((ipvs=br.readLine())!=null) {

            // clear all gateOut
        	for (String gName : gateOut.keySet())
        		gateOut.replace(gName, 2);
        	
            // Input Data
            ipvs = ipvs.trim() ;            // 如"01010011"的輸入
            fillInput(ipvs) ;               // 將ipvs分解為0, 1, …，填入電路的輸入腳
          
            // Output Data
            opvs = "";
            int size = outputList.size();
            rr = new int[size];
            /*
            Runnable r[] = new Runnable[size];
            Thread t[] = new Thread[size] ;
            for (int i = 0 ; i < size; i++) {  // evaluate gates one by one            	//rr[0] = 0;
            	rr[0] = 0;
                String gName = outputList.get(i);
                int k = i;
                r[i] = ()-> new BasicSim().doSim(gName, k); 
                t[i] = new Thread(r[i]) ;
                t[i].start();        
                t[i].join();
            }
            for (int j = 0; j < size; j++)
            	opvs = (rr[j] == 0 ? "0": "1");
			
            */
            for (int i = 0 ; i < size; i++) {  // evaluate gates one by one
                
                String gName = outputList.get(i);
                opvs += (doSim(gName, i) == 1 ? "1": "0");     // 依照每個邏輯閘特性，進行運算
            }
			 
            bw.write(ipvs + " " + opvs) ;   // 蒐集的輸出，並寫至檔案
            bw.newLine();   
        }
        br.close(); bw.close() ;
    }

    public static void fillInput(String ipvs) {
        
        if (ipvs.length() != inputList.size()) {
            
            throw new java.lang.RuntimeException("Input Size mismatch:"+ipvs.length()+","+inputList.size()) ;
        }
                
        // set input gates value
        for (int i = 0 ; i < ipvs.length(); i++) {
            
            int q = ipvs.charAt(i)=='0' ? 0: 1;
            gateOut.replace(inputList.get(i), q) ;
        }
    }

    // ------ Gate Value Evaluation Fuctions ----------
    public static int AND(int[] gateInfo) {

        for (int i = 0; i < gateInfo.length; i++)
            if (gateInfo[i] == 0)
                return 0;
        return 1;
    }
    public static int OR(int[] gateInfo) {
        
        for (int i = 0; i < gateInfo.length; i++) 
            if (gateInfo[i] == 1)
                return 1 ;
        return 0;        
    }
    public static int XOR(int[] gateInfo) {
        
        int v1 = gateInfo[0], v2 = gateInfo[1];
        return (v1==v2)? 0: 1;
    }    
    
    public static int NAND(int[] gateInfo) { return (AND(gateInfo) == 0 ? 1: 0); }
    public static int NOR(int[] gateInfo)  { return (OR(gateInfo) == 0 ?  1: 0); }    
    public static int NXOR(int[] gateInfo) { return (XOR(gateInfo) == 0 ? 1: 0); } 
    
    // return 0 or 1
    public static int doSim(String gName, int i) {

        int v = gateOut.get(gName);
        if (v == 0 || v == 1) {
        	rr[i] = v;
            return(v);
        }
            	
        String[] p = gateDef.get(gName) ; //[gName,"nand","G224","G898"] or [gName,"not","G146"]
        
        v = doSim(p[2], i);
        switch(p[1]) {              
            case "not":                     // [null,"not","G146"]
                v = (v == 0) ? 1: 0;
                gateOut.replace(gName, v);
            	rr[i] = v;
                return(v);
                
            case "buf":                     // [null,"buf","G146"]
                gateOut.replace(gName, v);
            	rr[i] = v;
                return(v);
        }
        
        int[] q = new int[p.length-2];
        q[0] = v;
        for (int j = 1; j < q.length; j++)
            q[j] = doSim(p[2+j], i);
        
        switch(p[1]) {
            case "and":
                v = AND(q);
                break;
                
            case "or":
                v = OR(q);
                break;
                
            case "xor":
                v = XOR(q);
                break;
            
            case "nand":
                v = NAND(q);
                break;

            case "nor":
                v = NOR(q);
                break;
                
            case "nxor":
                v = NXOR(q);
                break;
                
            default:
                throw new java.lang.RuntimeException("Unknown Gate:"+p[1]) ;
        }
        
        gateOut.replace(gName, v);
    	rr[i] = v;
        return(v);
    }
    

    // ------ Parsing Circuit File and Build Data Structure ----------
    //
    // 產生 Gate 的 HashMAP
    // INPUT:  ("G953", {"","in"})
    // GATE:   ("G231", {"", "nand", "G224", "G898"})
    //         ("G206", {"", "not", "G146"})
    // OUTPUT: ["G3","G4",...]
    //
    
    public static void parseBenchFile(String benchFile) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(benchFile)) ;
        String aLine = "" ;
        String gName = "";
        
        int ni = 0, no = 0, ng = 0;
        while ((aLine=br.readLine())!=null) {
            
            if (aLine.startsWith("#")|| aLine.trim().length()==0 ) continue ; 
            
            if (aLine.startsWith("INPUT")) {                
                
                //INPUT(G953)

                String[] tt = aLine.split("\\(") ;
                gName = tt[1].replace(")","") ;
                
                gateOut.put(gName,2);       // input gate = 2 (undefine)
                inputList.add(gName);       //["G1","G2",...]
                ni++;
            }
            else if (aLine.startsWith("OUTPUT")) {          
                
                //OUTPUT(G3)

            	String[] tt = aLine.split("\\(") ;
                gName = tt[1].replace(")","") ;
            	
                outputList.add(gName);      //["G3","G4",...]
                no++;
            }
            else {                                           

                //G206 = not(G146)
                //G231 = nand(G224, G898)

                aLine = aLine.replace(" ","") ;		//G206=not(G146)
                aLine = aLine.replace("=",",") ;	//G206,not(G146)
                aLine = aLine.replace("(",",") ;	//G206,not,G146)
                aLine = aLine.replace(")","") ;     //G231,nand,G224,G898 or G206,not,G146
                
                String[] p = aLine.split(",") ;		//G231 nand G224 G898                
                gName = p[0];                
                p[1] = p[1].toLowerCase();          //to lower case
                gateDef.put(gName,p);               //("G231", ["G231", "nand", "G224", "G898"])
                                                    //("G206", ["G206", "not", "G146"])
                                                    //("G216", ["G216", "buf", "G146"])

                gateOut.put(gName,2);               //2: undefine!
                ng++;
            } 
        }
        System.out.println(ni+" Input, "+no+" Output, "+ng+" Gates" );
        br.close() ;
    }    
}