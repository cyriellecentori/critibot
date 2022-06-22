package tk.cyriellecentori.CritiBot;

import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import tk.cyriellecentori.CritiBot.Ecrit.Status;
import tk.cyriellecentori.CritiBot.Ecrit.Type;

/**
 * Classe représentant un salon où s'afficheront les écrits ayant le status et le type demandés.
 * 
 */
public class Affichan {
	/**
	 * L'identifiant du salon.
	 */
	public final long chanID;
	/**
	 * 
	 */
	private TextChannel chan;
	/**
	 * La liste des messages.
	 */
	private Vector<Message> mes = new Vector<Message>();
	/**
	 * La liste des écrits, aux mêmes numéros que les messages.
	 */
	private Vector<Ecrit> ecr = new Vector<Ecrit>();
	/**
	 * Les messages avec l'un de ces tags seront affichés.
	 */
	private final String[] tags;
	/**
	 * Les messages avec l'un de ces status seront affichés.
	 */
	public final Status[] status;
	/**
	 * Les messages de l'un de ces types seront affichés.
	 */
	public final Type[] types;
	
	public Affichan(long chanID, Status[] status, Type[] types, String[] tags) {
		this.chanID = chanID;
		this.status = status;
		this.types = types;
		this.tags = tags;
	}
	
	public static Ecrit searchByHash(int hash, Vector<Ecrit> ecrits) {
		for(Ecrit e : ecrits) {
			if(e.hashCode() == hash) {
				return e;
			}
		}
		return null;
	}
	
	/**
	 * Initialise la liste des écrits et la récupère à partir de l'historique des messages.
	 */
	public boolean initialize(CritiBot bot) throws InterruptedException, ExecutionException {
		Vector<Ecrit> ecrits = bot.getEcrits();
		
		chan = bot.jda.getTextChannelById(chanID);
		if(chan == null) {
			System.err.println("Erreur : salon d'identifiant " + chanID + " non trouvé.");
			return false;
		}
		// Récupération des messages du bot contenant des embeds
		List<Message> embeds = chan.getIterableHistory().takeAsync(200).thenApply(list -> list.stream()
				.filter(m -> m.getAuthor().equals(bot.jda.getSelfUser()))
				.filter(m -> !m.getEmbeds().isEmpty())
				.collect(Collectors.toList())).get();
		System.out.println("Messages collectés : " + embeds.size());
		// Récupération des différents écrits correspondants aux messages
		for(Message m : embeds) {
			int hash = 0;
			try {
				hash = Integer.parseInt(m.getEmbeds().get(0).getFooter().getText());
				Ecrit e = searchByHash(hash, ecrits);
				if(e != null && !ecr.contains(e)) {
					mes.add(m);
					ecr.add(e);
				} else if(ecr.contains(e)) {
					System.out.println("Message en trop ! Suppression.");
					m.delete().queue();
				} else {
					System.out.println("Aucun écrit correspondant au hashcode " + hash + " (nom : " + m.getEmbeds().get(0).getTitle() + ").");
				}
				
			} catch(NumberFormatException e) {
				System.out.println("Message " + m.getEmbeds().get(0).getTitle() + " n'est pas correct.");
			} catch(NullPointerException e) {
				
			}
			
		}
		update(bot);
		return true;
	}
	
	public static <T> boolean contains(T[] tab, T e) {
		for(T i : tab) {
			if(i.equals(e))
				return true;
		}
		return false;
	}
	
	/**
	 * Met à jour les écrits du salon.
	 * 
	 * Vérifie si les écrits actuels correspondent encore aux critères, si des écrits ont été modifiés et si de nouveaux écrits doivent être ajoutés.
	 */
	public void update(CritiBot bot) {
		Vector<Ecrit> ecrits = bot.getEcrits();
		
		// Vérification des écrits actuellement présents
		Vector<Integer> toDel = new Vector<Integer>();
		for(int i = 0; i < mes.size(); i++) {
			boolean delete = false;
			if(!ecrits.contains(ecr.get(i))) { // Si l’écrit a été supprimé
				delete = true;
			}
			if(status != null && !delete)
				if(!contains(status, ecr.get(i).getStatus())) {
					delete = true;
				}
			if(types != null && !delete)
				if(!contains(types, ecr.get(i).getType())) {
					delete = true;
				}
			if(tags != null && !delete) {
				boolean hasTag = false;
				for(String tag : tags)
					if(ecr.get(i).hasTag(tag))
						hasTag = true;
				delete = !hasTag;
			}
			if(delete) // Supprime l'écrit s'il ne correspond plus aux critères
					toDel.add(i);
			else if(ecr.get(i).edited){ // Sinon, vérifie s'il a été modifié
				mes.get(i).editMessageEmbeds(ecr.get(i).toEmbed()).queue(); // Met à jour l’embed
				mes.get(i).editMessageComponents(ActionRow.of(getActionRow(ecr.get(i)))).queue(); // Met à jour les boutons
			}
		}
		for(int i = toDel.size() - 1; i >= 0; i--) {
			ecr.remove((int) toDel.get(i));
			Message mmm = mes.get((int) toDel.get(i));
			mes.remove((int) toDel.get(i));
			mmm.delete().queue();
		}
		
		// Vérification d'écrits non présents actuellement
		for(Ecrit e : ecrits) {
			boolean add = true;
			if(status != null)
				add = add && contains(status, e.getStatus());
			if(types != null)
				if(!contains(types, e.getType()))
					add = add && contains(types, e.getType());
			if(tags != null) {
				boolean hasTag = false;
				for(String tag : tags) {
					if(e.hasTag(tag)) {
						hasTag = true;
					}
				}
				add = add && hasTag;
			}
			add = add && !ecr.contains(e);
			if(add) {
				ecr.add(e);
				mes.add(sendMessage(bot, e));
			}
		}
		
		
		
	}
	
