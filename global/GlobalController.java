package global;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;
import homeAgent.HomeAgentController;

public class GlobalController {
	private static String HOME_AGENT_ADDRESS = "homeAgent";
	private static long CYCLE_TIME = 20000;
	private static int PORT = 1099;
	private static String HOST = null;
	@SuppressWarnings("unused")
	private static String PATH_TO_CONFIG = null; //TODO: read the config file and generate the agents

	public static void main(String[] args) throws StaleProxyException, InterruptedException {
		// Get a hold to the JADE runtime
		Runtime rt = Runtime.instance();

		// Launch the Main Container (with the administration GUI on top) listening on port 1099
		log(": Launching the platform Main Container on port "+ PORT +" ...");
		ProfileImpl pMain = new ProfileImpl(HOST, PORT, null,true);
		pMain.setParameter(Profile.GUI, "true");
		ContainerController mainCtrl = rt.createMainContainer(pMain);

		//Launch homecontainer and agents
		HomeController homeController = new HomeController(HOME_AGENT_ADDRESS,CYCLE_TIME,rt,HOST,PORT);
		try {
			homeController.createHomeAgent(30, 10);
			homeController.createHomeAgent(30, 10); //This is an error but it should not crash, thanks to the controller
			homeController.createAppliance("Appliance1");
			homeController.createAppliance("Appliance2");
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
