/* ----------------------------------------------------------------- */
/*    Run ApplianceAgentController for Appliance / Responder demo    */
/* ----------------------------------------------------------------- */

package global;

import jade.core.Runtime;
import applianceAgent.ApplianceAgent;
import homeAgent.HomeAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

public class HomeController {
	private String HOME_AGENT_ADDRESS;
	private long CYCLE_TIME;
	private ContainerController homeContainer;
	private AgentController homeAgent;

	public HomeController(String homeAgentAdress, long cycleTime, Runtime rt, String host, int port) {
		super();
		HOME_AGENT_ADDRESS =  homeAgentAdress;
		CYCLE_TIME = cycleTime;
		Profile pHomeContainer = new ProfileImpl(host,port,null,false); //create a non-main container
		pHomeContainer.setParameter("CONTAINER_NAME", "homeContainer");
		homeContainer = rt.createAgentContainer(pHomeContainer);
		log("The homeContainer was created!");
		homeAgent =null;
	}
	private AgentController makeCreateAppliance(String agentName, long cycleTime, String homeAgentAdress, ContainerController containerController) {
		Object[] appArgs = new Object[2];
		appArgs[0] = cycleTime;
		appArgs[1] = homeAgentAdress;
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
		return makeCreateAppliance(agentName, CYCLE_TIME, HOME_AGENT_ADDRESS, homeContainer);
	}
	private AgentController makeCreateHomeAgent(String name,int maxBuyPrice, int minSellPrice, long cycleTime) {
		if(homeAgent == null) {
			try {
				Object[] homeArgs = new Object[3];
				homeArgs[0] = maxBuyPrice; //maxBuyPrice
				homeArgs[1] = minSellPrice; //minSellPrice
				homeArgs[2] = cycleTime;
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
		return makeCreateHomeAgent(HOME_AGENT_ADDRESS, maxBuyPrice,minSellPrice,CYCLE_TIME);
	}

	private static String log(String s) {
		String toPrint = "[" + HomeController.class.getName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
}