	/**
	 * Supprime tous les messages d'écrits du salon.
	 */
	public void purge(JDA jda) {
		for(Message m : mes) {
			try {
				m.delete().queue();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		mes = new Vector<Message>();
		ecr = new Vector<Ecrit>();
	}
	
	/**
	 * Vérifie si un message supprimé est l'un des messages contenus dans le salon.
	 * 
	 * Si un message d'écrit est supprimé dans le salon, il est tout de suite remplacé.
	 */
	public void checkDeletion(CritiBot bot, MessageDeleteEvent mde) {
		for(int i = 0; i < mes.size(); i++) {
			if(mes.get(i).getIdLong() == mde.getMessageIdLong()) {
				mes.set(i, sendMessage(bot, ecr.get(i)));
			}
		}
	}
	
	/**
	 * Supprime et réaffiche un message d'écrit pour le remettre en avant.
	 * 
	 * Le message supprimé sera directement réaffiché par checkDeletion.
	 * Si le salon ne contient pas le message, rien ne se passe.
	 */
	public void up(Ecrit e) {
		if(ecr.contains(e)) {
			int index = ecr.indexOf(e);
			mes.get(index).delete().queue();
		}
	}
	
	private static String checkMark = "U+2705";
	
	/*
	 * Retourne les boutons appropriés pour un écrit.
	 */
	public static Vector<Button> getActionRow(Ecrit e) {
		Button marque = Button.primary("e" + e.forumID() + "-m", "Marquer");
		Button critique = Button.success("e" + e.forumID() + "-c", "Critiqué");
		Button refus = Button.danger("e" + e.forumID() + "-r", "Refusé");
		Button retirer = Button.secondary("e" + e.forumID() + "-d", "Retirer marque");
		Button up = Button.success("e" + e.forumID() + "-u", "Up");
		Button pubile = Button.success("e" + e.forumID() + "-p", "Publié");
		Button no = Button.primary("e" + e.forumID() + "-0", "Aucune action possible").asDisabled();
		Vector<Button> buttons = new Vector<Button>();
		
		if(e.getStatus() == Status.OUVERT || e.getStatus() == Status.OUVERT_PLUS) {
			buttons.add(marque);
			buttons.add(critique);
		}
		if(e.getStatus() == Status.INFRACTION || e.getStatus() == Status.EN_ATTENTE || e.getStatus() == Status.SANS_NOUVELLES || e.getStatus() == Status.EN_PAUSE || e.getStatus() == Status.INCONNU) {
			buttons.add(up);
		}
		if((e.getType() == Type.RAPPORT || e.getType() == Type.IDEE) && e.getStatus() != Status.REFUSE) {
			buttons.add(refus);
		}
		if(e.getStatus() == Status.OUVERT || e.getStatus() == Status.OUVERT_PLUS) {
			buttons.add(retirer);
		}
		if(e.getStatus() == Status.VALIDE) {
			buttons.add(pubile);
		}
		if(buttons.isEmpty()) {
			buttons.add(no);
		}
		return buttons;
	}
	
	/*
	 * Crée une action à compléter envoyant un message de description de l’écrit avec les boutons appropriés.
	 */
	public static MessageAction sendMessageWithActions(Ecrit e, TextChannel chan) {
		return chan.sendMessageEmbeds(e.toEmbed()).setActionRow(getActionRow(e));
	}
	
	public static WebhookMessageAction<Message> sendMessageWithActions(Ecrit e, InteractionHook hook) {
		return hook.sendMessageEmbeds(e.toEmbed()).addActionRow(getActionRow(e));

	}
	
	private Message sendMessage(CritiBot bot, Ecrit e) {
		return sendMessageWithActions(e, chan).complete();
	}
	
	/**
	 * Met à jour les références vers les écrits lorsqu'une modification a été annulée.
	 * 
	 * Puisque l'annulation remplace la base de donnnées par une copie antérieure de celle-ci,
	 * tous les écrits mis dans les vecteurs des affichans sont des références vers les écrits annulés.
	 * Cette méthode remplace les références annulées par les nouvelles.
	 */
	public void updateRefs(Vector<Ecrit> ecrits) {
		Vector<Integer> toDel = new Vector<Integer>();
		for(int i  = 0; i < this.ecr.size(); i++) {
			Ecrit newPointer = Affichan.searchByHash(ecr.get(i).hashCode(), ecrits);
			if(newPointer == null)
				toDel.add(i);
			else
				ecr.set(i, newPointer);
		}
		for(int i = toDel.size() - 1; i >= 0; i--) {
			ecr.remove((int) toDel.get(i));
			Message mmm = mes.get((int) toDel.get(i));
			mes.remove((int) toDel.get(i));
			mmm.delete().queue();
		}
	}
	
	/**
	 * Supprime l’écrit de la base de données de l’affichan.
	 */
	public void remove(Ecrit e) {
		for(int i = 0; i < ecr.size(); i++) {
			if(e == ecr.get(i)) { // True equality, since there is no copy it should reference the same object
				ecr.remove(i);
				mes.get(i).delete().queue();
				return;
			}
		}
	}
	
	
}
