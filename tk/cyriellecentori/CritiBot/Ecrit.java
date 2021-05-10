package tk.cyriellecentori.CritiBot;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.Gson;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class Ecrit implements Cloneable{
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
		REFUSE("Refusé");
		String nom;
		Status(String nom) {
			this.nom = nom;
		}
		
		public String toString() {
			return nom;
		}
		
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
			} else {
				return INCONNU;
			}
		}
	}
	
	public enum Type {
		CONTE("Conte"),
		IDEE("Idée"),
		RAPPORT("Rapport"),
		AUTRE("Autre");
		public final String nom;
		
		Type(String nom) {
			this.nom = nom;
		}
		
		public String toString() {
			return nom;
		}
		
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
	
	private String nom = "";
	private String lien = "";
	private Type type = null;
	private Status status = null;
	private Status old = null;
	private long reservation = 0;
	private String resName = "";
	private long resDate = 0;
	private long lastUpdate = 0;
	
	Ecrit(String nom, String lien, Type type, Status status) {
		this.nom = nom;
		this.lien = lien;
		this.type = type;
		this.status = status;
		lastUpdate = System.currentTimeMillis();
	}
	
	public void check() {
		if(lastUpdate == 0L)
			lastUpdate = System.currentTimeMillis();
	}
	
	public String toString() {
		return nom + " — " + type + " — " + status + " — " + lien;
	}
	
	public boolean reserver(User user) {
		if(!(status == Status.OUVERT || status == Status.ABANDONNE || status == Status.EN_PAUSE || status == Status.INCONNU || status == Status.VALIDE || status == Status.SANS_NOUVELLES))
			return false;
		resDate = System.currentTimeMillis();
		reservation = user.getIdLong();
		old = status;
		resName = user.getName();
		status = Status.RESERVE;
		return true;
	}
	
	public boolean reserver(String name) {
		if(!(status == Status.OUVERT || status == Status.ABANDONNE || status == Status.EN_PAUSE || status == Status.INCONNU || status == Status.VALIDE || status == Status.SANS_NOUVELLES))
			return false;
		resDate = System.currentTimeMillis();
		reservation = 0;
		old = status;
		resName = name;
		status = Status.RESERVE;
		return true;
	}
	
	public boolean liberer(Member member) {
		if(status != Status.RESERVE)
			return false;
		if(reservation == 0L) {
			resName = "";
			status = old;
			return true;
		} else if(member.getUser().getIdLong() == reservation || member.getRoles().contains(member.getGuild().getRoleById(612383955253067963L)) || resDate > 3*24*3600*1000) {
			reservation = 0;
			resName = "";
			status = old;
			return true;
		} else
			return false;
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
	
	public boolean complyWith(Type type, Status status) {
		boolean ret = true;
		if(type != null)
			ret = ret && this.type == type;
		if(status != null)
			ret = ret && this.status == status;
		return ret;
	}
	
	public boolean isDead() {
		return status == Status.ABANDONNE || status == Status.PUBLIE || status == Status.REFUSE;
	}
	
	public boolean setStatus(Status status) {
		if(status != Status.SANS_NOUVELLES)
			lastUpdate = System.currentTimeMillis();
		if(this.status == Status.RESERVE)
			return false;
		else if(status == Status.RESERVE)
			return false;
		this.status = status;
		return true;
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
	public void promote() {
		if(type == Type.IDEE)
			type = Type.RAPPORT;
	}
	
	public String getResDate() {
		return new SimpleDateFormat("dd MMM yyyy à HH:mm").format(new Date(resDate));
	}
	
	public String getLastUpdate() {
		return new SimpleDateFormat("dd MMM yyyy à HH:mm").format(new Date(lastUpdate));
	}
	
	public boolean olderThan(long time) {
		return time > lastUpdate;
	}
	
	
	public Ecrit clone() {
		return new Gson().fromJson(new Gson().toJson(this), this.getClass());
		
	}
}
