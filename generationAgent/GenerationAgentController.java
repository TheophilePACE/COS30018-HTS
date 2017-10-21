/* ------------------------------------------------------------------- */
/*    Run GenerationAgentController for Generation / Responder demo    */
/* ------------------------------------------------------------------- */

package generationAgent;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

public class GenerationAgentController {
	private static String HOME_AGENT_ADDRESS = "homeAgent";
	private static long CYCLE_TIME = 10000;
	public static void main(String[] args) throws StaleProxyException, InterruptedException {
		
		// Get a hold to the JADE runtime
		Runtime rt = Runtime.instance();
		
		// Launch the Main Container (with the administration GUI on top) listening on port 1099
		System.out.println(GenerationAgentController.class.getName() + ": Launching the platform Main Container...");
		Profile pMain = new ProfileImpl(null, 1099, null);
		pMain.setParameter(Profile.GUI, "true");
		ContainerController mainCtrl = rt.createMainContainer(pMain);
		
		// Start Generation Agent
		// appArgs[0] determines energy msg tick rate of GenerationAgent
		// appArgs[1] determines energy msg receiver
		Object[] appArgs = new Object[2];
		appArgs[0] = CYCLE_TIME;
		appArgs[1] = HOME_AGENT_ADDRESS;
		AgentController generationCtrl = mainCtrl.createNewAgent("Generator", GenerationAgent.class.getName(), appArgs);
		generationCtrl.start();
	}
}