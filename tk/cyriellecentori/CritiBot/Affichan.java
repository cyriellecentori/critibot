package tk.cyriellecentori.CritiBot;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
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
	 * Les messages avec l'un de ces status seront affichés.
	 */
	public final Status[] status;
	/**
	 * Les messages de l'un de ces types seront affichés.
	 */
	public final Type[] types;
	
	public Affichan(long chanID, Status[] status, Type[] types) {
		this.chanID = chanID;
		this.status = status;
		this.types = types;
	}
	
	public static Ecrit searchByHash(int hash, Vector<Ecrit> ecrits) {
		for(Ecrit e : ecrits) {
			if(ecrits.hashCode() == hash) {
				return e;
			}
		}
		return null;
	}
	
	public boolean initialize(JDA jda, Vector<Ecrit> ecrits) throws InterruptedException, ExecutionException {
		chan = jda.getTextChannelById(chanID);
		if(chan == null) {
			return false;
		}
		// Récupération des messages du bot contenant des embeds
		List<Message> embeds = chan.getIterableHistory().takeAsync(200).thenApply(list -> list.stream()
				.filter(m -> m.getAuthor().equals(jda.getSelfUser()))
				.filter(m -> !m.getEmbeds().isEmpty())
				.collect(Collectors.toList())).get();
		System.out.println("Messages collectés : " + embeds.size());
		// Récupération des différents écrits correspondants aux messages
		for(Message m : embeds) {
			int hash = 0;
			try {
				hash = Integer.parseInt(m.getEmbeds().get(0).getFooter().getText());
				Ecrit e = searchByHash(hash, ecrits);
				if(e != null) {
					mes.add(m);
					ecr.add(e);
				} else {
					System.out.println("Aucun écrit correspondant au hashcode " + hash + " (nom : " + m.getEmbeds().get(0).getTitle() + ").");
				}
				
			} catch(NumberFormatException e) {
				System.out.println("Message " + m.getEmbeds().get(0).getTitle() + " trop ancien.");
				m.delete().queue();
			}
			
		}
		return true;
	}
	
	public static <T> boolean contains(T[] tab, T e) {
		for(T i : tab) {
			if(i.equals(e))
				return true;
		}
		return false;
	}
	
	public void update(JDA jda, Vector<Ecrit> ecrits) {
		// Vérification des écrits actuellement présents
		Vector<Ecrit> toDel = new Vector<Ecrit>();
		for(Ecrit e : ecr) {
			if(status != null)
				if(!contains(status, e.getStatus())) {
					toDel.add(e);
				}
			if(types != null)
				if(!contains(types, e.getType())) {
					toDel.add(e);
				}
		}
		for(Ecrit del : toDel) {
			ecr.remove(del);
		}
		
		//Vérification d'écrits non présents actuellement
		for(Ecrit e : ecrits) {
			boolean add = true;
			if(status != null)
				add = add && contains(status, e.getStatus());
			if(types != null)
				if(!contains(types, e.getType()))
					add = add && contains(types, e.getType());
			add = add && !ecr.contains(e);
			if(add) {
				
			}
		}
		
	}
	
}
