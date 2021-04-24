package net.dodian.uber.game.model.player.casino;

import net.dodian.uber.game.Server;

public class Spin {
  Symbol[] symbols = new Symbol[3];
  int winnings = 0;
  
  public Spin(Symbol[] symbols) {
	    this.symbols[0] = symbols[0];
	    this.symbols[1] = symbols[1];
	    this.symbols[2] = symbols[2];
	    symbols[0].setColor("red");
	    symbols[1].setColor("red");
	    symbols[2].setColor("red");
	    if (symbols[0].getId() == symbols[1].getId() && symbols[1].getId() == symbols[2].getId()) {
    	    symbols[0].setColor("gre");
    	    symbols[1].setColor("gre");
    	    symbols[2].setColor("gre");
	    	if(symbols[0].getId() == 8) //Jackpot!
	    	    winnings = Server.slots.slotsJackpot + Server.slots.peteBalance >= Integer.MAX_VALUE ? Integer.MAX_VALUE : Server.slots.slotsJackpot + Server.slots.peteBalance;
	    	else if(symbols[0].getId() == 7)
	    	    winnings = 25000;
	    	else
	    		winnings = 3500;
	    } else if (symbols[0].getId() == symbols[1].getId() || symbols[1].getId() == symbols[2].getId() || symbols[0].getId() == symbols[2].getId()) {
	        int id = -1;
	        if (symbols[0] == symbols[1]) {
	          id = symbols[0].getId();
	    	  symbols[0].setColor("gre");
	    	  symbols[1].setColor("gre");
	        } else if (symbols[0] == symbols[2]) {
	          id = symbols[0].getId();
	    	  symbols[0].setColor("gre");
	    	  symbols[2].setColor("gre");
	        } else if (symbols[1] == symbols[2]) {
	          id = symbols[1].getId();
	    	  symbols[1].setColor("gre");
	    	  symbols[2].setColor("gre");
	        }
	        if(id != -1) {
	        	if(id == 8)
	        		winnings = 30000;
	        	else if(id == 7)
	        		winnings = 11000;
	        	else
	        		winnings = 1500;
	        }
	    } else {
	    	boolean found = false;
	    	for(int i = 0; i < symbols.length && !found; i++)
	    		if(symbols[i].getId() == 8 || symbols[i].getId() == 7) {
	    			symbols[i].setColor("gre");
	    			winnings = 1500;
	    			found = true;
	    		} else winnings = 0;
	    }
	  }

  public Symbol[] getSymbols() {
    return symbols;
  }

  public int getWinnings() {
    return winnings;
  }
}
