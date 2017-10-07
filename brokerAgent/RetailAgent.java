/** ----------------------------------------------------------------- */
/**   Retail Agent                                                    */
/**   Takes service type and company name as input args, proposes     */
/**   random energy prices to broker agent upon negotiation request.  */
/**   Buys/sells the requested energy upon acceptance of proposal     */
/** ----------------------------------------------------------------- */

package brokerAgent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

import org.json.JSONObject;

@SuppressWarnings("serial")
public class RetailAgent extends Agent {
	// The current price for electricity
	private double getBuyPrice() {
		return Math.ceil((Math.random() * 6) + 5); // 5 - 11
	}
	private double getSellPrice() {
		return Math.ceil((Math.random() * 10) + 20); // 20 - 30
	}
	private double currentOffer;

	// Agent initialization
	protected void setup() {
		Object[] args = getArguments();
		if (args == null || args.length == 0) {
			throw new Error("Retailer AGent needs arguments!!!");
		}		// Printout a welcome message
		log("I have been created. Company: " + args[1].toString());

		// Set the type of service and the company name as a start-up argument
		ServiceDescription sd  = new ServiceDescription();
		sd.setType( args[0].toString());
		sd.setName( args[1].toString());
		register(sd);

		// Add behaviour handling request from buyers
		addBehaviour(new OfferRequestBehaviour());

		// Add behaviour to handle negotiation process
		addBehaviour(new HandleNegotiationBehaviour());

		log("Waiting for price requests...");
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
		log("Terminating.");
		}
		catch (Exception e) {
			log("Error at service de-registration of Agent " + getLocalName() );
		}
	}
	/**
	 Inner class PriceResponderBehaviour
	 This behaviour is used by the RetailAgent to propose an energy price
	 to a buyer/seller who is sending a price request  
	 */
	private class OfferRequestBehaviour extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage request = myAgent.receive(mt);

			if (request != null) {
				log("Price request received from: " + request.getSender().getLocalName() + ". Request is: '" + request.getContent() + "'");
				ACLMessage reply = request.createReply();
				
				// Get request as JSON object
				JSONObject req = getRequestContent(request);
				String requestType = req.getString("requestType");

				if (checkAction()) {
					// Reply with the price offer based on request type
					double price = 0;
					
					if (requestType.equals("Buy")) // The broker wants to buy
						price = getSellPrice();
					else // The broker wants to sell
						price = getBuyPrice();
					
					// Reply with price offer based on request type
					log("Offering price: '" + price + " c/kWh'");
					String contentJSON = "{'price':" + price + "}";
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(contentJSON);
					currentOffer = price;
				}
				else {
					// The request cannot be fulfilled at this time
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent(getLocalName() + ": Cannot provide an offer at this time.");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}

	}  // End of inner class PriceResponderBehaviour

	/**
	  Inner class HandleNegotiationBehaviour
	  This behaviour is used if a response to a previous proposal has been received
	 */

	private class HandleNegotiationBehaviour extends CyclicBehaviour {
		// Switch structure implemented for potential extra negotiation in future
		private int step = 0;
		
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
		
		public void action() {
			switch (step) {
			case 0:
				// Receive accepted proposal message
				ACLMessage order = myAgent.receive(mt);
				
				if (order != null) 
				{
					log(order.getSender().getLocalName() + " accepted our proposal");
					log("Informing " + order.getSender().getLocalName() + " of order success");
					// Inform broker agent of order success
					ACLMessage reply = order.createReply();
					reply.setPerformative(ACLMessage.INFORM);
					String contentJSON = "{'price':" + currentOffer + "}";
					reply.setContent(contentJSON);
					myAgent.send(reply);
				}
				else
				{
					block();
				}
			}
		}
	}
	
	private JSONObject getRequestContent(ACLMessage request) {
		return new JSONObject(request.getContent());
	}
	
	private String log(String s) {
		String toPrint = "[" + getLocalName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
	
	//Method to check if requested action should be continued
	// 10% chance to fail at this stage
	private boolean checkAction() {
		if (Math.random() > 0.1)
			return true;
		else
			return false;
	}
}
