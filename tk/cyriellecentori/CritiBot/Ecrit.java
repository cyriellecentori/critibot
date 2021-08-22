package tk.cyriellecentori.CritiBot;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import com.google.gson.Gson;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

/**
 * Représente un fil sur le forum.
 * @author cyrielle
 *
 */
public class Ecrit implements Cloneable{
	/**
	 * Définit la liste des statuts possibles pour un écrit.
	 * @author cyrielle
	 *
	 */
	public enum Status {
		OUVERT("Ouvert"),
		EN_ATTENTE("En attente"),
		ABANDONNE("Abandonné"),
		EN_PAUSE("En pause"),
		SANS_NOUVELLES("Sans nouvelles"),
		INCONNU("Inconnu"),
		PUBLIE("Publié"),
		RESERVE("Réservé"),
		VALIDE("Validé"),
		REFUSE("Refusé"),
		INFRACTION("Infraction");
		/**
		 * Équivalent du statut en chaîne de caractères.
		 */
		String nom;
		Status(String nom) {
			this.nom = nom;
		}
		
		public String toString() {
			return nom;
		}
		
		/**
		 * Renvoie le Statut correspondant à la chaîne de caractère transmise.
		 */
		public static Status getStatus(String str) {
			if(str.equalsIgnoreCase(OUVERT.nom)) {
				return OUVERT;
			} else if(str.equalsIgnoreCase(EN_ATTENTE.nom)) {
				return EN_ATTENTE;
			} else if(str.equalsIgnoreCase(EN_PAUSE.nom)) {
				return EN_PAUSE;
			} else if(str.equalsIgnoreCase(SANS_NOUVELLES.nom)) {
				return SANS_NOUVELLES;
			} else if(str.equalsIgnoreCase(INCONNU.nom)) {
				return INCONNU;
			} else if(str.equalsIgnoreCase(PUBLIE.nom)) {
				return PUBLIE;
			} else if(str.equalsIgnoreCase(RESERVE.nom)) {
				return RESERVE;
			} else if(str.equalsIgnoreCase(VALIDE.nom)){
				return VALIDE;
			} else if(str.equalsIgnoreCase(REFUSE.nom)) {
				return REFUSE;
			} else if(str.equalsIgnoreCase(ABANDONNE.nom)) {
				return ABANDONNE;
			} else if(str.equalsIgnoreCase(INFRACTION.nom)) {
				return INFRACTION;
			} else {
				return INCONNU;
			}
		}
	}
	
	/**
	 * Définit la liste des types possibles pour les écrits.
	 * @author cyrielle
	 *
	 */
	public enum Type {
		CONTE("Conte"),
		IDEE("Idée"),
		RAPPORT("Rapport"),
		AUTRE("Autre");
		/**
		 * Équivalent en chaîne de caractère du type.
		 */
		public final String nom;
		
		Type(String nom) {
			this.nom = nom;
		}
		
		public String toString() {
			return nom;
		}
		
		/**
		 * Renvoie le Type correspondant à la chaîne de caractère transmise.
		 */
		public static Type getType(String str) {
			if(str.equalsIgnoreCase(CONTE.nom)) {
				return CONTE;
			} else if(str.equalsIgnoreCase(IDEE.nom)) {
				return IDEE;
			} else if(str.equalsIgnoreCase(RAPPORT.nom)) {
				return RAPPORT;
			} else {
				return AUTRE;
			}
		}
	}
	
	/**
	 * Nom de l'écrit.
	 */
	private String nom = "";
	/**
	 * Lien forum du fil de l'écrit.
	 */
	private String lien = "";
	private Type type = null;
	private Status status = null;
	/**
	 * Statut précédent de l'écrit, pour pouvoir le remettre après la fin d'une réservation.
	 */
	private Status old = null;
	/**
	 * Identifiant Discord de l'utilisateur ayant réservé l'écrit, 0 s'il n'y a pas de réservation.
	 */
	private long reservation = 0;
	/**
	 * Nom de la personne ayant réservé l'écrit.
	 */
	private String resName = "";
	/**
	 * Date de la réservation.
	 */
	private long resDate = 0;
	/**
	 * Date de la dernière mise à jour de l'écrit.
	 */
	private long lastUpdate = 0;
	/**
	 * Auteur de l'écrit.
	 */
	private String auteur = "";
	
	/**
	 * Message de statut de l'écrit.
	 */
	private BotMessage statusMessage = new BotMessage();
	
