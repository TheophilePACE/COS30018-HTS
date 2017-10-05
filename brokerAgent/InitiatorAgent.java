/** ----------------------------------------------------------------- */
/**   Initiator Agent                                                 */
/**   Takes tick rate and BrokerAgent name as input args, sends       */
/**   random buy/sell request to BrokerAgent each tick that           */
/**   is between 10 - 20 kWh                                          */
/** ----------------------------------------------------------------- */

package brokerAgent;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.domain.FIPANames;
import java.util.Date;

import org.json.JSONObject;

@SuppressWarnings("serial")
public class InitiatorAgent extends Agent {
	private int getQuantity() {
		return (int) Math.ceil((Math.random() * 10) + 10);
	}
	private String curBrokerRequest;
	
	protected void setup() {
		Object[] args = getArguments();
		
		if (args != null && args.length > 0) {
			log("I have been created");
			
			// Add Ticker Behaviour, rate is input arg
			addBehaviour(new TickerBehaviour(this, (long)args[0]) {				
				@Override
				public void onTick() {
					System.out.println("\n");
					log("Sending broker request");
					
					// Create REQUEST message 
					ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
					// Add receivers from input args
					msg.addReceiver(new AID((String)args[1], AID.ISLOCALNAME));
					// Set the interaction protocol
					msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					// Specify the reply deadline (10 seconds)
					msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
					// Set message content, random chance for buy or sell at this stage
					String contentJSON;
					if (Math.random() > 0.5)
						contentJSON = "{'requestType':'Buy','quantity':" + getQuantity() + "}";
					else
						contentJSON = "{'requestType':'Sell','quantity':" + getQuantity() + "}";

					msg.setContent(contentJSON);
					curBrokerRequest = contentJSON;
					
					// Add AchieveREInitiator behaviour 
					myAgent.addBehaviour(new AchieveREInitiator(myAgent, msg) {
						
						// Method to handle an agree message from responder
						protected void handleAgree(ACLMessage agree) {
							log(agree.getSender().getLocalName() + " has agreed to the request");
						}
						
						// Method to handle an inform message from responder
						protected void handleInform(ACLMessage inform) {
							JSONObject response = getRequestContent(inform);
							log(inform.getSender().getLocalName() + " successfully performed the request: '" + curBrokerRequest);
							log(inform.getSender().getLocalName() + " negotiated price of: '" + response.getDouble("price") + " c/kWh'");
						}

						// Method to handle a refuse message from responder
						protected void handleRefuse(ACLMessage refuse) {
							log(refuse.getSender().getLocalName() + " refused to perform the requested action");
						}

						// Method to handle a failure message (failure in delivering the message)
						protected void handleFailure(ACLMessage failure) {
							if (failure.getSender().equals(myAgent.getAMS())) {
								// FAILURE notification from the JADE runtime: the receiver (receiver does not exist)
								log("Responder does not exist");
							} else {
								log(failure.getSender().getLocalName() + " failed to perform the requested action");
							}
						}
					});
				}
			});
			log("Waiting to tick");
		}
		else {
			log("Instantiation failed.");
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
	
	protected void takeDown() {
		System.out.println(getLocalName() + ": Terminating");
		// do cleanup
	}
}
