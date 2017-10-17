/* ----------------------------------------------------------------- */
/*          Run for Generator / Responder demonstration              */
/* ----------------------------------------------------------------- */

package generationAgent;

import jade.core.Runtime;
import applianceAgent.ApplianceAgent;
import applianceAgent.ApplianceResponderAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

public class GenerationAgentController {
	public static void main(String[] args) throws StaleProxyException, InterruptedException {
		
		// Get a hold to the JADE runtime
		Runtime rt = Runtime.instance();
		
		// Launch the Main Container (with the administration GUI on top) listening on port 1099
		System.out.println(GenerationAgentController.class.getName() + ": Launching the platform Main Container...");
		Profile pMain = new ProfileImpl(null, 1099, null);
		pMain.setParameter(Profile.GUI, "true");
		ContainerController mainCtrl = rt.createMainContainer(pMain);
		
		// Start GenerationResponder agent
		AgentController responderCtrl = mainCtrl.createNewAgent("Responder", GenerationResponderAgent.class.getName(), new Object[0]);
		responderCtrl.start();
		
		// Start Generation Agent
		// appArgs[0] determines energy msg tick rate of GenerationAgent
		// appArgs[1] determines energy msg receiver
		Object[] genArgs = new Object[2];
		genArgs[0] = (long)10000;
		genArgs[1] = "Responder";
		AgentController applianceCtrl = mainCtrl.createNewAgent("Generator", GenerationAgent.class.getName(), genArgs);
		applianceCtrl.start();
	}
}
