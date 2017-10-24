/** ----------------------------------------------------------------- */
/**    HomeController											      */
/** ----------------------------------------------------------------- */

package homeAgent;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

import applianceAgent.ApplianceAgent;
import generationAgent.GenerationAgent;

public class HomeController {
	private String HOME_AGENT_ADDRESS;
	private long CYCLE_TIME;
	private ContainerController homeContainer;
	private AgentController homeAgent;
	private String BROKER_AGENT_ADDRESS;

	public HomeController(String homeAgentAddress, long cycleTime, Runtime rt, String host, int port, String brokerAgentAddress) {
		super();
		HOME_AGENT_ADDRESS =  homeAgentAddress;
		CYCLE_TIME = cycleTime;
		BROKER_AGENT_ADDRESS = brokerAgentAddress;
		Profile pHomeContainer = new ProfileImpl(host,port,null,false); //create a non-main container
		pHomeContainer.setParameter(Profile.CONTAINER_NAME, "homeContainer");
		homeContainer = rt.createAgentContainer(pHomeContainer);
		log("The homeContainer was created!");
		homeAgent =null;
	} 
	private AgentController makeCreateAppliance(String agentName, String homeAgentAddress, ContainerController containerController, String serviceType, String serviceName, String API_URL, String consumptionType) {
		Object[] appArgs = new Object[5];
		appArgs[0] = homeAgentAddress;
		appArgs[1] = serviceType;
		appArgs[2] = serviceName;
		appArgs[3] = API_URL;
		appArgs[4] = consumptionType;
		try {
			AgentController applianceCtrl = containerController.createNewAgent(agentName, ApplianceAgent.class.getName(), appArgs);
			applianceCtrl.start();
			return applianceCtrl; 
		} catch (Exception e) {
			log(e.toString());
			return null;
		}
	}
	public AgentController createAppliance(String agentName, String API_URL, String consumptionType) {
		return makeCreateAppliance(agentName, HOME_AGENT_ADDRESS, homeContainer, "Appliance", agentName, API_URL,consumptionType);
	}

	private AgentController makeCreateGeneration(String agentName, String homeAgentAddress, ContainerController containerController, String serviceType, String serviceName, String API_URL) {
		Object[] appArgs = new Object[4];
		appArgs[0] = homeAgentAddress;
		appArgs[1] = serviceType;
		appArgs[2] = serviceName;
		appArgs[3] = API_URL;
		try {
			AgentController generationCtrl = containerController.createNewAgent(agentName, GenerationAgent.class.getName(), appArgs);
			generationCtrl.start();
			return generationCtrl; 
		} catch (Exception e) {
			log(e.toString());
			return null;
		}
	}
	public AgentController createGeneration(String agentName, String apiUrl) {
		return makeCreateGeneration(agentName, HOME_AGENT_ADDRESS, homeContainer, "Generation", agentName, apiUrl);
	}

	private AgentController makeCreateHomeAgent(String name,String API_URL, long cycleTime, String brokerAgentAddress) {
		if(homeAgent == null) {
			try {
				Object[] homeArgs = new Object[3];
				homeArgs[0] = API_URL; //maxBuyPrice
				homeArgs[1] = cycleTime;
				homeArgs[2] = brokerAgentAddress;

				log("Creating agent Home ");
				homeAgent = homeContainer.createNewAgent(name, HomeAgent.class.getName(), homeArgs);
				homeAgent.start();		
			} catch (Exception e) {
				log(e.toString());
			}
		} else {
			log("HomeAgent already created");
		}
		return homeAgent;
	}
	public AgentController createHomeAgent(String API_URL) {
		return makeCreateHomeAgent(HOME_AGENT_ADDRESS, API_URL,CYCLE_TIME, BROKER_AGENT_ADDRESS);
	}

	private static String log(String s) {
		String toPrint = "[" + HomeController.class.getName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
}

