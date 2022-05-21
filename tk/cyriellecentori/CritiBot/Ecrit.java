package tk.cyriellecentori.CritiBot;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Vector;

import com.google.gson.Gson;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

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
		VALIDE("Validé"),
		REFUSE("Refusé"),
		INFRACTION("Infraction"),
		OUVERT_PLUS("Ouvert*");
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
		
		public static String[] names = {
			"Ouvert", "En attente", "Abandonné", "En pause", "Sans nouvelles", "Inconnu", "Publié",
			"Validé", "Refusé", "Infraction", "Ouvert*"
		};
		
		/**
		 * Renvoie le Statut correspondant à la chaîne de caractère transmise.
		 */
		public static Status getStatus(String rawStr) {
			String str = rawStr.strip();
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
			} else if(str.equalsIgnoreCase(VALIDE.nom)){
				return VALIDE;
			} else if(str.equalsIgnoreCase(REFUSE.nom)) {
				return REFUSE;
			} else if(str.equalsIgnoreCase(ABANDONNE.nom)) {
				return ABANDONNE;
			} else if(str.equalsIgnoreCase(INFRACTION.nom)) {
				return INFRACTION;
			} else if(str.equalsIgnoreCase(OUVERT_PLUS.nom)) {
				return OUVERT_PLUS;
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
		FORMAT_GDI("Format GdI"),
		AUTRE("Autre");
		/**
		 * Équivalent en chaîne de caractère du type.
		 */
		public final String nom;
		
		Type(String nom) {
			this.nom = nom;
		}
		
		public static String[] names = {
			"Conte", "Idée", "Rapport", "Format GdI", "Autre"
		};
		
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
			} else if(str.equalsIgnoreCase(FORMAT_GDI.nom)) {
				return FORMAT_GDI;
			} else {
				return AUTRE;
			}
		}
	}
	
	public class Interet {
		public final String name;
		public final long date;
		
		public Interet(String name, long date) {
			this.name = name;
			this.date = date;
		}

		public String getDate() {
			return new SimpleDateFormat("dd MMM yyyy à HH:mm").format(new Date(date));
		}
	}
	
	public class InteretMembre extends Interet {
		public final long member;
		
		public InteretMembre(Member member, long date) {
			super(member.getEffectiveName(), date);
			this.member = member.getIdLong();
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
	 * Date de la dernière mise à jour de l'écrit.
	 */
	private long lastUpdate = 0;
	/**
	 * Auteur de l'écrit.
	 */
	private String auteur = "";
	/**
	 * Personnes ayant manifesté de l'intérêt pour critiquer cet écrit.
	 */
	public Vector<Interet> interesses = new Vector<Interet>();
	
	/*
	 * Drapeau identifiant l'écrit comme modifié, permettant donc aux affichans de modifier son message.
	 */
	public boolean edited = false;
	
	/*
	 * Tableau dynamique contenant les tags associés à l'écrit.
	 */
	public Vector<String> tags = new Vector<String>();
	
	Ecrit(String nom, String lien, Type type, Status status, String auteur) {
		this.nom = nom;
		this.lien = lien;
		this.type = type;
		if(status == Status.OUVERT_PLUS)
			status = Status.OUVERT;
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
		if(interesses == null)
			interesses = new Vector<Interet>();
		if(tags == null)
			tags = new Vector<String>();
	}
	
	public String toString() {
		return nom + " — " + type + " — " + status + " — " + lien;
	}
	
	public Vector<String> getTags() {
		return tags;
	}
	
	public boolean removeTag(String searchTag) {
		Vector<Integer> res = CritiBot.search(searchTag, tags);
		if(res.size() != 1)
			return false;
		tags.remove(res.get(0).intValue());
		edited = true;
		return true;
	}
	
	public boolean addTag(String tag) {
		for(int index : CritiBot.search(tag, tags)) {
			if(tags.get(index).equals(tag)) {
				return false;
			}
		}
		tags.add(tag);
		edited = true;
		return true;
	}
	
	public boolean hasTag(String tag) {
		return !CritiBot.search(tag, tags).isEmpty();
	}
	
	/**
	 * Marque un écrit comme cible d'intérets.
	 * @param member Membre du Discord interessé par l'écrit.
	 * @return `true` s'il a été possible de marquer l'intérêt.
	 */
	public boolean marquer(Member member) {
		// Vérifie si l'écrit est réservable.
		if(!(status == Status.OUVERT || status == Status.OUVERT_PLUS))
			return false;
		// Remplit les informations de réservation.
		for(Interet i : interesses) {
			if(i instanceof InteretMembre) {
				if(((InteretMembre) i).member == member.getIdLong()) {
					return false;
				}
			}
		}
		Interet i = new InteretMembre(member, System.currentTimeMillis());
		interesses.add(i);
		status = Status.OUVERT_PLUS;
		edited = true;
		return true;
	}
	
	/**
	 * Marque un écrit comme cible d'intérets.
	 * @param member Nom de l'utilisateur interessé par l'écrit.
	 * @return `true` s'il a été possible de marquer l'intérêt.
	 */
	public boolean marquer(String name) {
		// Vérifie si l'écrit est marquable.
		if(status != Status.OUVERT && status != Status.OUVERT_PLUS)
			return false;
		// Remplit les informations de marquage.
		Interet i = new Interet(name, System.currentTimeMillis());
		interesses.add(i);
		status = Status.OUVERT_PLUS;
		edited = true;
		return true;
	}
	
	/**
	 * Libère l'intérêt d'un écrit.
	 * @param member Membre Discord libérant son interêt sur l'écrit.
	 * @return `true` si la libération à réussi.
	 */
	public boolean liberer(String name) {
		Interet toDel = null;
		for(Interet i : interesses) {
			if(i.name.equals(name))
				toDel = i;
		}
		if(toDel != null)
			interesses.remove(toDel);
		if(interesses.size() == 0) {
			status = Status.OUVERT;
		}
		edited = true;
		return (toDel != null);
	}
	
	/**
	 * Libère l'intérêt d'un écrit.
	 * @param member Membre Discord libérant son interêt sur l'écrit.
	 * @return `true` si la libération à réussi.
	 */
	public boolean liberer(Member member) {
		Interet toDel = null;
		for(Interet i : interesses) {
			if(i instanceof InteretMembre) {
				if(((InteretMembre) i).member == member.getIdLong()) {
					toDel = i;
				}
			}
		}
		if(toDel == null)
			return liberer(member.getEffectiveName());
		interesses.remove(toDel);
		if(interesses.size() == 0) {
			status = Status.OUVERT;
		}
		edited = true;
		return true;
	}
	
	
	public Vector<Interet> getInteresses() {
			return interesses;
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
		if(status == Status.OUVERT_PLUS)
			return false;
		if(status != Status.SANS_NOUVELLES) // Mise à jour de la date de dernière modification, sauf si on indique l'écrit comme étant sans nouvelles.
			lastUpdate = System.currentTimeMillis();
		if(this.status == Status.OUVERT_PLUS) {
			deleteInteret();
		}
		this.status = status;
		edited = true;
		return true;
	}
	
	public void deleteInteret() {
		interesses.clear();
	}
	
	public void setType(Type type) {
		this.type = type;
		edited = true;
	}
	
	public void rename(String newName) {
		this.nom = newName;
		edited = true;
	}
	
	/**
	 * Transforme une idée en rapport (utile pour les validations d'idées).
	 */
	public void promote() {
		if(type == Type.IDEE)
			type = Type.RAPPORT;
		edited = true;
	}
	
	/**
	 * @return la date de dernière mise à jour correctement formatée.
	 */
	public String getLastUpdate() {
		return new SimpleDateFormat("dd MMM yyyy à HH:mm").format(new Date(lastUpdate));
	}
	
	/**
	 * @return la date de dernière mise à jour en format numérique.
	 */
	public long getLastUpdateLong() {
		return lastUpdate;
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
	public void critique() {
		lastUpdate = System.currentTimeMillis();
		deleteInteret();
		status = Status.EN_ATTENTE;
		edited = true;
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
		embed.addField("Statut", status.toString(), false);
		if(status == Status.OUVERT_PLUS) {
			String interesList = "";
			for(Interet i : interesses) {
				interesList += i.name + " le " + i.getDate() + "\n";
			}
			embed.addField("Marques d'intérêt", interesList, false);
		}
		if(!tags.isEmpty()) {
			String total = "";
			for(String tag : tags) {
				total += tag + "\n";
			}
			embed.addField("Tags", total, false);
		}
		embed.setFooter(String.valueOf(hashCode()));
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
		case FORMAT_GDI:
			embed.setColor(0xAE1FF1);
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
		edited = true;
	}

	
	
	public String forumID() {
		return lien.substring(7).split("/")[2];
	}
	
	// Retourne une version int de l’indentifiant forum
	public static int fIDtoInt(String id) {
		return Integer.parseInt(id.substring(2));
	}
	
	@Override
	public int hashCode() {
		return fIDtoInt(forumID());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Ecrit other = (Ecrit) obj;
		if (lien == null) {
			if (other.lien != null)
				return false;
		} else if (!lien.equals(other.lien))
			return false;
		return true;
	}
}
