package retailAgent;

import jade.core.Runtime;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

public class RetailAgentController {
	//List of the args
	private static int PORT = 1099;
	private static String HOST = null;
	public static void main(String[] args) throws StaleProxyException, InterruptedException {
		
		// Get a hold to the JADE runtime
		Runtime rt = Runtime.instance();
		
		// Launch the Main Container (with the administration GUI on top) listening on port 1099
		System.out.println(RetailAgentController.class.getName() + ": Launching the platform Main Container on port "
		+ PORT +" ...");
		Profile pMain = new ProfileImpl(HOST, 1099, null);
		pMain.setParameter(Profile.GUI, "true");
		ContainerController mainCtrl = rt.createMainContainer(pMain);
		
		// Start reatilerAgents
		Object[] retailer1args = new Object[2];
		retailer1args[0] = "Electricity";
		retailer1args[1] = "Company1";
		AgentController retailer1 = mainCtrl.createNewAgent("Retailer1", RetailAgent.class.getName(), retailer1args);
		retailer1.start();
	}
}
