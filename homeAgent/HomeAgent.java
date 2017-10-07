package homeAgent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.domain.FIPANames;

import java.util.Date;
import java.util.HashMap;

import org.json.*;

public class HomeAgent extends Agent {
	private long CYCLE_TIME;
	//Internal variables
	private int energyBalance = 0; //sum of production and consumption of energy
	private int energyProducted = 0;
	private int energyConsumed = 0;

	//Limits for dealing. Could be change by the user
	private int maxBuyingPrice = 0;
	private int minSellingPrice = 0;

	//TODO Change at every cycle. Provided by the Broker
	@SuppressWarnings("unused")
	private int BetterPrice = 0;
	@SuppressWarnings("unused")
	private String BetterProvider = "";
	private String transmissionAgentAddress= "";
	private HashMap<String, Integer> applianceEnergyBalance = new HashMap<String, Integer>();
	protected void setup() {
		//FIPANames.InteractionProtocol.FIPA_REQUEST
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			maxBuyingPrice= Integer.parseInt(args[0].toString());
			minSellingPrice=Integer.parseInt(args[1].toString());
			CYCLE_TIME=Long.parseLong(args[2].toString());
			transmissionAgentAddress=args[3].toString();
			log("I have been created. CUSTOM maxBuyingPrice is "+ maxBuyingPrice +" and minSellingPrice is "+ minSellingPrice);
		} else {
			log("I have been created. DEFAULT maxBuyingPrice is "+ maxBuyingPrice +" and minSellingPrice is "+ minSellingPrice);
		}

		MessageTemplate energyBalanceMessageTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST));


		CyclicBehaviour energyBalanceReceivingBehaviour = (new CyclicBehaviour(this) {
			public void action() {
				ACLMessage msg= receive();
				if (msg!=null) {
					// Print out message content
					log(": Received message " + msg.getContent() + " from " + msg.getSender().getLocalName());
					if(energyBalanceMessageTemplate.match(msg)) {
						JSONObject JSONmsg = new JSONObject(msg.getContent());
						log("energy balance for " + msg.getSender().getLocalName() + " => consumption: " + JSONmsg.getInt("consumption"));
						setApplianceEnergyBalance(msg.getSender().getLocalName(), new Integer(JSONmsg.getInt("consumption")));
					} else {
						log("received message not matching : "+ msg.getSender().getName() + msg.getContent() );
					}
				}
				// Continue listening
				block();
			}
		}
		);
		addBehaviour(energyBalanceReceivingBehaviour);

		 
		addBehaviour(new TickerBehaviour(this,CYCLE_TIME) {
			public void onTick() {
				log("Going to compute balance");
				int quantity =  makeEnergyBalance();
				log("Consumption: " + energyConsumed + " production: " + energyProducted + " --> BALANCE = " + quantity);
				ACLMessage requestToTransmissionAgent = new ACLMessage(ACLMessage.REQUEST);
				// Add receivers from input args
				requestToTransmissionAgent.addReceiver(new AID(transmissionAgentAddress, AID.ISLOCALNAME));
				// Set the interaction protocol
				requestToTransmissionAgent.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				// Specify the reply deadline (10 seconds)
				requestToTransmissionAgent.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
				// Set message content, if quantity >0 we need to buy otherwise sell
				String contentJSON = "{'quantity':" + quantity + "}";

				requestToTransmissionAgent.setContent(contentJSON);
				myAgent.addBehaviour(new AchieveREInitiator(myAgent, requestToTransmissionAgent) {
					protected void handleAgree(ACLMessage agree) {
						log(agree.getSender().getLocalName() + " has agreed to the request");
					}
					
					// Method to handle an inform message from responder
					protected void handleInform(ACLMessage inform) {
						JSONObject response = new JSONObject(inform.getContent());
						log(inform.getSender().getLocalName() + " successfully performed the request: '" + requestToTransmissionAgent.getContent());
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

	}
	private void setApplianceEnergyBalance(String applianceName, Integer balance) {
		applianceEnergyBalance.put(applianceName, balance);
	}
	//TODO consider positive negative ...
	private int makeEnergyBalance() {
		energyConsumed = 0;
		energyProducted = 0;

		applianceEnergyBalance.forEach((k,v)->{
			if(v>0)
				energyConsumed+=v;
			else
				energyProducted+=v;
			storeApplianceEnergyBalance(k, v); //Store old value
		});
		applianceEnergyBalance.clear(); // remove old values

		energyBalance=energyConsumed + energyProducted;
		return energyBalance;
	}
	private void storeApplianceEnergyBalance(String k,int v) {
		//TODO IN A DBMS
		Date date = new Date();
		log("STORE: " +date.toString() + ": device"+ k + "-->" + v +"kW");
	}

	private String log(String s) {
		String toPrint = "[" + getLocalName() + "] " + s;
		System.out.println(toPrint);
		return toPrint;
	}
}
