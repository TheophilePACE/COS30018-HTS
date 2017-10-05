/** ----------------------------------------------------------------- */
/**              Run for Broker / Retailers demonstration             */
/** ----------------------------------------------------------------- */

package brokerAgent;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

public class BrokerAgentControlller {
	public static void main(String[] args) throws StaleProxyException, InterruptedException {
		
		// Get a hold to the JADE runtime
		Runtime rt = Runtime.instance();
		
		// Launch the Main Container (with the administration GUI on top) listening on port 1099
		System.out.println(BrokerAgentControlller.class.getName() + ": Launching the platform Main Container...");
		Profile pMain = new ProfileImpl(null, 1099, null);
		pMain.setParameter(Profile.GUI, "true");
		ContainerController mainCtrl = rt.createMainContainer(pMain);
		
		// Start Retail Agents
		// Args[0] determines service type name
		// Args[1] determines service company name
		Object[] retailArgs0 = new Object[2];
		retailArgs0[0] = "Retail Agent";
		retailArgs0[1] = "AGL";
		AgentController retailCtrl0 = mainCtrl.createNewAgent("Retail Agent 0", RetailAgent.class.getName(), retailArgs0);
		retailCtrl0.start();
		
		Object[] retailArgs1 = new Object[2];
		retailArgs1[0] = "Retail Agent";
		retailArgs1[1] = "Origin";
		AgentController retailCtrl1 = mainCtrl.createNewAgent("Retail Agent 1", RetailAgent.class.getName(), retailArgs1);
		retailCtrl1.start();
		
		Object[] retailArgs2 = new Object[2];
		retailArgs2[0] = "Retail Agent";
		retailArgs2[1] = "Lumo";
		AgentController retailCtrl2 = mainCtrl.createNewAgent("Retail Agent 2", RetailAgent.class.getName(), retailArgs2);
		retailCtrl2.start();
		
		// Start Broker Agent
		AgentController brokerCtrl = mainCtrl.createNewAgent("Broker Agent", BrokerAgent.class.getName(), new Object[0]);
		brokerCtrl.start();
		
		// Start Initiator Agent
		// Args[0] determines tick rate
		// Args[1] determines name of broker agent to send to
		Object[] initiatorArgs = new Object[2];
		initiatorArgs[0] = (long)10000;
		initiatorArgs[1] = "Broker Agent";
		AgentController initiatorCtrl = mainCtrl.createNewAgent("Initiator Agent", InitiatorAgent.class.getName(), initiatorArgs);
		initiatorCtrl.start();
	}
}
