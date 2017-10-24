/** ----------------------------------------------------------------- */
/**   Broker Agent                                                    */
/** ----------------------------------------------------------------- */

package brokerAgent;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.proto.ContractNetInitiator;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import org.json.JSONObject;

import FIPA.stringsHelper;
import apiWrapper.HttpClient;

@SuppressWarnings("serial") 
public class BrokerAgent extends Agent {
	private String requestType;		// The type of request for the broker to perform (buy/sell)
	private String brokeredDeal;	// The deal and price achieved by the broker agent
	private int quantity;			// The number of units requested
	private AID[] retailAgents; 	// The list of known retail agents
	
	//Limits for dealing. 
	private int maxBuyingPrice = 6;
	private int minSellingPrice = 20;
	
	// Variables for ContractNetInitiator
	private AID bestRetailer; // The agent who provides the best offer 
	private double bestPrice; // The best offered price
	private int repliesCnt;  // The counter of replies from seller agents
	private int round;	// The current round of negotiation
	private int roundLimit = 31; // The maximum no. of rounds of negotiation. ****THIS NEEDS TO BE SET VIA GUI OR SOMETHING****
	private boolean end; // Represents negotiation round limit status
	
	private HttpClient httpClient;
	public Behaviour b;		// To store + suspend/resume AchieveReResponder behaviour
	private String API_URL;
	
	protected void setup() {
		// Print creation messages
		log("I have been created");
		
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			API_URL= args[0].toString();
			httpClient = new HttpClient(API_URL);
		}
		
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
		
		repliesCnt = retailAgents.length;
		
		// Message template to listen only for messages matching the correct interaction protocol and performative
		MessageTemplate template = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
		
