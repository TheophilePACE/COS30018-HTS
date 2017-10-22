package global;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

import homeAgent.HomeAgentController;
import brokerAgent.BrokerAgentController;

public class GlobalController {
	private static String HOME_AGENT_ADDRESS = "homeAgent";
	private static long CYCLE_TIME = 10000;
	private static int PORT = 1099;
	private static String HOST = null;
	private static String BROKER_ADRESS = "brokerAgent";
	private static String TRANSMISSION_AGENT_ADDRESS = "transmissionAgent";
	@SuppressWarnings("unused")
	private static String PATH_TO_CONFIG = null; //TODO: read the config file and generate the agents

	public static void main(String[] args) throws StaleProxyException, InterruptedException {
		// Get a hold to the JADE runtime
		Runtime rt = Runtime.instance();

		// Launch the Main Container (with the administration GUI on top) listening on port 1099
		log(": Launching the platform Main Container on port "+ PORT +" ...");
		ProfileImpl pMain = new ProfileImpl(HOST, PORT, null,true);
		pMain.setParameter(Profile.GUI, "true");
		@SuppressWarnings("unused")
		ContainerController mainCtrl = rt.createMainContainer(pMain);

		//Launch homecontainer and agents
		HomeController homeController = new HomeController(HOME_AGENT_ADDRESS,CYCLE_TIME,rt,HOST,PORT,TRANSMISSION_AGENT_ADDRESS,BROKER_ADRESS);
		try {
			homeController.createTransmissionAgent();
			homeController.createHomeAgent(30, 10);
			homeController.createHomeAgent(30, 10); //This is an error but it should not crash, thanks to the controller
			homeController.createAppliance("applianceAgent1");
			homeController.createAppliance("applianceAgent2");
			homeController.createGeneration("generationAgent1");
		} catch (Exception e) {
			log(e.toString());
		}
		
		//Launch a broker and retailers
		BrokerAgentController brokerAgentController = new BrokerAgentController(CYCLE_TIME, rt, HOST, PORT, BROKER_ADRESS);
		brokerAgentController.createRetailerAgent("retailAgent1", "Retail Agent", "GloBird Energy","20.8");
		brokerAgentController.createRetailerAgent("retailAgent2", "Retail Agent", "Origin","23.56");
		brokerAgentController.createRetailerAgent("retailAgent3", "Retail Agent", "Pacific Hydro","16.1");
		brokerAgentController.createBrokerAgent();
//		brokerAgentController.createInitiatorAgent("Initiator Agent");
	}
	private static String log(String s) {
		String toPrint = "[" + HomeAgentController.class.getName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
}
