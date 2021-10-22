package tk.cyriellecentori.CritiBot;


import java.util.Vector;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Décrit une commande du bot.
 * @author cyrielle
 *
 */
public abstract class BotCommand {
	
	public BotCommand() {
	}
	
	/**
	 * Décrit l'action de la commande lorsqu'elle est executée.
	 * @param bot Une référence vers l'objet Critibot
	 * @param message Le message ayant invoqué la commande
	 * @param args Les arguments de la commande.
	 */
	public abstract void execute(CritiBot bot, MessageReceivedEvent message, String[] args);
	
	/**
	 * Permet d'envoyer un message en plusieurs parties si celui-ci dépasse les 2 000 caractères.
	 * @param message Message à envoyer
	 * @param chan Salon dans lequel envoyer le message
	 */
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
	
	/**
	 * Une commande servant d'alias vers une autre.
	 * @author cyrielle
	 *
	 */
	public static class Alias extends BotCommand {
		
		/**
		 * La commande pointée.
		 */
		BotCommand target;
		
		/**
		 * 
		 * @param target La commande pointée
		 */
		public Alias(BotCommand target) {
			super();
			this.target = target;
		}
		
		/**
		 * Execute simplement la commande pointée en retransmettant les arguments de la méthode.
		 */
		@Override
		public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
			target.execute(bot, message, args);
		}
		
		
		
	}
	
	/**
	 * Une commande particulière dont le premier argument est un critère de recherche pour un écrit.
	 * La méthode à redéfinir est « process », « execute » servant désormais à récupérer l'écrit en question.
	 * @author cyrielle
	 *
	 */
	public static abstract class SearchCommand extends BotCommand {
		
		public SearchCommand() {
			super();
		}
		
		/**
		 * Exécute la commande avec l'écrit recherché.
		 * @param e L'écrit recherché.
		 * @param bot Une référence vers l'objet Critibot.
		 * @param message Le message executant la commande.
		 * @param args Les arguments de la commande.
		 */
		public abstract void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args);
		
		/**
		 * Récupère l'écrit demandé et l'envoie à process. Si plusieurs résultats ou aucun se sont trouvés,
		 * renvoie un message d'erreur et n'execute pas la commande.
		 */
		public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
			try {
				Vector<Ecrit> res = bot.searchEcrit(args[0]);
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
