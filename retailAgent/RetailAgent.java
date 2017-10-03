/********************************************************************************************************** 
 *This goal of this agent is to provide electricity prices upon a request from the home agent/broker agent
 *DO NOT FORGET TO PASS TYPE AND NAME AS ARGUMENTS  
 **********************************************************************************************************/

package retailAgent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
//import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
//import jade.proto.AchieveREResponder;
//import jade.domain.FIPANames;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

public class RetailAgent extends Agent {
	// The current price for electricity
	private int electricity_price = 50 * (int)Math.random();
	private String type ;
	private String name ;
	// Agent initialization
	private void computeElectricityPrice(String req) {
		electricity_price = (int) (50 * Math.random());
		log("for the request " + req + "the price is " + electricity_price);
	}
	
	protected void setup() {
		//Get type and name
		Object[] args = getArguments();
		type =args[0].toString();
		name = args[1].toString();

		// Set the type of service and the company name as a start-up argument
		ServiceDescription sd  = new ServiceDescription();
		sd.setType(type);
		sd.setName(name);
		register(sd);

		// Add behaviour handling request from buyers
		addBehaviour(new OfferRequestBehaviour());

		// Add behaviour to handle negotiation process
		addBehaviour(new HandleNegotiationBehaviour());

		// Printout a welcome message
		log("reatiler ready! provide the service: " + type + " and is working for company: " + name);
	}

	// Method to register the service
	void register( ServiceDescription sd)
	{
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		dfd.addServices(sd); 

		try {  
			DFService.register(this, dfd );  
		}
		catch (FIPAException fe) { fe.printStackTrace(); }
	}

	// Method to de-register the service (on take down)
	protected void takeDown() 
	{
		try { DFService.deregister(this);
		// Printout a dismissal message
		log("Buyer-agent "+ getAID().getName()+" is terminating.");
		}
		catch (Exception e) {
			log("Error at service de-registration of Agent " + getLocalName() );
		}
	}
	
	
	/**
	 Inner class PriceResponderBehaviour
	 This behaviour is used by the RetailAgent to propose an electricity price
	 to a potential buyer who is sending a price request  
	 */
	private class OfferRequestBehaviour extends CyclicBehaviour {
		public void action() {
			log(getAID().getName() + ": waiting for price requests...");

			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage request = myAgent.receive(mt);

			if (request != null) {
				log(getAID().getName() + ": PRICE REQUEST received from "
						+ request.getSender().getName() + ". Query is: '" + request.getContent() + "'");
				ACLMessage reply = request.createReply();

				if (checkAction()) {
					// Reply with the price if negotiation went good
					computeElectricityPrice(request.getContent()); // sdet the new electricity price
					log("offering to" + request.getSender() + "electricity at a price of " + electricity_price + "AU$/kWh");
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent("{'price':" + electricity_price +"}");
				} else {
					// The requested book is NOT available for sale.
					reply.setPerformative(ACLMessage.REFUSE);
					log(" can not provide any electricity at the moment");
					reply.setContent("{'price':null}");
				}
				myAgent.send(reply);
			} else {
				block();
			}
		}

	}  // End of inner class PriceResponderBehaviour

	/**
	  Inner class HandleNegotiationBehaviour
	  This behaviour is used if a response to a previous proposal has been received
	 */

	private class HandleNegotiationBehaviour extends CyclicBehaviour {
		public void action() {
			// use getPerformative and then distinguish in between response with switch and case method
			log("NEGOTIATION TBD!");
			block();
		}
	}
	//Method to check if requested action should be continued
	private boolean checkAction() {
		// space holder for possible reason not to sell electricity
		return true;
	}
	public int getElectricity_price() {
		return electricity_price;
	}
	private String log(String s) {
		String toPrint = "[" + getLocalName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
}