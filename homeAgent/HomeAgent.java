package homeAgent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
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
	private int BetterPrice = 0;
	private String BetterProvider = "";
	
	private HashMap<String, Integer> applianceEnergyBalance = new HashMap<String, Integer>();
	protected void setup() {
		//FIPANames.InteractionProtocol.FIPA_REQUEST
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			maxBuyingPrice= Integer.parseInt(args[0].toString());
			minSellingPrice=Integer.parseInt(args[1].toString());
			CYCLE_TIME=Long.parseLong(args[2].toString());
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
		
		TickerBehaviour makeBalanceBehaviour = new TickerBehaviour(this,CYCLE_TIME) {
			public void onTick() {
				log("Going to compute balance");
				makeEnergyBalance();
				log("Consumption: " + energyConsumed + " production: " + energyProducted + " --> BALANCE = " + energyBalance);	
			}
		};
		addBehaviour(makeBalanceBehaviour);
		
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