		// Add the AchieveREResponder behaviour which implements the responder role in a FIPA_REQUEST interaction protocol
		// The responder can either choose to agree to request or refuse request
		addBehaviour(new AchieveREResponder(this, template) {
			protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
				log("Broker request received from " + request.getSender().getLocalName() + ". Request is: '" + request.getContent() + "'");
				brokeredDeal = null;
				
				// Refuse request if no retail agents are found
				if (retailAgents != null) {
					// Get entire request as JSON object
					JSONObject req = getRequestContent(request);	
					// Parse values from JSON object
					// Get request type value
					requestType = req.getString("requestType");
					// Get quantity value
					quantity = req.getInt("quantity");
					
					//update the roundLimit, the min price and the max buy
					updateSettings();                  //******UNCOMMENT FOR API SETTINGS*********
					round = 1;
					end = false;
					
					// Perform ContractNetInitiator behaviour
					// Get reference of this behaviour
					// Remove it after adding ContractNetInitiator and re-add in onEnd() method of ContractNetInitiator
					b = this;
					// Fill the CFP message
					ACLMessage msg = new ACLMessage(ACLMessage.CFP);
					for (int i = 0; i < retailAgents.length; ++i) {
						msg.addReceiver(retailAgents[i]);
					} 
					msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
					// We want to receive a reply in 5 secs
					msg.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
					String contentJSON = "{'requestType':'" + requestType + "','quantity':" + quantity + ",'round':" + round + ",'end':" + end +"}";
					msg.setContent(contentJSON);
					log("Sending price requests");
					
					 
					myAgent.addBehaviour(new ContractNetInitiator(myAgent, msg) {
						Vector refusals = new Vector();
						
						protected void handlePropose(ACLMessage propose, Vector v) {
							log(propose.getSender().getLocalName() + " offered: " + propose.getContent());
						}
						
						protected void handleRefuse(ACLMessage refuse) {
							log(refuse.getSender().getLocalName() + " refused");
							refusals.addElement(refuse.getSender());
						}
						
						protected void handleFailure(ACLMessage failure) {
							if (failure.getSender().equals(myAgent.getAMS())) {
								// FAILURE notification from the JADE runtime: the receiver
								// does not exist
								log("Responder does not exist");
							}
							else {
								log(failure.getSender().getLocalName() + " failed");
							}
							repliesCnt--;
						}
						
						protected void handleAllResponses(Vector responses, Vector acceptances) {
							if (responses.size() < repliesCnt) {
								// Some responder didn't reply within the specified timeout
								log("Timeout expired: missing "+ (repliesCnt - responses.size()) + " responses");
							}
							// Evaluate proposals.
							bestPrice = -1;
							bestRetailer = null;
							
							Enumeration e = responses.elements();
							acceptances.clear();
							ACLMessage accept = null;
							
							round++;
							if (round == roundLimit)
								end = true;
							
							for (int i = 0; i < refusals.size(); i++)
							{
								ACLMessage reoffer = new ACLMessage(ACLMessage.CFP);
								reoffer.addReceiver((AID) refusals.get(i));
								reoffer.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
								// We want to receive a reply in 5 secs
								reoffer.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
								String contentJSON = "{'requestType':'" + requestType + "','quantity':" + quantity + ",'round':" + round + ",'end':" + end +"}";
								reoffer.setContent(contentJSON);
								acceptances.addElement(reoffer);
								refusals.clear();
							}
							
							if (requestType.equals("Buy"))
							{
								while (e.hasMoreElements()) {
									ACLMessage msg = (ACLMessage) e.nextElement();
									if (msg.getPerformative() == ACLMessage.PROPOSE) {
										// This is an offer 
										ACLMessage reply = msg.createReply();
										reply.setPerformative(ACLMessage.CFP);
										reply.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
										// We want to receive a reply in 5 secs
										reply.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
										String contentJSON = "{'requestType':'" + requestType + "','quantity':" + quantity + ",'round':" + round + ",'end':" + end + "}";
										reply.setContent(contentJSON);
										acceptances.addElement(reply);
										JSONObject offer = getRequestContent(msg);
										double price = offer.getDouble("price");
										// Hold on to best price and retailer based on whether buying/selling
										if (bestRetailer == null || price < bestPrice) {
											// This is the best offer at present
											bestPrice = price;
											bestRetailer = msg.getSender();
											accept = reply;
										}
									}
								}
								
								if (bestPrice <= maxBuyingPrice && bestRetailer != null)
								{
									log("Accepting proposal from "+ bestRetailer.getLocalName() + ". Offer is: '" + bestPrice + " c/kWh'");
									accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
									acceptances.clear();
									acceptances.addElement(accept);
								}								
								else
								{
									log("No proposals within target range of '<= " + maxBuyingPrice + "'. Best proposal was " + bestPrice);
									if (round <= roundLimit)
									{
										log("Sending reoffer requests");
										newIteration(acceptances);
									}
									else
									{
										log("Round limit reached. Accepting best proposal from final round");
										log("Accepting proposal " + bestPrice + " from retailer "+ bestRetailer.getLocalName());
										accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
										acceptances.clear();
										acceptances.addElement(accept);
									}
								}
							}
							else
							{
								while (e.hasMoreElements()) {
									ACLMessage msg = (ACLMessage) e.nextElement();
									if (msg.getPerformative() == ACLMessage.PROPOSE) {
										// This is an offer 
										ACLMessage reply = msg.createReply();
										reply.setPerformative(ACLMessage.CFP);
										reply.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
										// We want to receive a reply in 5 secs
										reply.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
										String contentJSON = "{'requestType':'" + requestType + "','quantity':" + quantity + ",'round':" + round + ",'end':" + end + "}";
										reply.setContent(contentJSON);
										acceptances.addElement(reply);
										JSONObject offer = getRequestContent(msg);
										double price = offer.getDouble("price");
										// Hold on to best price and retailer based on whether buying/selling
										if (bestRetailer == null || price > bestPrice) {
											// This is the best offer at present
											bestPrice = price;
											bestRetailer = msg.getSender();
											accept = reply;
										}
									}
								}
								
								if (bestPrice >= minSellingPrice && bestRetailer != null)
								{
									log("Accepting proposal " + bestPrice + " from retailer "+ bestRetailer.getLocalName());
									accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
									acceptances.clear();
									acceptances.addElement(accept);
								}								
								else
								{
									log("No proposals within target range of '>= " + minSellingPrice + "'. Best proposal was " + bestPrice);
									if (round <= roundLimit)
									{
										log("Sending reoffer requests");
										newIteration(acceptances);
									}
									else
									{
										log("Round limit reached. Accepting best proposal from final round");
										log("Accepting proposal " + bestPrice + " from retailer "+ bestRetailer.getLocalName());
										accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
										acceptances.clear();
										acceptances.addElement(accept);
									}
								}
							}						
						}
						
						protected void handleInform(ACLMessage inform) {
							log(inform.getSender().getLocalName() + " successfully carried out the order");
							String contentJSON = "{'requestType':" + requestType + ", 'quantity':" + quantity + ", 'price':" + bestPrice + ",'retailerId':" + inform.getSender().getLocalName() +"}";
							brokeredDeal = contentJSON;
						}
						
						// Method to re add AchieveReResponder behaviour once ContractNet has finished
						public int onEnd() {
							myAgent.addBehaviour(b);
							return 0;
						}
					} );
					
					myAgent.removeBehaviour(this);
				}	
				else {
					log("Cannot initiate negotiation.");
					throw new RefuseException("evaluation-failed");
				}
				return null;
			}

			// If the agent agreed to the request received, then it has to perform the associated action and return the result of the action
			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
					throws FailureException {
				// Inform the initiator of success or failure
				if (brokeredDeal != null) {
					log("Informing " + request.getSender().getLocalName() + " of request success");
					ACLMessage inform = request.createReply();
					inform.setPerformative(ACLMessage.INFORM);
					// Reply with the string received from Request Performer
					inform.setContent(brokeredDeal);
					return inform;
				} else {
					// Action failed
					log("Negotiation failed. Informing " + request.getSender().getLocalName());
					throw new FailureException("unexpected-error");
				}
			}
		});	
		
		log("Waiting for broker requests...");
	}
	
	private JSONObject getRequestContent(ACLMessage request) {
		return new JSONObject(request.getContent());
	}
	
	private void updateSettings() {
		try {
			String settings = httpClient.getSettings();
			JSONObject jsonSettings = new JSONObject(settings);
			roundLimit= (int) (jsonSettings.get("roundsLimit"));
			minSellingPrice = (int) (jsonSettings.get("minSellingPrice"));
			maxBuyingPrice = (int) (jsonSettings.get("maxBuyingPrice"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
}