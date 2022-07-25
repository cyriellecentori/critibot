package tk.cyriellecentori.CritiBot;


import java.util.List;
import java.util.Vector;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

/**
 * Décrit une commande du bot.
 * @author cyrielle
 *
 */
public abstract class BotCommand {

	public String name;
	public OptionData[] options;
	
	public static List<Command> commandes = null;
	
	public BotCommand(CritiBot bot, String name, String desc, OptionData... options) {
		this.name = name;
		this.options = options;
		JDA jda = bot.jda;
		Guild guild = jda.getGuildById(bot.beta ? 372635468786827265L : 547458908361588771L);
		if(commandes == null) {
			commandes = guild.retrieveCommands().complete();
		}
		boolean found = false;
		for(Command c : commandes) {
			if(c.getName().equals(name)) {
				c.editCommand().addOptions(options).queue();
				c.editCommand().setDescription(desc).queue();
				found = true;
			}
		}
		if(!found) {
			guild.upsertCommand(name, desc).addOptions(options).queue();
		}
	}
	
	public BotCommand() {
		
	}
	
	/**
	 * Décrit l'action de la commande lorsqu'elle est executée.
	 * @param bot Une référence vers l'objet Critibot
	 * @param message Le message ayant invoqué la commande
	 * @param args Les arguments de la commande.
	 */
	public abstract void execute(CritiBot bot, MessageReceivedEvent message, String[] args);
	
	/*
	 * Décrit l’action de la commande lorsqu’elle est executée par une commande slash.
	 */
	public abstract void slash(CritiBot bot, SlashCommandInteractionEvent event);
	
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
		public Alias(CritiBot bot, String name, BotCommand target) {
			super(bot, name, "Alias pour /" + target.name, target.options);
			this.target = target;
		}
		
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

		@Override
		public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
			target.slash(bot, event);
		}
		
		
		
	}
	
	/**
	 * Une commande particulière dont le premier argument est un critère de recherche pour un écrit.
	 * La méthode à redéfinir est « process », « execute » servant désormais à récupérer l'écrit en question.
	 * @author cyrielle
	 *
	 */
	public static abstract class SearchCommand extends BotCommand {
		
		/**
		 * Ne pas oublier de donner une option avec l’identifiant « ecrit » dans les options.
		 */
		public SearchCommand(CritiBot bot, String name, String desc, OptionData... options) {
			super(bot, name, desc, options);
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
		 * Exécute la commande avec l'écrit recherché.
		 * @param e L'écrit recherché.
		 * @param bot Une référence vers l'objet Critibot.
		 * @param event La commande slash ayant lancé la commande
		 */
		public abstract void processSlash(Ecrit e, CritiBot bot, SlashCommandInteractionEvent event);
		
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
		
		public void slash(CritiBot bot, SlashCommandInteractionEvent event) {
			OptionMapping id = event.getOption("recherche-id");
			boolean id_search = (id == null) ? false : id.getAsBoolean();
			
			if(id_search) {
				try {
					Ecrit e = Affichan.searchByHash(Integer.parseInt(event.getOption("ecrit").getAsString()), bot.getEcrits());
					if(e == null)
						event.reply("Cet écrit n’existe pas.").queue();
					else
						processSlash(e, bot, event);
				} catch(NumberFormatException e) {
					event.reply("Vous avez sélectionné la recherche par ID mais il est impossible de reconnaître un nombre.").queue();
				}
			} else {
					Vector<Ecrit> res;
					try {
						res = bot.searchEcrit(event.getOption("ecrit").getAsString());
					} catch(NullPointerException e) {
						event.reply("Cette commande doit être appelée avec l’argument « id » ou l’argument « ecrit », aucun des deux n’a été fourni.").queue();
						return;
					}
					if(res.size() > 1)
						event.reply("J'ai plus d'un résultat : il va falloir affiner le critère de recherche ou utiliser l’identifiant.").queue();
					else if(res.size() == 0)
						event.reply("Aucun résultat trouvé.").queue();
					else
						processSlash(res.firstElement(), bot, event);
			}
		}
		
	}
}
