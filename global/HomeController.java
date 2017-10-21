/* ----------------------------------------------------------------- */
/*    Run ApplianceAgentController for Appliance / Responder demo    */
/* ----------------------------------------------------------------- */

package global;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;


import applianceAgent.ApplianceAgent;
import homeAgent.HomeAgent;
import homeAgent.TransmissionAgent;

public class HomeController {
	private String HOME_AGENT_ADDRESS;
	private long CYCLE_TIME;
	private ContainerController homeContainer;
	private AgentController homeAgent;
	private AgentController transmissionAgent;
	private String TRANSMISSION_AGENT_ADDRESS;
	private String BROKER_AGENT_ADDRESS;

	public HomeController(String homeAgentAdress, long cycleTime, Runtime rt, String host, int port, String transmissionAgentAddress, String brokerAdress) {
		super();
		HOME_AGENT_ADDRESS =  homeAgentAdress;
		CYCLE_TIME = cycleTime;
		TRANSMISSION_AGENT_ADDRESS = transmissionAgentAddress;
		BROKER_AGENT_ADDRESS = brokerAdress;
		Profile pHomeContainer = new ProfileImpl(host,port,null,false); //create a non-main container
		pHomeContainer.setParameter(Profile.CONTAINER_NAME, "homeContainer");
		homeContainer = rt.createAgentContainer(pHomeContainer);
		log("The homeContainer was created!");
		homeAgent =null;
	} 
	private AgentController makeCreateAppliance(String agentName, long cycleTime, String homeAgentAdress, ContainerController containerController, String serviceType, String serviceName) {
		Object[] appArgs = new Object[4];
		appArgs[0] = cycleTime;
		appArgs[1] = homeAgentAdress;
		appArgs[2] = serviceType;
		appArgs[3] = serviceName;
		try {
			AgentController applianceCtrl = containerController.createNewAgent(agentName, ApplianceAgent.class.getName(), appArgs);
			applianceCtrl.start();
			return applianceCtrl; 
		} catch (Exception e) {
			log(e.toString());
			return null;
		}
	}
	public AgentController createAppliance(String agentName) {
		return makeCreateAppliance(agentName, CYCLE_TIME, HOME_AGENT_ADDRESS, homeContainer, "Appliance", agentName);
	}
	private AgentController makeCreateHomeAgent(String name,int maxBuyPrice, int minSellPrice, long cycleTime, String transmissionAgentAdress) {
		if(homeAgent == null) {
			try {
				Object[] homeArgs = new Object[4];
				homeArgs[0] = maxBuyPrice; //maxBuyPrice
				homeArgs[1] = minSellPrice; //minSellPrice
				homeArgs[2] = cycleTime;
				homeArgs[3] = transmissionAgentAdress;
				
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
	public AgentController createHomeAgent(int maxBuyPrice, int minSellPrice) {
		return makeCreateHomeAgent(HOME_AGENT_ADDRESS, maxBuyPrice,minSellPrice,CYCLE_TIME, TRANSMISSION_AGENT_ADDRESS);
	}

	private AgentController makeCreateTransmissionAgent(String name, String brokerAdress) {
		if(transmissionAgent == null) {
			try {
				Object[] homeArgs = new Object[1];
				homeArgs[0] = brokerAdress; 
				log("Creating agent Transmission ");
				transmissionAgent = homeContainer.createNewAgent(name, TransmissionAgent.class.getName(), homeArgs);
				transmissionAgent.start();		
			} catch (Exception e) {
				log(e.toString());
			}
		} else {
			log("TransmissionAgent already created");
		}
		return transmissionAgent;
	}
	public AgentController createTransmissionAgent() {
		return makeCreateTransmissionAgent(TRANSMISSION_AGENT_ADDRESS, BROKER_AGENT_ADDRESS);
	}
	
	private static String log(String s) {
		String toPrint = "[" + HomeController.class.getName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
}

