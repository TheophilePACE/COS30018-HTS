/** ----------------------------------------------------------------- */
/**      BrokerAgentController							              */
/** ----------------------------------------------------------------- */

package brokerAgent;

import jade.core.Runtime;
import homeAgent.HomeController;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;
import retailAgent.RetailAgent;

public class BrokerAgentController {
	private ContainerController retailersContainer;
	private AgentController brokerAgent;
	private String API_URL;
	
	public BrokerAgentController(Runtime rt, String host, int port, String apiUrl) {
		super();
		Profile pRetailerContainer = new ProfileImpl(host,port,null,false); //create a non-main container
		pRetailerContainer.setParameter(Profile.CONTAINER_NAME,"retailersContainer");
		retailersContainer = rt.createAgentContainer(pRetailerContainer);
		log("The retailer container was created!");
		brokerAgent =null;
		API_URL = apiUrl;
	}
	
	//pure function. Private, usable through createBrokerAGent or the main
	private AgentController makeCreateBrokerAgent(ContainerController cc, String apiUrl) {
			try {
				// Start Broker Agent
				Object[] args = new Object[1];
				args[0] = apiUrl;
				AgentController newAgent = cc.createNewAgent("brokerAgent", BrokerAgent.class.getName(), args);
				newAgent.start();
				return newAgent;
			} catch (Exception e) {
				log(e.toString());
				return null;
			}	
	}
	//Usable from another class.
	public AgentController createBrokerAgent() {
		if(brokerAgent == null) {
			log("Creating agent broker ");
			brokerAgent= makeCreateBrokerAgent(retailersContainer, API_URL);
		} else {
			log("HomeAgent already created");
		}
		return brokerAgent;
	}
	
	private AgentController makeCreateRetailerAgent(String name,String service,String companyName, String initialOffer, ContainerController cc) {
		// Start Retail Agents
		// Args[0] determines service type name
		// Args[1] determines service company name
		// Args[2] determines service initial offer price
		Object[] retailArgs0 = new Object[3];
		retailArgs0[0] = service;
		retailArgs0[1] = companyName;
		retailArgs0[2] = initialOffer;
		AgentController retailCtrl0;
		try {
			retailCtrl0 = cc.createNewAgent(name, RetailAgent.class.getName(), retailArgs0);
			retailCtrl0.start();
			return retailCtrl0;	
		} catch (StaleProxyException e) {
			e.printStackTrace();
			return null;
		}
	}

	public AgentController createRetailerAgent(String agentName, String service,String companyName, String initialOffer) {
		return makeCreateRetailerAgent(agentName, service, companyName, initialOffer, retailersContainer);
	}
	public static void main(String[] args) throws StaleProxyException, InterruptedException {
		
		// Get a hold to the JADE runtime
		Runtime rt = Runtime.instance();
		
		// Launch the Main Container (with the administration GUI on top) listening on port 1099
		log(": Launching the platform Main Container...");
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
	}
	private static String log(String s) {
		String toPrint = "[" + HomeController.class.getName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
}
