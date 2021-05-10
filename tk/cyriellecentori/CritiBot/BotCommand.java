package tk.cyriellecentori.CritiBot;


import java.util.Vector;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class BotCommand {
	
	public BotCommand() {
	}
	
	public abstract void execute(CritiBot bot, MessageReceivedEvent message, String[] args);
	
	public static void sendLongMessage(String message, MessageChannel chan) {
		String[] spli = message.split("\n");
		String send = "";
		for(String str : spli) {
			if(send.length() + str.length() + 1 > 2000) {
				chan.sendMessage(send).queue();
				send = "";
			}
			send += str + "\n";
		}
		chan.sendMessage(send).queue();
	}
	
	public static class Alias extends BotCommand {

		BotCommand target;
		
		public Alias(BotCommand target) {
			super();
			this.target = target;
		}

		@Override
		public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
			target.execute(bot, message, args);
		}
		
		
		
	}
	
	public static abstract class SearchCommand extends BotCommand {
		
		public SearchCommand() {
			super();
		}
		
		public abstract void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args);
		
		public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
			try {
				Vector<Ecrit> res = bot.search(args[0]);
				if(res.size() > 1) {
					message.getChannel().sendMessage("J'ai plus d'un résultat : il va falloir affiner le critère de recherche.").queue();
				} else if(res.size() == 0) {
					message.getChannel().sendMessage("Aucun résultat trouvé.").queue();
				} else {
					process(res.get(0), bot, message, args);
				}
			} catch(ArrayIndexOutOfBoundsException e) {
				message.getChannel().sendMessage("Mauvais usage de la commande.").queue();
			}
		}
	}
}
