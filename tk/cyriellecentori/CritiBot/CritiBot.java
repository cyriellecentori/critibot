package tk.cyriellecentori.CritiBot;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import tk.cyriellecentori.CritiBot.Ecrit.Status;
import tk.cyriellecentori.CritiBot.Ecrit.Type;

public class CritiBot implements EventListener {
	
	public static void main(String[] args) {
		if(args.length < 1) {
			System.err.println("Merci d'indiquer le token du bot en paramètre.");
			return;
		}
		CritiBot cb = new CritiBot(args[0]);

	}
	
	private long lastCheck = 1620570510854L;
	private Vector<SyndEntry> inbox = new Vector<SyndEntry>();
	private String token;
	private JDABuilder builder;
	private JDA jda;
	private Gson gson;
	private Stack<Vector<Ecrit>> cancel = new Stack<Vector<Ecrit>>();
	private Vector<Ecrit> ecrits;
	private boolean errorSave = false;
	private LinkedHashMap<String, BotCommand> commands = new LinkedHashMap<String, BotCommand>();
	private final long organichan;
	private final long openchan;
	private final long fluxchan = 710767720433451088L;
	private final long henritueur;
	private final long henricheck;
	private final long henricross;
	//private final String whiteCheckBox = "U+2705";
	private final String unlock = "U+1f513";
	//private final String cross = "U+274e";
	private final String prefix;
	public long lastUpdate;
	
	public CritiBot(String token) {
		this.token = token;
		if(token.hashCode() == 1973164890) {
			prefix = "c";
			organichan = 614947463610236939L;
			henritueur = 817064076244418610L;
			openchan = 843956373103968308L;
			henricheck = 843965097428516864L;
			henricross = 843965099986780200L;
		} else {
			prefix = "bc";
			organichan = 843885689397575740L;
			henritueur = 843938845724115009L;
			openchan = 843884076821643286L;
			henricheck = 843965501671473174L;
			henricross = 843965494142173215L;
			System.out.println("Booting in beta.");
		}
		
		try {

			builder = JDABuilder.createDefault(this.token)
					.addEventListeners(this);

			jda = builder.build();
		} catch(LoginException | IllegalArgumentException e) {
			e.printStackTrace();
			return;
		}
		jda.setAutoReconnect(true);
		
		GsonBuilder gsonBilder = new GsonBuilder();
		gsonBilder.setPrettyPrinting();

		gson =gsonBilder.create();
		
		String data = "";
		BufferedReader dataFile;
		try {
			dataFile = new BufferedReader(new FileReader("critibot.json"));
			while(true) {
				String str = dataFile.readLine();
				if(str == null) break;
				else data = data + "\n" + str;
			}

			dataFile.close();
			
		} catch (IOException exp) {
			exp.printStackTrace();
			System.out.println("Impossible d'ouvrir les données, de nouvelles seront crées à la prochaine sauvegarde.");
			ecrits = new Vector<Ecrit>();
		}
		
		TypeToken<Vector<Ecrit>> ttve = new TypeToken<Vector<Ecrit>>() {};
		
		try {
			lastCheck = Long.parseLong(data.split("θ")[1]);
			ecrits = gson.fromJson(data.split("θ")[0], ttve.getType());
		} catch(Exception e) {
			ecrits = gson.fromJson(data, ttve.getType());
		}
		
		
		
		if(ecrits == null)
			ecrits = new Vector<Ecrit>();
		
		/*Vector<Ecrit> nouveau = new Vector<Ecrit>();
		java.util.Random r = new java.util.Random();
		for(Ecrit e : ecrits) {
			if(r.nextBoolean()) {
				nouveau.add(e);
			}
		}
		ecrits = nouveau;
		try {
			save();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.exit(0);*/
		
		initCommands();
		
		lastUpdate = System.currentTimeMillis();

	}
	