	Ecrit(String nom, String lien, Type type, Status status, String auteur) {
		this.nom = nom;
		this.lien = lien;
		this.type = type;
		this.status = status;
		this.auteur = auteur;
		lastUpdate = System.currentTimeMillis();
	}
	
	/**
	 * Vérifie si certains champs ne sont pas `null` pour éviter des NullPointerException.
	 */
	public void check() {
		if(nom == null)
			nom = "";
		if(lien == null)
			lien = "";
		if(auteur == null)
			auteur = "";
	}
	
	/**
	 * Change de message de statut.
	 * @param message
	 * @return `true` si le changement est réussi, `false` sinon.
	 */
	public boolean setStatusMessage(Message message) {
		boolean change = true;
		if(statusMessage.isInitialized()) {
			change = false;
		}
		if(change) {
			this.statusMessage = new BotMessage(message);
		}
		return change;
	}
	
	/**
	 * Supprime le message de statut.
	 */
	public void removeStatusMessage() {
		statusMessage.getMessage().delete().complete();
		statusMessage = new BotMessage();
	}
	
	public BotMessage getStatusMessage() {
		return statusMessage;
	}
	
	/**
	 * Récupère le lien API vers le message de statut, si disponible.
	 * @param jda
	 */
	public void check(JDA jda) {
		if(lastUpdate == 0L)
			lastUpdate = System.currentTimeMillis();
		if(statusMessage != null) {
			if(!statusMessage.isInitialized())
				statusMessage.retrieve(jda);
		} else {
			statusMessage = new BotMessage();
		}
	}
	
	public String toString() {
		return nom + " — " + type + " — " + status + " — " + lien;
	}
	
	/**
	 * Réserve un écrit.
	 * @param member Membre du Discord tentant de réserver l'écrit.
	 * @return `true` si la réservation à réussi.
	 */
	public boolean reserver(Member member) {
		// Vérifie si l'écrit est réservable.
		if(!(status == Status.OUVERT || status == Status.ABANDONNE || status == Status.EN_PAUSE || status == Status.INCONNU || status == Status.VALIDE || status == Status.SANS_NOUVELLES))
			return false;
		// Remplit les informations de réservation.
		resDate = System.currentTimeMillis();
		reservation = member.getIdLong();
		old = status;
		resName = member.getEffectiveName();
		status = Status.RESERVE;
		return true;
	}
	
	/**
	 * Résere un écrit.
	 * @param name Nom de l'utilisation réservant l'écrit.
	 * @return `true` si la réservation à réussi.
	 */
	public boolean reserver(String name) {
		// Vérifie si l'écrit est réservable.
		if(!(status == Status.OUVERT || status == Status.ABANDONNE || status == Status.EN_PAUSE || status == Status.INCONNU || status == Status.VALIDE || status == Status.SANS_NOUVELLES))
			return false;
		// Remplit les informations de réservation.
		resDate = System.currentTimeMillis();
		reservation = 0;
		old = status;
		resName = name;
		status = Status.RESERVE;
		return true;
	}
	
	/**
	 * Libère la réservation d'un écrit.
	 * @param member Membre Discord essayant de libérer l'écrit.
	 * @return `true` si la libération à réussi.
	 */
	public boolean liberer(Member member) {
		if(status != Status.RESERVE)
			return true; // Rien ne sert de libérer si l'écrit n'est pas réservé.
		try {
			if(reservation == 0L) { // La réservation n'est que par un nom : libération automatique.
				resName = "";
				status = old;
				return true;
			  // Sinon, vérifie que la personne souhaitant libérer est la personne ayant réservé,
			  // un membre de l'équipe critique ou que la réservation est plus vieille que trois jours.
			} else if(member.getUser().getIdLong() == reservation 
					|| member.getRoles().contains
					(member.getGuild().getRoleById(612383955253067963L)) || resDate > 3*24*3600*1000) {
				reservation = 0;
				resName = "";
				status = old;
				return true;
			} else
				return false;
		} catch(NullPointerException e) { // En gros le null ça veut dire tkt fais-moi confiance bro
			reservation = 0;
			resName = "";
			status = old;
			return true;
		}
	}
	
