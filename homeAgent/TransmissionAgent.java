package homeAgent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.FailureException;

import java.util.Date;

import org.json.JSONObject;

public class TransmissionAgent  extends Agent {
	private int quantity=0;
	private int getQuantity() {
		return quantity;
	}


	private String curBrokerRequest;
	private String brokerAddress=null;
	private MessageTemplate homeRequestTemplate = MessageTemplate.and(
			MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
			MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
	protected void setup() {
		Object[] args = getArguments();
		if (args == null || args.length == 0) {
			throw new Error("transmissionAgent requires args !!!");
			//FELLFAST Strategy
		}
		log("I have been created");
		brokerAddress = (String) args[0];

		//Triggered each time the home sent a message
		addBehaviour(new CyclicBehaviour(this) {

			@Override
			public void action() {
				ACLMessage msg = receive();
				//check if msgf and is the msg is a request (from home)
				if (msg!=null) {
					if (homeRequestTemplate.match(msg)) {
						log("Received request from "+ msg.getSender().getLocalName()+" with status : "+ msg.getPerformative() +" : " + msg.getContent());
						quantity = getRequestContent(msg).getInt("quantity");
						// Create REQUEST message 
						ACLMessage requestToBroker = new ACLMessage(ACLMessage.REQUEST);
						// Add receivers from input args
						requestToBroker.addReceiver(new AID(brokerAddress, AID.ISLOCALNAME));
						// Set the interaction protocol
						requestToBroker.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
						// Specify the reply deadline (10 seconds)
						requestToBroker.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
						// Set message content, if quantity >0 we need to buy otherwise sell
						String contentJSON = "{'requestType':'" 
								+( ( getQuantity() > 0) ? "Buy" : "Sell" )+ 
								"','quantity':" + getQuantity() + "}";

						requestToBroker.setContent(contentJSON);
						curBrokerRequest = contentJSON;

						// Add AchieveREInitiator behaviour 
						myAgent.addBehaviour(new AchieveREInitiator(myAgent, requestToBroker) {

							// Method to handle an agree message from responder
							protected void handleAgree(ACLMessage agree) {
								log(agree.getSender().getLocalName() + " has agreed to the request");
								ACLMessage replyToHome = msg.createReply();
								//The retzilers have agreed. send the info to home
								replyToHome.setPerformative(ACLMessage.AGREE);
								replyToHome.setContent(agree.getContent());
								send(replyToHome);
							}

							// Method to handle an inform message from responder
							protected void handleInform(ACLMessage inform) {
								JSONObject response = getRequestContent(inform);
								log(inform.getSender().getLocalName() + " successfully performed the request: '" + curBrokerRequest);
								log(inform.getSender().getLocalName() + " negotiated price of: '" + response.getDouble("price") + " c/kWh'");
								log("Informing " + msg.getSender().getLocalName());
								ACLMessage replyToHome = msg.createReply();
								//The retzilers have agreed. send the info to home
								replyToHome.setPerformative(ACLMessage.INFORM);
								replyToHome.setContent(inform.getContent());
								send(replyToHome);

							}

							// Method to handle a refuse message from responder
							protected void handleRefuse(ACLMessage refuse) {
								log(refuse.getSender().getLocalName() + " refused to perform the requested action");
								ACLMessage replyToHome = msg.createReply();
								//The retailers have refused. send the info to home
								replyToHome.setPerformative(ACLMessage.REFUSE);
								replyToHome.setContent(refuse.getContent());
								send(replyToHome);
							}

							// Method to handle a failure message (failure in delivering the message)
							protected void handleFailure(ACLMessage failure) {
								if (failure.getSender().equals(myAgent.getAMS())) {
									// FAILURE notification from the JADE runtime: the receiver (receiver does not exist)
									log("Responder does not exist");
								} else {
									log(failure.getSender().getLocalName() + " failed to perform the requested action.");
									log("Informing " + msg.getSender().getLocalName());
									ACLMessage replyToHome = msg.createReply();
									replyToHome.setPerformative(ACLMessage.FAILURE);
									send(replyToHome);
								}
							}
						});
					}
				}
				else {
					block(); //waiting for a request from home
				}
			}
		});
		log("Waiting a request from homeAgent");
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