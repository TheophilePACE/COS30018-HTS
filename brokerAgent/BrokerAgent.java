/** ----------------------------------------------------------------- */
/**   Broker Agent                                                    */
/**   Receives energy buy/sell request from initiator agent.          */
/**   Forwards request as negotiation to available retail agents.     */
/**   Accepts best proposal and informs initiator agent of result     */
/** ----------------------------------------------------------------- */

package brokerAgent;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import org.json.JSONObject;

@SuppressWarnings("serial")
public class BrokerAgent extends Agent {
	private String requestContent;	// The entire request 
	private String requestType;		// The type of request for the broker to perform (buy/sell)
	private String brokeredDeal;	// The deal and price achieved by the broker agent
	private int quantity;			// The number of units requested
	private AID[] retailAgents; 	// The list of known retail agents
	public Behaviour b;				// To store + suspend AchieveReResponder behaviour
	
	protected void setup() {
		// Print creation messages
		log("I have been created");
		
		// Search for available retail agents
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Retail Agent");
		dfd.addServices(sd);
		
		// Wait 1s to ensure retail agents have started properly
		doWait(1000);
		
		// Create and print list of retail agents found
		try {
			DFAgentDescription[] result = DFService.search(this, dfd); 
			log("Found the following retail agents:");
			retailAgents = new AID[result.length];
			for (int i = 0; i < result.length; ++i) {
				retailAgents[i] = result[i].getName();
				log(retailAgents[i].getLocalName());
			}
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		// Message template to listen only for messages matching the correct interaction protocol and performative
		MessageTemplate template = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

		
		// Add the AchieveREResponder behaviour which implements the responder role in a FIPA_REQUEST interaction protocol
		// The responder can either choose to agree to request or refuse request
		addBehaviour(new AchieveREResponder(this, template) {
			protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
				log("Broker request received from: " + request.getSender().getLocalName() + ". Request is: '" + request.getContent() + "'");
				
				// Refuse request if no retail agents are found
				ACLMessage response = request.createReply();
				if (retailAgents != null) {
					response.setPerformative(ACLMessage.AGREE);
					// Get entire request as both JSON object and string
					JSONObject req = getRequestContent(request);
					requestContent = request.getContent();
					
					// Parse values from JSON object
					// Get request type value
					requestType = req.getString("requestType");
					// Get quantity value
					quantity = req.getInt("quantity");
					
					// Perform the action
					// Get reference of this behaviour
					// Remove it after adding request performer and re-add in request performer onEnd()
					b = this;
					myAgent.addBehaviour(new RequestPerformer());
					myAgent.removeBehaviour(this);
				}	
				else {
					response.setPerformative(ACLMessage.REFUSE);
				}
				return response;
			}

			// If the agent agreed to the request received, then it has to perform the associated action and return the result of the action
			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
					throws FailureException {
				// Inform the initiator of success or failure
				if (brokeredDeal != null) {
					log("Action successfully performed, informing initiator");
					ACLMessage inform = request.createReply();
					inform.setPerformative(ACLMessage.INFORM);
					// Reply with the string received from Request Performer
					inform.setContent(brokeredDeal);
					return inform;
				} else {
					// Action failed
					log("Action failed, informing initiator");
					throw new FailureException("unexpected-error");
				}
			}
		});	
		
		log("Waiting for broker requests...");
	}
	
	private JSONObject getRequestContent(ACLMessage request) {
		return new JSONObject(request.getContent());
	}
	
	private String log(String s) {
		String toPrint = "[" + getLocalName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}

	// Agent clean-up
	protected void takeDown() {
		// Print termination message
		log("Terminating.");
	}

	/**
	   Inner class RequestPerformer.
	   This is the behaviour used by broker agent to request retail
	   agents a buy/sell price offer.
	 */
	private class RequestPerformer extends Behaviour {
		private AID bestRetailer; // The agent who provides the best offer 
		private double bestPrice;  // The best offered price
		private int repliesCnt = 0; // The counter of replies from seller agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;

		public void action() {
			switch (step) {
			case 0:
				// Send the cfp to all sellers
				log("Sending price requests");
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < retailAgents.length; ++i) {
					cfp.addReceiver(retailAgents[i]);
				} 
				cfp.setContent(requestContent);
				cfp.setConversationId(requestType + " energy");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId(requestType + " energy"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer 
						JSONObject offer = getRequestContent(reply);
						double price = offer.getDouble("price");
						// Hold on to best price and retailer based on whether buying/selling
						if (requestType.equals("Buy")) {
							if (bestRetailer == null || price < bestPrice) {
								// This is the best offer at present
								bestPrice = price;
								bestRetailer = reply.getSender();
							}
						}
						else {
							if (bestRetailer == null || price > bestPrice) {
								// This is the best offer at present
								bestPrice = price;
								bestRetailer = reply.getSender();
							}
						}
					}
					repliesCnt++;
					if (repliesCnt >= retailAgents.length) {
						// We received all replies
						log("Received all proposals");
						step = 2; 
					}
				}
				else {
					block();
				}
				break;
			case 2:
				// Send the final order to the retail agent that provided the best offer
				log("Accepting proposal from: " + bestRetailer.getLocalName());
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestRetailer);
				order.setContent(requestContent);
				order.setConversationId(requestType + " energy");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				// Prepare the template to get the purchase order reply
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId(requestType + " energy"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:      
				// Receive the purchase order reply
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Purchase order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Purchase successful. We can inform initiator
						if (requestType.equals("Buy"))
							log("'" + requestContent + "' successfull. Purchased from: " + reply.getSender().getLocalName());

						else
							log("'" + requestContent + "' successfull. Sold to: " + reply.getSender().getLocalName());
						
						log("Price: '" + bestPrice + " c/kWh'");
						String contentJSON = "{'requestType':" + requestType + ", 'quantity':" + quantity + ", 'price':" + bestPrice + "}";
						brokeredDeal = contentJSON;
					}
					else {
						log("Attempt failed.");
					}
					step = 4;
				}
				else {
					block();
				}
				break;
			}        
		}
		
		// Method to re add AchieveReResponder behaviour once RequestPerforme has finished
		public int onEnd() {
			myAgent.addBehaviour(b);
			return 0;
		}

		public boolean done() {
			if (step == 2 && bestRetailer == null) {
				log("Attempt failed. Retailers not found");
			}
			return ((step == 2 && bestRetailer == null) || step == 4);
		}
	}  // End of inner class RequestPerformer
}