	public void addNew() throws IllegalArgumentException, MalformedURLException, FeedException, IOException {
		SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL("http://fondationscp.wikidot.com/feed/forum/ct-656675.xml")));
		Date lastDate = new Date(0);
		for(Object e : feed.getEntries()) {
			SyndEntry entry = (SyndEntry) e;
			if(entry.getPublishedDate().after(new Date(lastCheck))) {
				if(entry.getTitle().contains("]")) {
					Type type = null;
					String[] sp = entry.getTitle().split("]");
					String balises = "";
					for(int i = 0; i < sp.length - 1; i++) {
						balises += sp[i] + "]";
					}
					String unclean = sp[sp.length - 1];
					if(unclean.contains("SCP") && unclean.contains("FR")) {
						unclean = unclean.split("FR",2)[1];
					}
					while(unclean.startsWith(" ") || unclean.startsWith(":")) {
						unclean = unclean.substring(1);
					}
					if(unclean.startsWith("\"")) {
						unclean = unclean.substring(1, unclean.length() - 1);
					}
					if(unclean.isEmpty() || sp.length == 1) {
						Vector<Ecrit> es = search("sans nom");
						unclean = "(sans nom " + es.size() + ")";
						balises = entry.getTitle();
					}
					if(balises.contains("Idée") || balises.contains("idée")) {
						type = Type.IDEE;
					} else if(balises.contains("Conte") || balises.contains("Série")) {
						type = Type.CONTE;
					} else if(balises.contains("Refus")) {
						type = Type.AUTRE;
					} else {
						type = Type.RAPPORT;
					}
					Ecrit ecrit = new Ecrit(unclean, entry.getLink(), type, Status.OUVERT);
					ecrits.add(ecrit);
					
				} else {
					inbox.add(entry);
					jda.getPresence().setStatus(OnlineStatus.IDLE);
				}
			}
			if(lastDate.before(entry.getPublishedDate()))
				lastDate = entry.getPublishedDate();
		}
		if(lastDate.getTime() != 0) {
			lastCheck = lastDate.getTime() + 1;
		}
	}
	
	public void save() throws IOException {
		BufferedWriter dataFile = new BufferedWriter(new FileWriter("critibot.json"));
		dataFile.write(gson.toJson(ecrits) + "θ" + lastCheck);
		dataFile.close();
	}
	
	private String basicize(String s) {
		return s.toLowerCase()
				.replaceAll("é", "e")
				.replaceAll("ê", "e")
				.replaceAll("à", "a")
				.replaceAll("ï", "i")
				.replaceAll("ç", "c")
				.replaceAll("ë", "e")
				.replaceAll("ô", "o")
				.replaceAll("è", "e")
				.replaceAll("î", "i")
				.replaceAll("œ", "oe")
				.replaceAll("æ", "ae")
				.replaceAll("û", "u");
		
	}
	
	public Vector<Ecrit> search(String s) {
		Vector<Ecrit> list = new Vector<Ecrit>();
		String[] motsSearch = basicize(s).split(" ");
		for(Ecrit e : ecrits) {
			String[] mots = basicize(e.getNom()).split(" ");
			boolean ok = true;
			for(String motSearch : motsSearch) {
				boolean found = false;
				for(String mot : mots) {
					if(mot.toLowerCase().contains(motSearch.toLowerCase())) {
						found = true;
					}
				}
				if(!found) {
					ok = false;
					break;
				}
			}
			if(ok) {
				list.add(e);
			}
		}
		return list;
	}
	
	public void clean(boolean fort) {
		Vector<Ecrit> toRem = new Vector<Ecrit>();
		for(Ecrit e : ecrits) {
			if(e.isDead() || (fort && e.getStatus() == Status.SANS_NOUVELLES))
				toRem.add(e);
		}
		for(Ecrit e : toRem) {
			ecrits.remove(e);
		}
	}
	
	public void remove(Ecrit e) {
		ecrits.remove(e);
	}
	
	public void updateOpen() {
		for(Ecrit e : ecrits) {
			if(e.getStatus() == Status.OUVERT && !e.getStatusMessage().isInitialized()) {
				Message m = jda.getTextChannelById(openchan).sendMessage(e.toEmbed()).complete();
				e.setStatusMessage(m);
				m.addReaction(jda.getEmoteById(henritueur)).queue();
				m.addReaction(jda.getEmoteById(henricheck)).queue();
				if(e.getType() == Type.IDEE)
					m.addReaction(jda.getEmoteById(henricross)).queue();
			} else if(e.getStatus() != Status.OUVERT && e.getStatusMessage().isInitialized()) {
				if(e.getStatusMessage().getMessage().getChannel().getIdLong() == openchan) {
					e.removeStatusMessage();
				}
			} 
			if(e.getStatus() == Status.RESERVE && !e.getStatusMessage().isInitialized()) {
				Message m = jda.getTextChannelById(organichan).sendMessage(e.toEmbed()).complete();
				e.setStatusMessage(m);
				m.addReaction(jda.getEmoteById(henricheck)).queue();
				if(e.getType() == Type.IDEE)
					m.addReaction(jda.getEmoteById(henricross)).queue();
				m.addReaction(unlock).queue();
			} else if(e.getStatus() != Status.RESERVE && e.getStatusMessage().isInitialized())
				if(e.getStatusMessage().getMessage().getChannel().getIdLong() == organichan)
					e.removeStatusMessage();
		}
	}
	
	public void refreshMessages() {
		for(Ecrit e : ecrits) {
			if(e.getStatusMessage().isInitialized()) {
				e.removeStatusMessage();
			}
		}
	}
	
	private void initCommands() {
		commands.put("aide", new BotCommand() {
			public void execute(CritiBot bb, MessageReceivedEvent message, String[] args) {
				EmbedBuilder b = new EmbedBuilder();
				b.setTitle("Aide de Critibot");
				b.setDescription("Les paramètres entre crochets sont optionnels, entre accolades obligatoires.");
				b.addField("Valeurs de Statut", "Statut doit être « Ouvert — En attente — Abandonné — En pause — Sans nouvelles — Inconnu — Publié — Réservé — Validé — Refusé — Infraction »", false);
				b.addField("Valeurs de Type", "Type doit être « Conte — Rapport — Idée — Autre »", false);
				b.addField("Commandes de base", "`c!aide` : Cette commande d'aide.\n"
						+ "`c!annuler` : Annule la dernière modification effectuée.", false);
				b.addField("Commandes de gestion et d'affichage de la liste", "`c!ajouter {Nom};{Type};{Statut};{URL}` : Ajoute manuellement un écrit à la liste.\n"
						+ "`c!rechercher {Critère}` : Affiche tous les écrits contenant {Critère}.\n"
						+ "`c!lister {Statut};[Type]` : Affiche la liste des écrits avec le statut et du type demandés. Statut et Type peuvent prendre la valeur « Tout ».\n"
						+ "`c!supprimer {Critère}` : Supprime un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit. __**ATTENTION**__ : Il n'y a pas de confirmation, faites attention à ne pas vous tromper dans le Critère.\n"
						+ "`c!inbox` : Affiche la boîte de réception, contenant les écrits qui n'ont pas pu être ajoutés automatiquement. Attention, l'appel à cette commande supprime le contenu de la boîte.", false);
				b.addField("Commandes de critiques", "`c!réserver {Critère}` : Réserve un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!réserver_pour {Critère};{Nom}` : Réserve un écrit pour quelqu'un d'autre. Le Critère doit être assez fin pour aboutir à un unique écrit."
						+ "`c!libérer {Critère}` : Supprime la réservation d'un écrit si vous êtes la personne l'ayant réservé, ou membre de l'équipe critique, ou que l'écrit a été réservé par procuration. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!réservation {Critère}` : Affiche les informations de réservation d'un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!up {Critère}` : Marque un écrit ouvert et le remet au premier plan dans le salon des fils ouverts s'il l'était déjà. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!valider {Critère}` : Change le type du rapport en Rapport si c'était une idée et fait le même effet que c!critiqué. Le Critère doit être assez fin pour aboutir à un unique écrit.", false);
				b.addField("Commandes de modification d'un écrit",  "`c!statut {Critère};{Statut}` : Change le statut de l'écrit demandé. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!type {Critère};{Type}` : Change le type de l'écrit demandé. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!ouvrir {Critère}` : Raccourci pour `c!statut {Critère};Ouvert`.\n"
						+ "`c!renommer {Critère};{Nouveau nom}` : Renomme un écrit.", false);
				b.addField("Commandes d'entretien de la base de données (À utiliser avec précaution)",
						"`c!nettoyer` : Supprime tous les écrits abandonnés / refusés / publiés de la liste.\n"
						+ "`c!archiver_avant {Date}` : Met le statut « sans nouvelles » à tous les écrits n'ayant pas été mis à jour avant la date indiquée. La date doit être au format dd/mm/yyyy.\n"
						+ "`c!nettoyer_fort` : Supprime tous les écrits abandonnés / refusés / publiés / sans nouvelles de la liste.\n"
						+ "`c!doublons` : Supprime les éventuels doublons.", false);
				b.addField("Code source", "Disponible sur [Github](https://github.com/cyriellecentori/critibot).", false);
				b.setFooter("Version 1.4");
				b.setAuthor("Critibot", null, "https://media.discordapp.net/attachments/719194758093733988/842082066589679676/Critiqueurs5.jpg");
				message.getChannel().sendMessage(b.build()).queue();
			}
		});
		
		commands.put("ajouter", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				try {
					ecrits.add(new Ecrit(args[0],  args[3], Type.getType(args[1]), Status.getStatus(args[2])));
					message.getChannel().sendMessage("Ajouté !").queue();
				} catch (ArrayIndexOutOfBoundsException e) {
					message.getChannel().sendMessage("Utilisation : c!add Nom;Type;Statut;URL.").queue();
				}
			}
			
		});
		
		commands.put("lister", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				try {
					Status status = (args[0].equalsIgnoreCase("tout") ? null : Status.getStatus(args[0]));
					Type type = null;
					if(args.length > 1)
						type = (args[1].equalsIgnoreCase("tout") ? null : Type.getType(args[1]));
					
					
					Vector<MessageEmbed> embeds = new Vector<MessageEmbed>();
					Vector<String> messages = new Vector<String>();
					String buffer = "";
					for(Ecrit e : ecrits) {
						if(e.complyWith(type, status)) {
							String toAdd = "[**" + e.getNom() + "**](" + e.getLien() + ")\n" + e.getStatus() + " — " + e.getType() + "\n\n";
							if(buffer.length() + toAdd.length() > 2000) {
								messages.add(buffer);
								buffer = "";
							}
							buffer += toAdd;
						}
							
					}
					if(buffer.isEmpty()) {
						EmbedBuilder b = new EmbedBuilder();
						b.setTitle("Aucun résultat");
						b.setAuthor("Recherche : " + ((status == null) ? "Tous statuts" : status) + " — " + ((type == null) ? "Tous types" : type));
						b.setTimestamp(Instant.now());
						b.setColor(16001600);
						message.getChannel().sendMessage(b.build()).queue();
					} else {
						messages.add(buffer);
						EmbedBuilder b = new EmbedBuilder();
						b.setTitle("Résultats de la recherche");
						b.setAuthor("Recherche : " + ((status == null) ? "Tous statuts" : status) + " — " + ((type == null) ? "Tous types" : type));
						b.setTimestamp(Instant.now());
						b.setColor(73887);
						for(int i = 0; i < messages.size(); i++) {
							b.setFooter("Page " + (i + 1) + "/" + messages.size());
							b.setDescription(messages.get(i));
							embeds.add(b.build());
						}
						
						for(MessageEmbed embed : embeds) {
							message.getChannel().sendMessage(embed).queue();
						}
					}
					
				} catch(ArrayIndexOutOfBoundsException e) {
					message.getChannel().sendMessage("Utilisation : c!list Statut;Type. Il est possible de ne pas spécifier le type si non nécessaire, alors la commande sera c!list Statut et tous les types seront affichés.").queue();
				}
			}
			
		});
		commands.put("l", new BotCommand.Alias(commands.get("lister")));
		
		commands.put("plop", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getChannel().sendMessage("plop").queue();
				
			}
			
		});
		
		commands.put("rechercher", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				try {
					Vector<Ecrit> res = bot.search(args[0]);
					if(res.isEmpty()) {
						EmbedBuilder b = new EmbedBuilder();
						b.setTitle("Aucun résultat");
						b.setAuthor("Recherche : " + args[0]);
						b.setTimestamp(Instant.now());
						b.setColor(16001600);
						message.getChannel().sendMessage(b.build()).queue();
					} else if(res.size() <= 3) {
						for(Ecrit e : res) {
							message.getChannel().sendMessage(e.toEmbed()).queue();
						}
					} else {
						Vector<MessageEmbed> embeds = new Vector<MessageEmbed>();
						Vector<String> messages = new Vector<String>();
						String buffer = "";
						for(Ecrit e : res) {
							String toAdd = "[**" + e.getNom() + "**](" + e.getLien() + ")\n" + e.getStatus() + " — " + e.getType() + "\n\n";
							if(buffer.length() + toAdd.length() > 2000) {
								messages.add(buffer);
								buffer = "";
							}
							buffer += toAdd;
						}
						messages.add(buffer);
						EmbedBuilder b = new EmbedBuilder();
						b.setTitle("Résultats de la recherche");
						b.setAuthor("Recherche : " + args[0]);
						b.setTimestamp(Instant.now());
						b.setColor(73887);
						for(int i = 0; i < messages.size(); i++) {
							b.setFooter("Page " + (i + 1) + "/" + messages.size());
							b.setDescription(messages.get(i));
							embeds.add(b.build());

							for(MessageEmbed embed : embeds) {
								message.getChannel().sendMessage(embed).queue();
							}
						}
					}
				} catch(ArrayIndexOutOfBoundsException e) {
					message.getChannel().sendMessage("Rechercher… Quoi exactement ?").queue();
				}
			}

		});
		
		commands.put("s", new BotCommand.Alias(commands.get("rechercher")));
		
		commands.put("réserver", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getMessage().delete().queue();
				archiver();
				if(e.reserver(message.getMember())) {
					message.getChannel().sendMessage("« " + e.getNom() + " » réservé par " + message.getAuthor().getName() + " !").queue();
				} else {
					message.getAuthor().openPrivateChannel().complete().sendMessage("L'écrit « " + e.getNom() + " » ne peut pas être réservé. Il l'est peut-être déjà, ou n'a pas un statut réservable. Les écrits pouvant être réservés sont les écrits Ouverts, Abandonnés, En Pause, Validés, Sans Nouvelles ou au statut inconnu. Vous pouvez également tenter de libérer l'écrit si vous faites partie de l'Équipe Critique ou que l'écrit est réservé depuis plus de trois jours.").queue();
				}
			}
		});
		
		commands.put("r", new BotCommand.Alias(commands.get("réserver")));
		
		commands.put("reserver", new BotCommand.Alias(commands.get("réserver")));
		
		commands.put("libérer", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getMessage().delete().queue();
				archiver();
				if(e.getStatus() != Status.RESERVE) {
					message.getChannel().sendMessage("« " + e.getNom() + " » n'est pas réservé.").queue();
					return;
				}
				if(e.liberer(message.getMember())) {
					message.getChannel().sendMessage("Réservation sur « " + e.getNom() + " » supprimée.").queue();
				} else {
					message.getAuthor().openPrivateChannel().complete().sendMessage("Vous n'êtes pas à l'origine de la réservation sur « " + e.getNom() + " » ou vous n'êtes pas de l'équipe critique.").queue();
				}
			}
		});
		
		commands.put("nettoyer", new BotCommand() {
			
			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				bot.clean(false);
				message.getChannel().sendMessage("Écrits abandonnés, publiés et refusés supprimés de la liste !").queue();
			}
		});
		
		commands.put("status", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				Status status = Status.getStatus(args[1]);
				boolean ret = e.setStatus(status);
				if(!ret) {
					if(status == Status.RESERVE) {
						message.getChannel().sendMessage("Utilisez c!réserver pour réserver un écrit.").queue();
					} else {
						message.getChannel().sendMessage("Cet écrit est réservé, libérez le d'abord avec c!libérer pour changer son statut.").queue();
					}
				} else
					message.getChannel().sendMessage("Statut de « " + e.getNom() + " » changé !").queue();
			}
		});
		
		commands.put("statut", new BotCommand.Alias(commands.get("status")));
		
		commands.put("type", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				Type status = Type.getType(args[1]);
				e.setType(status);
				message.getChannel().sendMessage("Type de « " + e.getNom() + " » changé !").queue();
			}
		});
		
		commands.put("valider", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getMessage().delete().queue();
				archiver();
				e.promote();
				message.getChannel().sendMessage("Si l'écrit « " + e.getNom() + " » était une idée, c'est maintenant un rapport ! Sinon, rien n'a changé.").queue();
				if(e.getStatus() == Status.RESERVE)
					if(!e.liberer(message.getMember()))
						message.getAuthor().openPrivateChannel().complete().sendMessage("L'écrit « " + e.getNom() + " » a déjà été réservé par quelqu'un d'autre. Je vais cependant vous croire sur le fait que vous avez critiqué l'écrit, mais vous avez probablement enfreint une réservation et c'est pas très gentil.").queue();
				e.setStatus(Status.EN_ATTENTE);
				message.getChannel().sendMessage("L'écrit « " + e.getNom() + " » a été noté comme critiqué !").queue();
				
			}
		});
		
		commands.put("réservation", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				if(e.getStatus() == Status.RESERVE)
					message.getChannel().sendMessage("L'écrit « " + e.getNom() + " » est a été réservé par " + e.getReservation(message.getGuild()) + " le " + e.getResDate() + ".").queue();
				else
					message.getChannel().sendMessage("L'écrit « " + e.getNom() + " » n'est pas réservé.").queue();
				
			}
		});
		
		commands.put("reservation", new BotCommand.Alias(commands.get("réservation")));
		
		commands.put("supprimer", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				bot.remove(e);
				message.getChannel().sendMessage("L'écrit « " + e.getNom() + " » a été supprimé.").queue();	
			}
		});
		
		commands.put("réserver_pour", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getMessage().delete().queue();
				archiver();
				if(e.reserver(args[1])) {
					message.getChannel().sendMessage("« " + e.getNom() + " » réservé pour " + args[1] + " !").queue();
				} else {
					message.getAuthor().openPrivateChannel().complete().sendMessage("L'écrit « " + e.getNom() + " » ne peut pas être réservé. Il l'est peut-être déjà, ou n'a pas un statut réservable. Les écrits pouvant être réservés sont les écrits Ouverts, Abandonnés, En Pause, Validés, Sans Nouvelles ou au statut inconnu. Vous pouvez également tenter de libérer l'écrit si vous faites partie de l'Équipe Critique ou que l'écrit est réservé depuis plus de trois jours.").queue();
				}
				
			}
		});
		
		commands.put("critiqué", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getMessage().delete().queue();
				archiver();
				message.getChannel().sendMessage("« " + e.getNom() + " » critiqué !").queue();
				if(!e.critique(message.getMember())) {
					message.getAuthor().openPrivateChannel().complete().sendMessage("Vous avez indiqué avoir critiqué « " + e.getNom() + " » mais vous n'étiez pas à l'origine de la réservation. Je vous fais confiance, mais sachez que c'est pas cool.").queue();
				}
			}
		});
		
		commands.put("critique", new BotCommand.Alias(commands.get("critiqué")));
		
		commands.put("inbox", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				String mes = "Boîte de réception :\n";
				for(SyndEntry e : bot.getInbox()) {
					mes += e.getTitle() + " — " + e.getLink() + "\n";
				}
				sendLongMessage(mes, message.getChannel());
				bot.getInbox().clear();
				bot.getJda().getPresence().setStatus(OnlineStatus.ONLINE);
				
			}
		});
		
		commands.put("archiver_avant", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				long date = 0L;
				try {
					date = new SimpleDateFormat("dd/MM/yyyy").parse(args[0]).getTime();
					int n = 0;
					for(Ecrit e : bot.getEcrits()) {
						if(e.olderThan(date)) {
							e.setStatus(Status.SANS_NOUVELLES);
							n++;
						}
					}
					message.getChannel().sendMessage(n + " écrits ont été marqués sans nouvelles depuis le " + new SimpleDateFormat("dd MMM yyyy").format(new Date(date)) + ".").queue();
				} catch(ParseException | NullPointerException e) {
					message.getChannel().sendMessage("Erreur dans le format de la date.").queue();
				}
				
			}
			
		});
		
		commands.put("annuler", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				if(!bot.annuler()) {
					message.getChannel().sendMessage("Aucune modification effectuée.").queue();
				} else {
					message.getChannel().sendMessage("Dernière modification annulée !").queue();
				}
				
			}
			
		});
		
		commands.put("update_open", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				bot.updateOpen();
				
			}
			
		});
		
		commands.put("refresh_messages", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				bot.refreshMessages();
				
			}
			
		});
		
		
		
		commands.put("maj", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				try {
					int tailleAncienne = bot.getEcrits().size();
					addNew();
					if(tailleAncienne != bot.getEcrits().size()) {
						bot.updateOpen();
					}
					message.getChannel().sendMessage("Mise à jour effectuée.").queue();
				} catch (IllegalArgumentException | FeedException | IOException e) {
					jda.getTextChannelById(737725144390172714L).sendMessage("<@340877529973784586>\n" + e.getStackTrace().toString()).queue();
					e.printStackTrace();
				}
				
			}
			
		});
		
		commands.put("renommer", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				String oldName = e.getNom();
				try {
					e.rename(args[1]);
					message.getChannel().sendMessage("« " + oldName + " » renommé en « " + e.getNom() + " ».").queue();
				} catch(ArrayIndexOutOfBoundsException ex) {
					message.getChannel().sendMessage("Utilisation : `c!rename {Critère};{Nouveau nom}").queue();
				}
			}
		});
		
		commands.put("doublons", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				bot.doublons();
				message.getChannel().sendMessage("Doublons supprimés !").queue();
			}
			
		});
		
		commands.put("nettoyer_fort", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				bot.clean(true);
				message.getChannel().sendMessage("Écrits abandonnés, refusés, publiés et sans nouvelles supprimés de la liste !").queue();
			}
			
		});
		
		commands.put("up", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				if(e.getStatusMessage().isInitialized()) {
					e.removeStatusMessage();
				} else if(e.getStatus() != Status.RESERVE) {
					e.setStatus(Status.OUVERT);
				}
				message.getChannel().sendMessage("« " + e.getNom() + " » up !").queue();
			}
		});
		
		commands.put("réouvert", new BotCommand.Alias(commands.get("up")));
		commands.put("ouvrir", new BotCommand.Alias(commands.get("up")));
		commands.put("o", new BotCommand.Alias(commands.get("up")));
		commands.put("reouvert", new BotCommand.Alias(commands.get("up")));
		
		commands.put("manual_update", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				bot.lastUpdate = System.currentTimeMillis();
				bot.getJda().getPresence().setActivity(Activity.playing("critiquer. Dernière mise à jour forum le " + new SimpleDateFormat("dd MMM yyyy à HH:mm").format(new Date(lastUpdate))));
				message.getChannel().sendMessage("Bot indiqué comme mis à jour.").queue();
				
			}
			
		});
		
		commands.put("bdd", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getChannel().sendFile(new File("critibot.json")).queue();
				
			}
			
		});
		
	}
	
	public void doublons() {
		Vector<String> noms = new Vector<String>();
		Vector<Ecrit> toDel = new Vector<Ecrit>();
		for(Ecrit e : ecrits) {
			if(noms.contains(basicize(e.getNom()))) {
				toDel.add(e);
			} else {
				noms.add(basicize(e.getNom()));
			}
		}
		for(Ecrit e : toDel) {
			ecrits.remove(e);
		}
	}
	
	public boolean annuler() {
		if(cancel.empty()) {
			return false;
		} else {
			ecrits = cancel.pop();
			for(Ecrit e : ecrits) {
				e.getStatusMessage().retrieve(jda);
			}
			return true;
		}
	}
	
	public Vector<Ecrit> getEcrits() {
		return ecrits;
	}
	
	public BotCommand getCommand(String name) {
		return commands.get(name);
	}
	
	public JDA getJda() {
		return jda;
	}
	
	public Vector<SyndEntry> getInbox() {
		return inbox;
	}
	
	public void archiver() {
		cancel.add(new Vector<Ecrit>());
		for(Ecrit e : ecrits) {
			cancel.peek().add(e.clone());
		}
		if(cancel.size() > 20) {
			cancel.remove(0);
		}
	}
	
	private int counter = 0;
	
	private ScheduledExecutorService shreduler = Executors.newScheduledThreadPool(1);

	@Override
	public void onEvent(GenericEvent event) {
		if(event instanceof ReadyEvent) {
			for(Ecrit e : ecrits)
				e.check(jda);
			shreduler.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					try {
						int tailleAncienne = ecrits.size();
						addNew();
						if(tailleAncienne != ecrits.size()) {
							updateOpen();
						}
					} catch (IllegalArgumentException | FeedException | IOException e) {
						e.printStackTrace();
						jda.getTextChannelById(737725144390172714L).sendMessage("<@340877529973784586>\n" + e.getStackTrace().toString()).queue();
					}
					System.out.println("Shreduled update.");
				}
				
			}, 10, 10, TimeUnit.MINUTES);
			jda.getPresence().setActivity(Activity.playing("critiquer. Aucune mise à jour forum depuis le redémarrage."));

		}
		if(event instanceof MessageReactionAddEvent) {
			MessageReactionAddEvent mrae = (MessageReactionAddEvent) event;
			if(!mrae.getUser().isBot())
				for(Ecrit e : ecrits) {
					if(e.getStatusMessage().isInitialized())
					if(mrae.getMessageIdLong() == e.getStatusMessage().getMessage().getIdLong()) {
						if(mrae.getReactionEmote().isEmoji()) {
							if(mrae.getReactionEmote().getAsCodepoints().equals(unlock) && e.getResId() == mrae.getUserIdLong() || (e.getResId() == 0L && e.getStatus() == Status.RESERVE)) {
								archiver();
								e.liberer(null);
								e.removeStatusMessage();
							}
						} else {
							if(mrae.getReactionEmote().getEmote().getIdLong() == henritueur && mrae.getChannel().getIdLong() == openchan) { // Réservation
								if(e.getStatus() == Status.RESERVE && e.getResId() != mrae.getUserIdLong()) {
									mrae.getUser().openPrivateChannel().complete().sendMessage("L'écrit « " + e.getNom() + " » est déjà réservé !").queue();
								} else {
									archiver();
									if(e.reserver(mrae.getMember())) {
										Message m = jda.getTextChannelById(organichan).sendMessage(e.toEmbed()).complete();
										e.removeStatusMessage();
										e.setStatusMessage(m);
										m.addReaction(jda.getEmoteById(henricheck)).queue();
										if(e.getType() == Type.IDEE)
											m.addReaction(jda.getEmoteById(henricross)).queue();
										m.addReaction(unlock).queue();
									} else {
										mrae.getUser().openPrivateChannel().complete().sendMessage("L'écrit « " + e.getNom() + " » ne peut pas être réservé. Il l'est peut-être déjà, ou n'a pas un statut réservable. Les écrits pouvant être réservés sont les écrits Ouverts, Abandonnés, En Pause, Validés, Sans Nouvelles ou au statut inconnu. Vous pouvez également tenter de libérer l'écrit si vous faites partie de l'Équipe Critique ou que l'écrit est réservé depuis plus de trois jours.").queue();
									}
								}
							} else if(mrae.getReactionEmote().getEmote().getIdLong() == henricross && e.getType() == Type.IDEE) {
								archiver();
								e.liberer(null);
								e.setStatus(Status.REFUSE);
								jda.getTextChannelById(organichan).sendMessage("« " + e.getNom() + " » refusé !").queue();
								e.removeStatusMessage();
							} else if(mrae.getReactionEmote().getEmote().getIdLong() == henricheck) {
								archiver();
								jda.getTextChannelById(organichan).sendMessage("« " + e.getNom() + " » critiqué !").queue();
								if(!e.critique(mrae.getMember())) {
									mrae.getUser().openPrivateChannel().complete().sendMessage("Vous avez indiqué avoir critiqué « " + e.getNom() + " » mais vous n'étiez pas à l'origine de la réservation. Je vous fais confiance, mais sachez que c'est pas cool.").queue();
								}
								e.removeStatusMessage();
							} 
						}
						updateOpen();
						try {
							save();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
		} /*else if(event instanceof GuildMessageReactionRemoveEvent) {
			GuildMessageReactionRemoveEvent mrre = (GuildMessageReactionRemoveEvent) event;
			for(Ecrit e : ecrits) {
				if(mrre.getMessageIdLong() == e.getStatusMessage().getMessage().getIdLong()) {
					if(mrre.getReactionEmote().isEmoji()) {
						
					} else {
						if(mrre.getReactionEmote().getEmote().getIdLong() == henritueur && e.getResId() == mrre.getUserIdLong()) { // Réservation
							archiver();
							e.liberer(null);
							jda.getTextChannelById(organichan).sendMessage("Réservation sur « " + e.getNom() + " » supprimée.").queue();
						}
					}
					updateOpen();
				}
			}
		}*/ else if(event instanceof MessageReceivedEvent) {
			MessageReceivedEvent mre = (MessageReceivedEvent) event;
			
			if(!mre.getMessage().getContentRaw().startsWith(prefix + "!") || mre.getAuthor().isBot() || mre.getAuthor().getId().equals(jda.getSelfUser().getId()))
				return;
			
			String command = mre.getMessage().getContentRaw().split(" ", 2)[0].split("!", 2)[1];
			String args;
			try {
				args = mre.getMessage().getContentRaw().split(" ", 2)[1];
			} catch(ArrayIndexOutOfBoundsException e) {
				args = "";
			}
			System.out.println(command);
			System.out.println(args);
			System.out.println("——————————");
			MessageChannel chan = mre.getChannel();
			
			try {
				commands.get(command).execute(this, mre, args.split(";"));
			} catch(NullPointerException e) {
				chan.sendMessage("Commande inconnue.").queue();
			}
			updateOpen();
			
			try {
				save();
			} catch (IOException e) {
				if(!errorSave) {
					mre.getChannel().sendMessage("Impossible de sauvegarder les données. Je vais continuer à essayer, mais n'enverrai pas d'autres messages si cela échoue.").queue();
					e.printStackTrace();
					errorSave = true;
				}
			}
		}
		
	}

}
