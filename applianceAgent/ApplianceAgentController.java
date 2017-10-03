/* ----------------------------------------------------------------- */
/*    Run ApplianceAgentController for Appliance / Responder demo    */
/* ----------------------------------------------------------------- */

package applianceAgent;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

public class ApplianceAgentController {
	private static String HOME_AGENT_ADDRESS = "homeAgent";
	private static long CYCLE_TIME = 10000;
	public static void main(String[] args) throws StaleProxyException, InterruptedException {
		
		// Get a hold to the JADE runtime
		Runtime rt = Runtime.instance();
		
		// Launch the Main Container (with the administration GUI on top) listening on port 1099
		System.out.println(ApplianceAgentController.class.getName() + ": Launching the platform Main Container...");
		Profile pMain = new ProfileImpl(null, 1099, null);
		pMain.setParameter(Profile.GUI, "true");
		ContainerController mainCtrl = rt.createMainContainer(pMain);
		
		// Start Appliance Agent
		// appArgs[0] determines energy msg tick rate of ApplianceAgent
		// appArgs[1] determines energy msg receiver
		Object[] appArgs = new Object[2];
		appArgs[0] = CYCLE_TIME;
		appArgs[1] = HOME_AGENT_ADDRESS;
		AgentController applianceCtrl = mainCtrl.createNewAgent("Appliance", ApplianceAgent.class.getName(), appArgs);
		applianceCtrl.start();
	}
}