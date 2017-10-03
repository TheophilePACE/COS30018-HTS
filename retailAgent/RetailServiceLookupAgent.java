/*********************************************************************************** 
 * This agents collects the address of all agents providing the a certain service
 * The service has to be passed by setting it in the argument
 ***********************************************************************************/

package retailAgent;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
//import jade.core.AID;

public class RetailServiceLookupAgent extends Agent {
		// The service type to be looked up
		private String service;
		
		// The list of known seller agents ???unnecessary b/c of DFAgentDescription[]???
		//private AID[] sellerAgents;
		
		// Add a subscribe function as well???
		
		protected void setup() 
		{
			// Set the service to be looked up
			Object[] args = getArguments();
			service = args[0].toString();
			
			// Get list of agents offering "Electricity" service
			DFAgentDescription[] serviceAgents = getService(service);
			for (DFAgentDescription serviceAgent : serviceAgents) {
				log(serviceAgent.getName().getName());
			
			}
		}

		DFAgentDescription[] getService( String service_name )
		{
			DFAgentDescription dfd = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType( service_name );
			dfd.addServices(sd);
			try {
				DFAgentDescription[] result = DFService.search(this, dfd);
				return result;
			}
			catch (Exception fe) {
				log("ERROR: " + fe.toString());
			}
			return null;
		}
		private String log(String s) {
			String toPrint = "[" + getLocalName() + "] " + s;
			System.out.println(toPrint);
			return toPrint;
		}
}