	public String getReservation(Guild guild) {
			return resName;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public String getNom() {
		return nom;
	}
	
	/**
	 * Vérifie si l'écrit correspond aux critères demandés.
	 * @param type Type demandé, `null` pour tout type.
	 * @param status Statut demandé, `null` pour tout statut.
	 * @return si l'écrit correspond aux critères demandés
	 */
	public boolean complyWith(Type type, Status status) {
		boolean ret = true;
		if(type != null)
			ret = ret && this.type == type;
		if(status != null)
			ret = ret && this.status == status;
		return ret;
	}
	
	/**
	 * 
	 * @return `true` si l'écrit est abandonné, publié ou refusé.
	 */
	public boolean isDead() {
		return status == Status.ABANDONNE || status == Status.PUBLIE || status == Status.REFUSE;
	}
	
	/**
	 * Change le statut de l'écrit.
	 * @param status
	 * @return `false` si le changement n'a pas pu avoir lieu.
	 */
	public boolean setStatus(Status status) {
		// Protection contre la réservation indirecte, utiliser reserver()
		if(this.status == Status.RESERVE)
			return false;
		else if(status == Status.RESERVE)
			return false;
		if(status != Status.SANS_NOUVELLES) // Mise à jour de la date de dernière modification, sauf si on indique l'écrit comme étant sans nouvelles.
			lastUpdate = System.currentTimeMillis();
		this.status = status;
		if(this.statusMessage.isInitialized()) // Met à jour le message de statut si possible.
			this.statusMessage.getMessage().editMessage(this.toEmbed()).queue();
		return true;
	}
	
	public void setType(Type type) {
		this.type = type;
		if(this.statusMessage.isInitialized()) // Met à jour le message de statut si possible.
			this.statusMessage.getMessage().editMessage(this.toEmbed()).queue();
	}
	
	public void rename(String newName) {
		this.nom = newName;
	}
	
	/**
	 * Transforme une idée en rapport (utile pour les validations d'idées).
	 */
	public void promote() {
		if(type == Type.IDEE)
			type = Type.RAPPORT;
	}
	
	/**
	 * @return la date de réservation correctement formatée.
	 */
	public String getResDate() {
		return new SimpleDateFormat("dd MMM yyyy à HH:mm").format(new Date(resDate));
	}
	
	/**
	 * @return la date de dernière mise à jour correctement formatée.
	 */
	public String getLastUpdate() {
		return new SimpleDateFormat("dd MMM yyyy à HH:mm").format(new Date(lastUpdate));
	}
	
	/**
	 * Vérifie si l'écrit est plus vieux que la date demandée.
	 * @param time date demandée
	 */
	public boolean olderThan(long time) {
		return time > lastUpdate;
	}
	
	
	public Ecrit clone() {
		return new Gson().fromJson(new Gson().toJson(this), this.getClass());
		
	}
	
	/**
	 * Indique l'écrit comme critiqué.
	 * @param u Membre ayant critiqué.
	 */
	public boolean critique(Member u) {
		lastUpdate = System.currentTimeMillis();
		boolean b = liberer(u);
		if(!b) {
			resName = "";
			reservation = 0;
		}
		status = Status.EN_ATTENTE;
		return b;
	}
	
	public long getResId() {
		return reservation;
	}
	
	public String getLien() {
		return lien;
	}
	
	/**
	 * Retourne un embed décrivant l'écrit.
	 */
	public MessageEmbed toEmbed() {
		EmbedBuilder embed = new EmbedBuilder();
		embed.setTitle(nom, lien);
		embed.addField("Type", type.toString(), false);
		if(status == Status.RESERVE) {
			embed.addField("Statut", status.toString() + " par " + resName, false);
		} else
			embed.addField("Statut", status.toString(), false);
		embed.setFooter("Dernière modification");
		embed.setAuthor(auteur);
		embed.setTimestamp(Instant.ofEpochMilli(lastUpdate));
		switch(type) {
		case AUTRE:
			embed.setColor(Color.WHITE);
			break;
		case CONTE:
			embed.setColor(0x008000);
			break;
		case IDEE:
			embed.setColor(0xDF7401);
			break;
		case RAPPORT:
			embed.setColor(0x01A9DB);
			break;
		default:
			break;
		
		}
		return embed.build();
	}
	
	public Type getType() {
		return type;
	}
	
	public String getAuteur() {
		return auteur;
	}
	
	public void setAuteur(String auteur) {
		this.auteur = auteur;
		if(this.statusMessage.isInitialized())
			this.statusMessage.getMessage().editMessage(this.toEmbed()).queue();
	}
}
