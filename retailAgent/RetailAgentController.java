package retailAgent;

import jade.core.Runtime;

import java.util.HashMap;
import java.util.Map;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

public class RetailAgentController {

	//List of the args
	private static int PORT = 1099;
	private static String HOST = null;
	private static String PATH_TO_CONFIG = null; //TODO: read the config file and generate the agents

	public static void main(String[] args) throws StaleProxyException, InterruptedException {
		
		// Get a hold to the JADE runtime
		Runtime rt = Runtime.instance();
		
		// Launch the Main Container (with the administration GUI on top) listening on port 1099
		System.out.println(RetailAgentController.class.getName() 
				+ ": Launching the platform Main Container on port "+ PORT +" ...");
		Profile pMain = new ProfileImpl(HOST, PORT, null);
		pMain.setParameter(Profile.GUI, "true");
		ContainerController mainCtrl = rt.createMainContainer(pMain);
		
		// parse the file, then launch the agent according to the config
		Map<String, Object[]> retailers = getRetailersList(); //file parsing
		
		retailers.forEach((k,v) -> {
			try {
				log("Creating agent " + k + " with args " + v[0] + " ; "+v[1]);
				mainCtrl.createNewAgent(k, RetailAgent.class.getName(), v).start();
				log("Started retailer : " + k);
			} catch (Exception e) {
				log(e.toString());
				
			}
		});
	}
	
	private static Map<String, Object[]> getRetailersList() {
		Map<String, Object[]> retailersList = new HashMap<String, Object[]>();
		
		//TODO: willm be replaced by the parsing of a config file
		if(PATH_TO_CONFIG == null) {
			for(int i =0; i< 5; i ++) { 
				Object[] retailerargs = new Object[2];
				retailerargs[0] = "Electricity";
				retailerargs[1] = "Company" + i;
				retailersList.put("Retailer" + i, retailerargs);
			}
		}
		return retailersList;
	}
	
	private static String log(String s) {
		String toPrint = "[" + RetailAgentController.class.getName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
	
}
