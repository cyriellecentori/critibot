package tk.cyriellecentori.CritiBot;

import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
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
				System.out.println("Message " + m.getEmbeds().get(0).getTitle() + " trop ancien.");
				m.delete().queue();
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
				mes.get(i).editMessage(ecr.get(i).toEmbed()).queue();
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
	
	/**
	 * Met à jour les références vers les écrits lorsqu'une modification a été annulée.
	 * 
	 * Puisque l'annulation remplace la base de donnnées par une copie antérieure de celle-ci,
	 * tous les écrits mis dans les vecteurs des affichans sont des références vers les écrits annulés.
	 * Cette méthode remplace les références annulées par les nouvelles.
	 */
	public void updateRefs(Vector<Ecrit> ecrits) {
		for(Ecrit e : ecr) {
			e = searchByHash(e.hashCode(), ecrits);
		}
	}
	
	private static String checkMark = "U+2705";
	
	private Message sendMessage(CritiBot bot, Ecrit e) {
		Message m = chan.sendMessage(e.toEmbed()).complete();
		m.addReaction(bot.jda.getEmoteById(bot.henritueur)).queue();
		m.addReaction(bot.jda.getEmoteById(bot.henricheck)).queue();
		if(e.getType() == Type.IDEE)
			m.addReaction(bot.jda.getEmoteById(bot.henricross)).queue();
		if(e.getStatus() == Status.INFRACTION)
			m.addReaction(checkMark).queue();
		m.addReaction(bot.unlock).queue();
		return m;
	}
	
	public void reactionAdd(CritiBot bot, MessageReactionAddEvent mrae) {
		Ecrit e = null;
		for(int i = 0; i < mes.size(); i++) { // Cherche l'écrit correspondant au message
			if(mes.get(i).getIdLong() == mrae.getMessageIdLong())
				e = ecr.get(i);
		}
		if(e == null)
			return;
		
		if(mrae.getReactionEmote().isEmoji()) { // Vérifie les actions pour les emojis
			// Actions de libération de la réservation
			if(mrae.getReactionEmote().getAsCodepoints().equals(bot.unlock)) {
				bot.archiver();
				e.liberer(mrae.getMember());
			} else if(mrae.getReactionEmote().getAsCodepoints().equals(checkMark) && e.getStatus() == Status.INFRACTION) {
				bot.archiver();
				e.setStatus(Status.OUVERT);
			}
		} else { // Vérifie les actions pour les emotes
			if(mrae.getReactionEmote().getEmote().getIdLong() == bot.henritueur) { // Interêt
				bot.archiver();
				e.marquer(mrae.getMember());
			} else if(mrae.getReactionEmote().getEmote().getIdLong() == bot.henricross && e.getType() == Type.IDEE) { // Refus d'une idée
				bot.archiver();
				e.setStatus(Status.REFUSE);
				bot.jda.getTextChannelById(bot.organichan).sendMessage("« " + e.getNom() + " » refusé !").queue();
			} else if(mrae.getReactionEmote().getEmote().getIdLong() == bot.henricheck) { // Idée indiquée comme critiquée
				bot.archiver();
				bot.jda.getTextChannelById(bot.organichan).sendMessage("« " + e.getNom() + " » critiqué !").queue();
				e.critique();
			} 
		}
		// Met à jour les messages
		bot.updateOpen();
		try { // Essaye de sauvegarder
			bot.save();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	
}
