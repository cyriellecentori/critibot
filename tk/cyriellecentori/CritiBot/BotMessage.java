package tk.cyriellecentori.CritiBot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

public class BotMessage {
	private transient Message message;
	private long messageID;
	private long channelID;
	
	public BotMessage(Message message) {
		this.message = message;
		messageID = message.getIdLong();
		channelID = message.getChannel().getIdLong();
	}
	
	public BotMessage(long messageID, long channelID, long guildID) {
		this.messageID = messageID;
		this.channelID = channelID;
		message = null;
	}
	
	public BotMessage() {
		this.messageID = 0;
		this.channelID = 0;
		message = null;
	}
	
	public boolean isInitialized() {
		return message != null;
	}
	
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
