package tk.cyriellecentori.CritiBot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

/**
 * Représente un lien vers un message Discord pouvant être enregistré dans un fichier et récupéré plus tard.
 * @author cyrielle
 *
 */
public class BotMessage {
	/**
	 * Le lien vers le message par l'API.
	 */
	private transient Message message;
	/**
	 * L'identifiant du message.
	 */
	private long messageID;
	/**
	 * L'identifiant du salon dans lequel est le message.
	 */
	private long channelID;
	
	/**
	 * Construit l'objet à partir d'un message de l'API.
	 * @param message Le lien vers le message par l'API.
	 */
	public BotMessage(Message message) {
		this.message = message;
		messageID = message.getIdLong();
		channelID = message.getChannel().getIdLong();
	}
	
	/**
	 * Construit l'objet à partir de ses coordonnées.
	 * @param messageID L'identifiant du message.
	 * @param channelID L'identifiant du salon du message.
	 * @param guildID L'identifiant du serveur du message.
	 */
	public BotMessage(long messageID, long channelID, long guildID) {
		this.messageID = messageID;
		this.channelID = channelID;
		message = null;
	}
	
	/**
	 * Construit un objet vide.
	 */
	public BotMessage() {
		this.messageID = 0;
		this.channelID = 0;
		message = null;
	}
	
	/**
	 * 
	 * @return si le lien par l'API est établi ou non.
	 */
	public boolean isInitialized() {
		return message != null;
	}
	
	/**
	 * Récupère si possible le lien par l'API au message.
	 * @param jda L'API
	 */
	public void retrieve(JDA jda) {
		if(messageID != 0)
			try {
				message = jda.getTextChannelById(channelID).retrieveMessageById(messageID).complete();
			} catch(ErrorResponseException | NullPointerException e) {
				message = null;
			}
	}
	
	public Message getMessage() {
		return message;
	}
}
