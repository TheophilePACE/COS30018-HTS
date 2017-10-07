package homeAgent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

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
	private AID[] applianceList;

	MessageTemplate energyBalanceMessageTemplate = MessageTemplate.and(
			MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
			MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

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

		TickerBehaviour triggerEnergyBalance = (new TickerBehaviour(this,CYCLE_TIME) {
			public void onTick() {
				System.out.println();
				log("<---------------- NEW CYCLE ----------------->");
				//Get all aplliances address
				applianceList = getAgentDescriptionList("Appliance");
				//MSG to all appliances asking for consumption
				ACLMessage consumptionRequest = createConsumptionRequest(applianceList);
				addBehaviour(new collectApplianceEnergyBalances(myAgent,consumptionRequest, applianceList.length));
			}
		});
		addBehaviour(triggerEnergyBalance);
	}
	
	private class collectApplianceEnergyBalances extends AchieveREInitiator {
		private int nResponders =0;
		private Agent a;
		public collectApplianceEnergyBalances(Agent ag, ACLMessage consumptionRequest, int nbAppliances) {
			super(ag, consumptionRequest);
			a= ag;
			nResponders = nbAppliances;
		}
		private ACLMessage createTradeRequest(int quantity) {
			
			ACLMessage tradeRequest = new ACLMessage(ACLMessage.REQUEST);
			// Add receivers from input args
			tradeRequest.addReceiver(new AID(transmissionAgentAddress, AID.ISLOCALNAME));
			// Set the interaction protocol
			tradeRequest.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
			// Specify the reply deadline (10 seconds)
			tradeRequest.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
			// Set message content, if quantity >0 we need to buy otherwise sell
			String contentJSON = "{'quantity':" + quantity + "}";
			tradeRequest.setContent(contentJSON);
			return tradeRequest;
		}
		protected void handleInform(ACLMessage inform) {
			//Received the expected information
			JSONObject JSONmsg = new JSONObject(inform.getContent());
			log("energy balance for " + inform.getSender().getLocalName() + " => consumption: " + JSONmsg.getInt("consumption"));
			setApplianceEnergyBalance(inform.getSender().getLocalName(), new Integer(JSONmsg.getInt("consumption")));
			nResponders--;
		}
		protected void handleFailure(ACLMessage failure) {
			if (failure.getSender().equals(a.getAMS())) {
				// FAILURE notification from the JADE runtime: the receiver
				// does not exist
				System.out.println("Responder does not exist");
			}
			else {
				System.out.println("Agent "+failure.getSender().getName()+" failed to perform the requested action");
			}
		}
		protected void handleAllResultNotifications(@SuppressWarnings("rawtypes") Vector notifications) {
			//Response received/not received from all appliances
			if (notifications.size() < nResponders) {
				// Some responder didn't reply within the specified timeout
				log("Timeout expired: missing "+(nResponders - notifications.size())+" responses");
			} else {
				log("Received all consumption respones.");
			}
			int quantity =  makeEnergyBalance();
			log("Consumption: " + energyConsumed + " production: " + energyProducted + " --> BALANCE = " + quantity);
			ACLMessage tradeRequest = createTradeRequest(quantity);
			a.addBehaviour(new Negotiation(a,tradeRequest));
		}
		
	} 
	
	private class Negotiation extends AchieveREInitiator{
		private ACLMessage tR;
		public Negotiation(Agent a, ACLMessage tRM) {
			super(a, tRM);
			tR = tRM;
		}

		protected void handleAgree(ACLMessage agree) {
			log(agree.getSender().getLocalName() + " has agreed to the request");
		}

		// Method to handle an inform message from responder
		protected void handleInform(ACLMessage inform) {
			JSONObject response = new JSONObject(inform.getContent());
			log(inform.getSender().getLocalName() + " successfully performed the request: '" + tR.getContent()
			+ " negotiated price of: '" + response.getDouble("price") + " c/kWh'");
			log("<-----------------END OF NEGOTIATION ---------------------->");
			System.out.println();
			System.out.println();
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
	}

	private ACLMessage createConsumptionRequest(AID[] receivers) {
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		for (int i = 0; i < receivers.length; ++i) {
			msg.addReceiver(receivers[i]);
		}
		msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
		msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
		msg.setContent("{'request':'energyConsumption'}");
		return msg;
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

	private AID[] getAgentDescriptionList(String serviceType) {
		// Search for appliance agents
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(serviceType);
		dfd.addServices(sd);

		// Create and print list of retail agents found
		try {
			DFAgentDescription[] result = DFService.search(this, dfd); 
			log("Found the following" + serviceType + " agents:");
			AID[] agentsFound = new AID[result.length];
			for (int i = 0; i < result.length; ++i) {
				agentsFound[i] = result[i].getName();
				log(agentsFound[i].getLocalName());
			}
			return agentsFound;
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
			return null;
		}
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
