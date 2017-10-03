package homeAgent;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

public class HomeAgentController {
	private static long CYCLE_TIME = 10000;

	private static int PORT = 1099;
	private static String HOST = null;
	@SuppressWarnings("unused")
	private static String PATH_TO_CONFIG = null; //TODO: read the config file and generate the agents

	public static void main(String[] args) throws StaleProxyException, InterruptedException {
		// Get a hold to the JADE runtime
		Runtime rt = Runtime.instance();

		// Launch the Main Container (with the administration GUI on top) listening on port 1099
		log(": Launching the platform Main Container on port "+ PORT +" ...");
		Profile pMain = new ProfileImpl(HOST, PORT, null);
		pMain.setParameter(Profile.GUI, "true");
		ContainerController mainCtrl = rt.createMainContainer(pMain);
		try {
			Object[] homeArgs = new Object[3];
			homeArgs[0] = 25; //maxBuyPrice
			homeArgs[1] = 20; //minSellPrice
			homeArgs[2] = CYCLE_TIME;
			log("Creating agent Home with args " + homeArgs);
			mainCtrl.createNewAgent("homeAgent", HomeAgent.class.getName(), homeArgs).start();
			log("Started HomeAgent");
		} catch (Exception e) {
			log(e.toString());
			
		}
		

	}
	private static String log(String s) {
		String toPrint = "[" + HomeAgentController.class.getName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
}
